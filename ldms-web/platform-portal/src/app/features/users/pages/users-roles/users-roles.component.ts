import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UsersPortalService } from '../../services/users-portal.service';
import { Subject, Subscription, catchError, debounceTime, finalize, map, of, switchMap, throwError } from 'rxjs';
import {
  LxExportFormat,
  downloadBlob,
  exportClientTableAsCsv,
  exportFilename,
  exportFormatLabel,
} from '@shared/utils/lx-export.util';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import {
  LdmsRoleModuleSection,
  listLdmsRoleModuleSections,
  moduleCatalogSearchHint,
  moduleSectionFromApi,
} from '@shared/utils/ldms-role-module.util';

export interface RoleRow {
  id: number;
  role: string;
  description: string;
  moduleKey: string;
  moduleLabel: string;
  status: string;
  statusLabel: string;
}

@Component({
  selector: 'app-users-roles',
  templateUrl: './users-roles.component.html',
  styleUrl: './users-roles.component.scss',
  standalone: false,
})
export class UsersRolesComponent implements OnInit, OnDestroy {
  readonly pageLead =
    'LDMS role catalog — assign capabilities to user groups and control what each persona can do in the portal.';

  /** Row height for CDK virtual scroll in explorer layout (px). */
  readonly virtualRowHeight = 72;

  fetching = false;
  exporting = false;
  groupId: string | null = null;
  /** Filled when viewing `/users/groups/:id/roles` from `getUserGroupById` for the banner. */
  contextGroupName = '';
  contextGroupDescription = '';
  /**
   * One-shot banner hint from {@link UsersGroupsComponent.goViewGroupRoles} (or profile link state)
   * so the title matches the list after an edit when GET lags or returns a nested envelope.
   */
  private pendingGroupBannerFromNav: { id: number; name: string; description: string } | null = null;
  private routeParamsSub?: Subscription;

  /** Catalog list layout — explorer scales to very large role counts. */
  catalogLayout: 'explorer' | 'table' = 'explorer';
  /** `all` or a {@link RoleRow.moduleKey} value. */
  moduleFilterKey = 'all';
  moduleSidebarSearch = '';
  /** Row targeted by the shared actions menu (avoids brittle {@code matMenuTriggerData}). */
  activeRoleRow: RoleRow | null = null;
  dataSource: RoleRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;
  showSampleCsvInfo = false;
  readonly sampleCsvDescription =
    'Use this template to prepare user role imports. Keep the role and description headers unchanged and provide one role per row.';
  readonly importCsvDisclaimer =
    'CSV import only. Keep `role` and `description` headers unchanged and use unique role names.';

  columnFilters = {
    role: '',
    description: '',
  };

  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;
  totalRecords = 0;
  private readonly reload$ = new Subject<void>();
  private latestLoadToken = 0;
  showCreateModal = false;
  creating = false;
  private pendingRoleMutation = false;
  private pendingCreateTempId: number | null = null;
  private pendingDeleteSnapshot: { row: RoleRow; index: number; didPageBack: boolean } | null = null;
  createError = '';
  createMode: 'create' | 'view' | 'edit' = 'create';
  createModel = { id: 0, role: '', description: '' };

  constructor(
    private readonly title: Title,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly usersService: UsersPortalService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {
    const nav = this.router.getCurrentNavigation();
    const st = nav?.extras?.state as {
      lxGroupId?: unknown;
      lxGroupName?: unknown;
      lxGroupDescription?: unknown;
    } | undefined;
    const gid = Number(st?.lxGroupId);
    if (Number.isFinite(gid) && gid > 0 && (st?.lxGroupName != null || st?.lxGroupDescription != null)) {
      this.pendingGroupBannerFromNav = {
        id: gid,
        name: String(st.lxGroupName ?? '').trim(),
        description: String(st.lxGroupDescription ?? '').trim(),
      };
    }
  }

  get isViewMode(): boolean {
    return this.createMode === 'view';
  }

  get displayedColumns(): string[] {
    const cols = ['module', 'role', 'description', 'status'];
    if (!this.groupId) {
      cols.push('actions');
    }
    return cols;
  }

  get catalogModuleSections(): LdmsRoleModuleSection[] {
    const q = this.moduleSidebarSearch.trim().toLowerCase();
    const all = listLdmsRoleModuleSections();
    if (!q) {
      return all;
    }
    return all.filter(
      (section) =>
        section.label.toLowerCase().includes(q) || section.key.toLowerCase().includes(q),
    );
  }

  get activeModuleSection(): LdmsRoleModuleSection | null {
    if (this.moduleFilterKey === 'all') {
      return null;
    }
    return listLdmsRoleModuleSections().find((s) => s.key === this.moduleFilterKey) ?? null;
  }

  get filteredRows(): RoleRow[] {
    if (!this.isEmbeddedGroupRolesView()) {
      return this.dataSource;
    }
    const q = this.searchQuery.trim().toLowerCase();
    const roleFilter = this.columnFilters.role.trim().toLowerCase();
    const descFilter = this.columnFilters.description.trim().toLowerCase();
    return this.dataSource.filter((row) => {
      if (this.moduleFilterKey !== 'all' && row.moduleKey !== this.moduleFilterKey) {
        return false;
      }
      if (roleFilter && !row.role.toLowerCase().includes(roleFilter)) {
        return false;
      }
      if (descFilter && !row.description.toLowerCase().includes(descFilter)) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        row.role.toLowerCase().includes(q) ||
        row.description.toLowerCase().includes(q) ||
        row.moduleLabel.toLowerCase().includes(q)
      );
    });
  }

  get displayRows(): RoleRow[] {
    if (this.isEmbeddedGroupRolesView()) {
      return this.slicePage(this.filteredRows);
    }
    return this.dataSource;
  }

  get paginationLength(): number {
    if (this.isEmbeddedGroupRolesView()) {
      return this.filteredRows.length;
    }
    return this.totalRecords;
  }

  get catalogRangeLabel(): string {
    if (this.isEmbeddedGroupRolesView()) {
      const total = this.filteredRows.length;
      if (total === 0) {
        return 'No roles on this group';
      }
      const start = this.pageIndex * this.pageSize + 1;
      const end = Math.min((this.pageIndex + 1) * this.pageSize, total);
      return `Showing ${start.toLocaleString()}–${end.toLocaleString()} of ${total.toLocaleString()} group roles`;
    }
    if (this.totalRecords === 0) {
      return 'No roles in catalog';
    }
    const start = this.pageIndex * this.pageSize + 1;
    const end = Math.min((this.pageIndex + 1) * this.pageSize, this.totalRecords);
    return `Showing ${start.toLocaleString()}–${end.toLocaleString()} of ${this.totalRecords.toLocaleString()} roles`;
  }

  get showModuleSidebar(): boolean {
    return this.isEmbeddedGroupRolesView() || (!this.groupId && this.catalogLayout === 'explorer');
  }

  /**
   * Lets {@link UsersGroupsComponent} re-fetch this group by id when the paged list is stale.
   */
  backToGroupsState(): { lxUserGroupListRefresh: { id: number } } | undefined {
    if (this.groupId === null) {
      return undefined;
    }
    const id = Number(this.groupId);
    if (!Number.isFinite(id) || id <= 0) {
      return undefined;
    }
    return {
      lxUserGroupListRefresh: { id },
    };
  }

  resetPaging(): void {
    this.pageIndex = 0;
    if (!this.isEmbeddedGroupRolesView()) {
      this.reload$.next();
    }
    this.cdr.markForCheck();
  }

  selectModuleFilter(key: string): void {
    if (this.moduleFilterKey === key) {
      return;
    }
    this.moduleFilterKey = key;
    this.pageIndex = 0;
    if (!this.isEmbeddedGroupRolesView()) {
      this.reload$.next();
    }
    this.cdr.markForCheck();
  }

  prepareRowActions(event: Event, row: RoleRow): void {
    event.stopPropagation();
    event.preventDefault();
    this.activeRoleRow = row;
  }

  clearRowActions(): void {
    this.activeRoleRow = null;
  }

  trackRole(_index: number, row: RoleRow): number {
    return row.id;
  }

  setCatalogLayout(layout: 'explorer' | 'table'): void {
    if (this.catalogLayout === layout) {
      return;
    }
    this.catalogLayout = layout;
    this.cdr.markForCheck();
  }

  onPage(e: PageEvent): void {
    if (e.pageIndex === this.pageIndex && e.pageSize === this.pageSize) {
      return;
    }
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
    if (!this.isEmbeddedGroupRolesView()) {
      this.reload$.next();
    }
  }

  ngOnInit(): void {
    this.title.setTitle('Roles | LX Admin');
    this.reload$.pipe(debounceTime(150)).subscribe(() => this.loadRows());
    this.routeParamsSub = this.route.paramMap.subscribe((pm) => {
      this.groupId = pm.get('groupId');
      this.reload$.next();
    });
  }

  ngOnDestroy(): void {
    this.routeParamsSub?.unsubscribe();
    this.syncPageModalScrollLock(false);
  }

  refresh(): void {
    this.reload$.next();
  }

  stubImport(): void {}

  exportAs(format: LxExportFormat): void {
    if (this.exporting) {
      return;
    }
    if (this.groupId) {
      const saved = exportClientTableAsCsv(
        format,
        this.dataSource,
        [
          { header: 'role', value: (r) => r.role },
          { header: 'description', value: (r) => r.description },
          { header: 'status', value: (r) => r.statusLabel },
        ],
        'user-group-roles',
        (message) =>
          this.snackBar.open(message, 'Close', {
            duration: 4500,
          }),
        { title: 'Group roles' },
      );
      if (saved) {
        this.snackBar.open(`Exported group roles as ${exportFormatLabel(format)}.`, 'Close', {
          duration: 3500,
          panelClass: ['app-snackbar-success'],
        });
      }
      return;
    }
    this.exporting = true;
    this.usersService
      .exportUserRoles(
        {
          page: this.pageIndex,
          size: this.pageSize,
          searchQuery: this.searchQuery,
          columnFilters: this.columnFilters,
        },
        format,
      )
      .pipe(
        finalize(() => {
          this.exporting = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('user-roles', format));
          this.snackBar.open(`Exported roles as ${format.toUpperCase()}.`, 'Close', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Export failed.', 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  downloadSampleCsv(): void {
    const rows = [
      'role,description',
      'KYC Reviewer,Reviews submitted KYC records',
      'Branch Supervisor,Approves branch-level user actions',
    ].join('\n');
    this.downloadCsv('user-roles-sample.csv', rows);
  }

  openCreateModal(): void {
    this.createMode = 'create';
    this.showCreateModal = true;
    this.syncPageModalScrollLock(true);
    this.createError = '';
    this.createModel = { id: 0, role: '', description: '' };
  }

  viewRow(row: RoleRow): void {
    this.createMode = 'view';
    this.showCreateModal = true;
    this.syncPageModalScrollLock(true);
    this.createError = '';
    this.createModel = { id: row.id, role: row.role, description: row.description };
  }

  editRow(row: RoleRow): void {
    this.createMode = 'edit';
    this.showCreateModal = true;
    this.syncPageModalScrollLock(true);
    this.createError = '';
    this.createModel = { id: row.id, role: row.role, description: row.description };
  }

  deleteRow(row: RoleRow): void {
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: {
          entityLabel: 'user role',
          onConfirm: () => {
            const idx = this.dataSource.findIndex((r) => r.id === row.id);
            let didPageBack = false;
            this.dataSource = this.dataSource.filter((r) => r.id !== row.id);
            if (this.isEmbeddedGroupRolesView()) {
              this.totalRecords = this.dataSource.length;
            } else {
              this.totalRecords = Math.max(0, this.totalRecords - 1);
              if (this.dataSource.length === 0 && this.totalRecords > 0 && this.pageIndex > 0) {
                this.pageIndex -= 1;
                didPageBack = true;
                this.loadRows({ suppressSpinner: true });
              }
            }
            this.pendingDeleteSnapshot = { row: { ...row }, index: idx >= 0 ? idx : 0, didPageBack };
            this.cdr.detectChanges();
          },
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;
        this.usersService.deleteUserRole(row.id).subscribe({
          next: () => {
            this.pendingDeleteSnapshot = null;
            this.snackBar.open('User role deleted successfully.', 'Close', {
              duration: 5000,
              panelClass: ['app-snackbar-success'],
            });
          },
          error: () => {
            if (this.pendingDeleteSnapshot?.didPageBack) {
              this.pageIndex += 1;
              this.pendingDeleteSnapshot = null;
              this.loadRows();
            } else {
              this.rollbackOptimisticDelete();
            }
            this.snackBar.open('Failed to delete user role.', 'Close', {
              duration: 5000,
              panelClass: ['app-snackbar-error'],
            });
          },
        });
      });
  }

  closeCreateModal(): void {
    if (this.pendingRoleMutation) return;
    this.showCreateModal = false;
    this.syncPageModalScrollLock(false);
  }

  submitCreate(): void {
    if (this.createMode === 'view') {
      this.closeCreateModal();
      return;
    }
    if (this.pendingRoleMutation) {
      return;
    }
    const role = this.createModel.role.trim();
    const description = this.createModel.description.trim();
    if (!role || !description) {
      this.createError = 'Role and description are required.';
      return;
    }
    const submittedMode = this.createMode;
    const submittedModel = { ...this.createModel };
    this.createError = '';
    const rowSnapshotBeforeEdit: RoleRow | undefined =
      submittedMode === 'edit'
        ? (() => {
            const r = this.dataSource.find((row) => row.id === submittedModel.id);
            return r ? { ...r } : undefined;
          })()
        : undefined;

    this.showCreateModal = false;
    this.syncPageModalScrollLock(false);
    this.pendingRoleMutation = true;
    if (submittedMode === 'edit') {
      this.applySuccessfulSaveToVisibleRows('edit', submittedModel, null);
    } else {
      const tempId = -Math.abs(Date.now() + Math.floor(Math.random() * 1000));
      this.pendingCreateTempId = tempId;
      this.applyOptimisticCreateRow(tempId, role, description);
    }

    if (submittedMode === 'create' && this.isEmbeddedGroupRolesView()) {
      const groupIdNum = Number(this.groupId);
      this.usersService
        .createUserRole({ role, description })
        .pipe(
          switchMap((response) => {
            const createdId = this.extractCreatedUserRoleId(response);
            if (createdId === null) {
              return throwError(() => new Error('NO_ROLE_ID'));
            }
            return this.usersService.assignUserRolesToUserGroup(groupIdNum, [createdId]).pipe(
              map(() => ({ createdId, assignOk: true as const })),
              catchError(() => of({ createdId, assignOk: false as const })),
            );
          }),
        )
        .subscribe({
          next: ({ createdId, assignOk }) => {
            this.pendingRoleMutation = false;
            this.finalizeOptimisticCreate(createdId, role, description);
            this.pendingCreateTempId = null;
            this.snackBar.open(
              assignOk
                ? 'Role created and linked to this group.'
                : 'Role created, but linking it to this group failed. Use Assign roles to add it.',
              'Close',
              {
                duration: assignOk ? 5000 : 8000,
                panelClass: [assignOk ? 'app-snackbar-success' : 'app-snackbar-error'],
              },
            );
            if (!assignOk) {
              this.loadRows();
            }
            this.cdr.markForCheck();
          },
          error: () => {
            this.pendingRoleMutation = false;
            if (this.pendingCreateTempId !== null) {
              this.rollbackOptimisticCreate();
              this.pendingCreateTempId = null;
            }
            this.createMode = submittedMode;
            this.createModel = submittedModel;
            this.showCreateModal = true;
            this.syncPageModalScrollLock(true);
            this.createError = 'Failed to create user role.';
            this.snackBar.open(this.createError, 'Close', {
              duration: 5000,
              panelClass: ['app-snackbar-error'],
            });
            this.cdr.markForCheck();
          },
        });
      return;
    }

    const request$ =
      submittedMode === 'edit'
        ? this.usersService.updateUserRole({ id: submittedModel.id, role, description })
        : this.usersService.createUserRole({ role, description });
    request$.subscribe({
      next: (response) => {
        this.pendingRoleMutation = false;
        const createdId = submittedMode === 'create' ? this.extractCreatedUserRoleId(response) : null;
        if (submittedMode === 'create') {
          if (createdId !== null) {
            this.finalizeOptimisticCreate(createdId, role, description);
          } else {
            this.rollbackOptimisticCreate();
            this.loadRows();
          }
          this.pendingCreateTempId = null;
        } else {
          this.applySuccessfulSaveToVisibleRows(submittedMode, submittedModel, response, createdId);
        }
        this.snackBar.open(
          submittedMode === 'edit' ? 'User role updated successfully.' : 'User role created successfully.',
          'Close',
          {
            duration: 5000,
            panelClass: ['app-snackbar-success'],
          },
        );
        this.cdr.markForCheck();
      },
      error: () => {
        this.pendingRoleMutation = false;
        if (submittedMode === 'create' && this.pendingCreateTempId !== null) {
          this.rollbackOptimisticCreate();
          this.pendingCreateTempId = null;
        }
        if (submittedMode === 'edit' && rowSnapshotBeforeEdit) {
          const idx = this.dataSource.findIndex((r) => r.id === rowSnapshotBeforeEdit.id);
          if (idx >= 0) {
            const restored = [...this.dataSource];
            restored[idx] = { ...rowSnapshotBeforeEdit };
            this.dataSource = restored;
          }
        }
        this.createMode = submittedMode;
        this.createModel = submittedModel;
        this.showCreateModal = true;
        this.syncPageModalScrollLock(true);
        this.createError = submittedMode === 'edit' ? 'Failed to update user role.' : 'Failed to create user role.';
        this.snackBar.open(this.createError, 'Close', {
          duration: 5000,
          panelClass: ['app-snackbar-error'],
        });
        this.cdr.markForCheck();
      },
    });
  }

  private isEmbeddedGroupRolesView(): boolean {
    if (this.groupId === null) {
      return false;
    }
    const id = Number(this.groupId);
    return Number.isFinite(id);
  }

  private loadRows(opts?: { suppressSpinner?: boolean }): void {
    const suppressSpinner = opts?.suppressSpinner === true;
    const loadToken = ++this.latestLoadToken;
    if (!suppressSpinner) {
      this.fetching = true;
    }
    if (this.groupId) {
      const id = Number(this.groupId);
      if (!Number.isFinite(id)) {
        this.dataSource = [];
        this.totalRecords = 0;
        this.contextGroupName = '';
        this.contextGroupDescription = '';
        this.fetching = false;
        this.cdr.markForCheck();
        return;
      }
      this.contextGroupName = '';
      this.contextGroupDescription = '';
      this.usersService.getUserGroupById(id).subscribe({
        next: (group) => {
          if (loadToken !== this.latestLoadToken) return;
          const src = this.unwrapEmbeddedUserGroup(group);
          if (this.pendingGroupBannerFromNav?.id === id) {
            this.pendingGroupBannerFromNav = null;
          }
          const apiName = String(src['name'] ?? '').trim() || `Group #${id}`;
          const apiDesc = String(src['description'] ?? '').trim();
          this.contextGroupName = apiName;
          this.contextGroupDescription = apiDesc;
          const roles = (src['userRoleDtoSet'] as Record<string, unknown>[] | undefined) ?? [];
          this.dataSource = roles.map((r) => this.mapRecordToRoleRow(r));
          this.totalRecords = this.dataSource.length;
          this.fetching = false;
          this.title.setTitle(`${this.contextGroupName} — Roles | LX Admin`);
          this.cdr.markForCheck();
        },
        error: () => {
          if (loadToken !== this.latestLoadToken) return;
          const hint =
            this.pendingGroupBannerFromNav?.id === id ? this.pendingGroupBannerFromNav : null;
          if (this.pendingGroupBannerFromNav?.id === id) {
            this.pendingGroupBannerFromNav = null;
          }
          this.contextGroupName =
            hint && hint.name.trim().length > 0 ? hint.name.trim() : `Group #${id}`;
          this.contextGroupDescription = hint?.description?.trim() ?? '';
          this.dataSource = [];
          this.totalRecords = 0;
          this.fetching = false;
          this.title.setTitle('Roles | LX Admin');
          this.cdr.markForCheck();
        },
      });
      return;
    }
    this.contextGroupName = '';
    this.contextGroupDescription = '';
    this.usersService
      .queryUserRoles({
        page: this.pageIndex,
        size: this.pageSize,
        searchQuery: this.catalogSearchValue(),
        columnFilters: this.columnFilters,
      })
      .subscribe({
        next: ({ rows, totalElements }) => {
          if (loadToken !== this.latestLoadToken) return;
          if (rows.length === 0 && totalElements > 0 && this.pageIndex > 0) {
            this.pageIndex = 0;
            this.loadRows(opts);
            return;
          }
          this.dataSource = rows.map((r) => this.mapRecordToRoleRow(r));
          this.totalRecords = totalElements;
          this.fetching = false;
          this.title.setTitle('Roles | LX Admin');
          this.cdr.markForCheck();
        },
        error: () => {
          if (loadToken !== this.latestLoadToken) return;
          this.dataSource = [];
          this.totalRecords = 0;
          this.fetching = false;
          this.title.setTitle('Roles | LX Admin');
          this.cdr.markForCheck();
        },
      });
  }

  /** Backend may return the group as a flat object or nested under `userGroupDto`. */
  private unwrapEmbeddedUserGroup(group: Record<string, unknown> | null): Record<string, unknown> {
    if (!group) return {};
    const nested = group['userGroupDto'];
    if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
      return nested as Record<string, unknown>;
    }
    return group;
  }

  private mapRecordToRoleRow(r: Record<string, unknown>): RoleRow {
    const role = String(r['role'] ?? '');
    return this.buildRoleRow(
      Number(r['id'] ?? 0),
      role,
      String(r['description'] ?? '—'),
      String(r['entityStatus'] ?? 'active').toLowerCase(),
      String(r['entityStatus'] ?? 'Active'),
      String(r['moduleKey'] ?? r['module_key'] ?? ''),
      String(r['moduleLabel'] ?? r['module_label'] ?? ''),
    );
  }

  private buildRoleRow(
    id: number,
    role: string,
    description: string,
    status: string,
    statusLabel: string,
    moduleKey = '',
    moduleLabel = '',
  ): RoleRow {
    const module = moduleSectionFromApi(role, moduleKey, moduleLabel);
    return {
      id,
      role,
      description,
      moduleKey: module.key,
      moduleLabel: module.label,
      status,
      statusLabel,
    };
  }

  private downloadCsv(filename: string, contents: string): void {
    const blob = new Blob([contents], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  private applySuccessfulSaveToVisibleRows(
    mode: 'create' | 'edit',
    model: { id: number; role: string; description: string },
    response: unknown,
    knownCreatedId?: number | null,
  ): void {
    if (mode === 'edit') {
      const idx = this.dataSource.findIndex((r) => r.id === model.id);
      if (idx >= 0) {
        const updated = [...this.dataSource];
        updated[idx] = {
          ...updated[idx],
          role: model.role,
          description: model.description,
        };
        this.dataSource = updated;
      }
      return;
    }

    const createdId = knownCreatedId ?? this.extractCreatedUserRoleId(response);
    if (!createdId) {
      return;
    }
    const nextRow = this.buildRoleRow(
      createdId,
      model.role.trim(),
      model.description.trim(),
      'active',
      'Active',
    );
    this.appendRoleRowBottom(nextRow);
  }

  private applyOptimisticCreateRow(tempId: number, role: string, description: string): void {
    const nextRow = this.buildRoleRow(tempId, role, description, 'active', 'Active');
    this.appendRoleRowBottom(nextRow);
    this.cdr.detectChanges();
  }

  /** Append row at bottom; in server-paged mode trim to pageSize from the end and increment total. */
  private appendRoleRowBottom(row: RoleRow): void {
    if (this.isEmbeddedGroupRolesView()) {
      const withoutDup = this.dataSource.filter((r) => r.id !== row.id);
      this.dataSource = [...withoutDup, row];
      this.totalRecords = this.dataSource.length;
      return;
    }
    const withoutDup = this.dataSource.filter((r) => r.id !== row.id);
    let next = [...withoutDup, row];
    if (next.length > this.pageSize) {
      next = next.slice(next.length - this.pageSize);
    }
    this.dataSource = next;
    this.totalRecords += 1;
  }

  private finalizeOptimisticCreate(createdId: number, role: string, description: string): void {
    const temp = this.pendingCreateTempId;
    if (temp === null) {
      return;
    }
    const idx = this.dataSource.findIndex((r) => r.id === temp);
    if (idx >= 0) {
      const next = [...this.dataSource];
      next[idx] = this.buildRoleRow(createdId, role, description, 'active', 'Active');
      this.dataSource = next;
      if (this.isEmbeddedGroupRolesView()) {
        this.totalRecords = this.dataSource.length;
      }
    } else {
      const row = this.buildRoleRow(createdId, role, description, 'active', 'Active');
      const noTemp = this.dataSource.filter((r) => r.id !== temp && r.id !== createdId);
      if (this.isEmbeddedGroupRolesView()) {
        this.dataSource = [...noTemp, row];
        this.totalRecords = this.dataSource.length;
      } else {
        let next = [...noTemp, row];
        if (next.length > this.pageSize) {
          next = next.slice(next.length - this.pageSize);
        }
        this.dataSource = next;
      }
    }
    this.cdr.detectChanges();
  }

  private rollbackOptimisticCreate(): void {
    const temp = this.pendingCreateTempId;
    if (temp === null) {
      return;
    }
    this.dataSource = this.dataSource.filter((r) => r.id !== temp);
    if (this.isEmbeddedGroupRolesView()) {
      this.totalRecords = this.dataSource.length;
    } else {
      this.totalRecords = Math.max(0, this.totalRecords - 1);
    }
  }

  private rollbackOptimisticDelete(): void {
    const snap = this.pendingDeleteSnapshot;
    if (!snap) {
      return;
    }
    const restored = [...this.dataSource];
    const insertAt = Math.min(snap.index, restored.length);
    restored.splice(insertAt, 0, snap.row);
    if (this.isEmbeddedGroupRolesView()) {
      this.dataSource = restored;
      this.totalRecords = this.dataSource.length;
    } else {
      this.dataSource = restored.slice(0, this.pageSize);
      this.totalRecords += 1;
    }
    this.pendingDeleteSnapshot = null;
    this.cdr.detectChanges();
  }

  private normalizeMaybeStringJson(value: unknown): unknown {
    if (typeof value !== 'string') {
      return value;
    }
    const trimmed = value.trim();
    if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
      return value;
    }
    try {
      return JSON.parse(trimmed) as unknown;
    } catch {
      return value;
    }
  }

  private extractCreatedUserRoleId(response: unknown): number | null {
    const normalized = this.normalizeMaybeStringJson(response);
    if (!normalized || typeof normalized !== 'object') {
      return null;
    }
    const fromTree = this.findNestedUserRoleDtoId(normalized);
    if (fromTree !== null) {
      return fromTree;
    }
    const root = normalized as Record<string, unknown>;
    const data = this.asRecord(root['data']) ?? root;
    const userRoleDto = this.asRecord(data['userRoleDto']);
    const userRole = this.asRecord(data['userRole']);
    const candidates = [userRoleDto?.['id'], userRole?.['id'], data['id'], root['id']];
    for (const candidate of candidates) {
      const parsed = Number(candidate);
      if (Number.isFinite(parsed) && parsed > 0) {
        return parsed;
      }
    }
    return null;
  }

  private findNestedUserRoleDtoId(value: unknown, depth = 0): number | null {
    if (depth > 12 || value === null || typeof value !== 'object') {
      return null;
    }
    const rec = value as Record<string, unknown>;
    const inner = this.asRecord(rec['userRoleDto']);
    if (inner) {
      const id = Number(inner['id']);
      if (Number.isFinite(id) && id > 0) {
        return id;
      }
    }
    for (const nested of Object.values(rec)) {
      if (nested !== null && typeof nested === 'object') {
        const found = this.findNestedUserRoleDtoId(nested, depth + 1);
        if (found !== null) {
          return found;
        }
      }
    }
    return null;
  }

  private asRecord(value: unknown): Record<string, unknown> | null {
    return value !== null && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }

  private catalogSearchValue(): string {
    const q = this.searchQuery.trim();
    if (this.isEmbeddedGroupRolesView()) {
      return q;
    }
    const hint =
      this.moduleFilterKey !== 'all' ? moduleCatalogSearchHint(this.moduleFilterKey).trim() : '';
    if (!hint) {
      return q;
    }
    if (!q) {
      return hint;
    }
    return `${q} ${hint}`;
  }

  private slicePage(rows: RoleRow[]): RoleRow[] {
    const start = this.pageIndex * this.pageSize;
    return rows.slice(start, start + this.pageSize);
  }

  private syncPageModalScrollLock(open: boolean): void {
    if (typeof document === 'undefined') {
      return;
    }
    document.body.classList.toggle('lx-page-modal-open', open);
  }
}
