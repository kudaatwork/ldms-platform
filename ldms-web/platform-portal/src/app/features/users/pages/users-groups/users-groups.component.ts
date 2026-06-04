import { Location } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { UsersPortalService } from '../../services/users-portal.service';
import { PageEvent } from '@angular/material/paginator';
import { Subject, Subscription, debounceTime, distinctUntilChanged, finalize, map, of, switchMap } from 'rxjs';
import {
  LxExportFormat,
  downloadBlob,
  exportFilename,
} from '@shared/utils/lx-export.util';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { AssignRolesDialogComponent } from '../../components/assign-roles-dialog/assign-roles-dialog.component';
import {
  UserGroupMembersDialogComponent,
  UserGroupMembersDialogResult,
} from '../../components/user-group-members-dialog/user-group-members-dialog.component';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';

interface UserGroupRow {
  id: number;
  name: string;
  description: string;
  users: number;
  roles: number;
  status: string;
  statusLabel: string;
}

@Component({
  selector: 'app-users-groups',
  templateUrl: './users-groups.component.html',
  styleUrl: './users-groups.component.scss',
  standalone: false,
})
export class UsersGroupsComponent implements OnInit, OnDestroy {
  readonly pageLead =
    'Your organisation workspace starts with the provisioned Administrator group. Create additional groups here for roles unique to your organisation.';

  /** Bumped when row shape changes (e.g. member counts) so stale localStorage does not keep `users: 0`. */
  private static readonly ROWS_CACHE_KEY = 'lx.admin.users.userGroups.rows.v3';
  private viewGroupQuerySub?: Subscription;
  fetching = false;
  exporting = false;
  displayedColumns = ['name', 'description', 'users', 'roles', 'status', 'actions'];
  searchQuery = '';
  filterFieldsOpen = false;
  showSampleCsvInfo = false;
  readonly sampleCsvDescription =
    'Use this template to prepare user group imports. Keep the name and description headers unchanged and provide one group per row.';
  readonly importCsvDisclaimer =
    'CSV import only. Keep `name` and `description` headers unchanged and avoid duplicate group names.';
  columnFilters = { name: '', description: '' };
  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;
  totalRecords = 0;
  private readonly reload$ = new Subject<void>();
  private latestLoadToken = 0;

  groups: UserGroupRow[] = [];
  showCreateModal = false;
  creating = false;
  private pendingGroupMutation = false;
  private pendingCreateTempId: number | null = null;
  private pendingDeleteSnapshot: { row: UserGroupRow; index: number; didPageBack: boolean } | null = null;
  /**
   * When returning from `/users/groups/:id/roles`, re-fetch that row by id so name/description
   * match the server instead of stale router state.
   */
  private pendingListRefreshFromRoles: { id: number } | null = null;
  createError = '';
  createMode: 'create' | 'view' | 'edit' = 'create';
  createModel = { id: 0, name: '', description: '' };

  get isViewMode(): boolean {
    return this.createMode === 'view';
  }

  isProvisionedAdministratorGroup(row: UserGroupRow): boolean {
    return row.name.trim().toLowerCase() === 'administrator';
  }

  constructor(
    private readonly title: Title,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly location: Location,
    private readonly usersService: UsersPortalService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.readPendingGroupListRefreshFromLocationState();
    this.title.setTitle('User Groups | LX Admin');
    this.reload$.pipe(debounceTime(150)).subscribe(() => this.loadGroups());
    this.reload$.next();
    this.viewGroupQuerySub = this.route.queryParamMap
      .pipe(
        map((qm) => {
          const raw = qm.get('viewGroupId');
          if (!raw?.trim()) {
            return 0;
          }
          const id = Number(raw);
          return Number.isFinite(id) && id > 0 ? id : 0;
        }),
        distinctUntilChanged(),
        switchMap((id) =>
          id ? this.usersService.getUserGroupById(id).pipe(map((dto) => ({ id, dto }))) : of(null),
        ),
      )
      .subscribe((res) => {
        if (!res?.id) {
          return;
        }
        const { id, dto } = res;
        void this.router
          .navigate([], {
            relativeTo: this.route,
            queryParams: { viewGroupId: null },
            queryParamsHandling: 'merge',
            replaceUrl: true,
          })
          .then(() => {
            if (!dto) {
              this.snackBar.open('User group could not be loaded.', 'Close', { duration: 5000 });
              return;
            }
            const name = String(dto['name'] ?? '').trim() || 'User group';
            const description = String(dto['description'] ?? '').trim();
            const row = this.mapRecordToUserGroupRow(dto as Record<string, unknown>);
            this.viewRow({
              id: Number(dto['id'] ?? id),
              name,
              description,
              users: row.users,
              roles: row.roles,
              status: 'active',
              statusLabel: 'Active',
            });
            this.cdr.markForCheck();
          });
      });
  }

  private readPendingGroupListRefreshFromLocationState(): void {
    const raw = this.location.getState() as {
      lxUserGroupListRefresh?: { id: unknown; name?: unknown; description?: unknown };
    } | null;
    const r = raw?.lxUserGroupListRefresh;
    const id = Number(r?.id);
    if (!Number.isFinite(id) || id <= 0) {
      return;
    }
    this.pendingListRefreshFromRoles = { id };
  }

  ngOnDestroy(): void {
    this.viewGroupQuerySub?.unsubscribe();
  }

  resetPaging(): void {
    this.pageIndex = 0;
    this.reload$.next();
  }

  onPage(e: PageEvent): void {
    if (e.pageIndex === this.pageIndex && e.pageSize === this.pageSize) {
      return;
    }
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
    this.reload$.next();
  }

  refresh(): void {
    this.reload$.next();
  }

  stubImport(): void {}

  exportAs(format: LxExportFormat): void {
    if (this.exporting) {
      return;
    }
    this.exporting = true;
    this.usersService
      .exportUserGroups(
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
          downloadBlob(blob, exportFilename('user-groups', format));
          this.snackBar.open(`Exported user groups as ${format.toUpperCase()}.`, 'Close', {
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
      'name,description',
      'Operations Managers,Oversees regional operational workflows',
      'Compliance Team,Handles audit and KYC compliance activities',
    ].join('\n');
    this.downloadCsv('user-groups-sample.csv', rows);
  }

  openCreateModal(): void {
    this.createMode = 'create';
    this.showCreateModal = true;
    this.createError = '';
    this.createModel = { id: 0, name: '', description: '' };
  }

  viewRow(row: UserGroupRow): void {
    this.createMode = 'view';
    this.showCreateModal = true;
    this.createError = '';
    this.createModel = { id: row.id, name: row.name, description: row.description };
  }

  editRow(row: UserGroupRow): void {
    this.createMode = 'edit';
    this.showCreateModal = true;
    this.createError = '';
    this.createModel = { id: row.id, name: row.name, description: row.description };
  }

  /** Navigate with latest list row + state so the group-roles screen banner matches what you just edited. */
  goViewGroupRoles(row: UserGroupRow): void {
    if (!Number.isFinite(row.id) || row.id <= 0) {
      return;
    }
    const fresh = this.groups.find((g) => g.id === row.id) ?? row;
    void this.router.navigate(['/users', 'groups', fresh.id, 'roles'], {
      state: {
        lxGroupId: fresh.id,
        lxGroupName: fresh.name,
        lxGroupDescription: fresh.description,
      },
    });
  }

  openAssignRolesDialog(row: UserGroupRow): void {
    const fresh = this.groups.find((g) => g.id === row.id) ?? row;
    this.dialog
      .open(AssignRolesDialogComponent, {
        width: '840px',
        maxWidth: '96vw',
        maxHeight: '92vh',
        panelClass: 'lx-location-dialog-panel',
        data: { userGroupId: fresh.id, groupLabel: fresh.name },
      })
      .afterClosed()
      .subscribe((changed) => {
        if (changed) {
          this.reload$.next();
        }
      });
  }

  openManageUsersInGroupDialog(row: UserGroupRow): void {
    if (!Number.isFinite(row.id) || row.id <= 0) {
      return;
    }
    this.dialog
      .open(UserGroupMembersDialogComponent, {
        width: '840px',
        maxWidth: '96vw',
        maxHeight: '92vh',
        autoFocus: 'first-tabbable',
        panelClass: 'lx-location-dialog-panel',
        data: { userGroupId: row.id, groupLabel: row.name },
      })
      .afterClosed()
      .subscribe((result) => {
        this.onManageUsersDialogClosed(result);
      });
  }

  private onManageUsersDialogClosed(result: UserGroupMembersDialogResult | false | undefined): void {
    if (!result) {
      return;
    }
    if (result.memberCounts && Object.keys(result.memberCounts).length > 0) {
      this.applyMemberCountPatches(result.memberCounts);
    }
    if (result.changed) {
      this.reload$.next();
    }
  }

  private applyMemberCountPatches(patches: Record<number, { users: number; roles: number }>): void {
    let updated = false;
    const next = this.groups.map((row) => {
      const patch = patches[row.id];
      if (!patch) {
        return row;
      }
      updated = true;
      return { ...row, users: patch.users, roles: patch.roles };
    });
    if (updated) {
      this.groups = next;
      this.persistRowsCache();
      this.cdr.markForCheck();
    }
  }

  deleteRow(row: UserGroupRow): void {
    if (this.isProvisionedAdministratorGroup(row)) {
      this.snackBar.open('The Administrator workspace group cannot be deleted.', 'Close', {
        duration: 5000,
        panelClass: ['app-snackbar-error'],
      });
      return;
    }
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: {
          entityLabel: 'user group',
          onConfirm: () => {
            const idx = this.groups.findIndex((r) => r.id === row.id);
            let didPageBack = false;
            this.groups = this.groups.filter((r) => r.id !== row.id);
            this.totalRecords = Math.max(0, this.totalRecords - 1);
            if (this.groups.length === 0 && this.totalRecords > 0 && this.pageIndex > 0) {
              this.pageIndex -= 1;
              didPageBack = true;
              this.loadGroups({ suppressSpinner: true });
            } else {
              this.persistRowsCache();
            }
            this.pendingDeleteSnapshot = { row: { ...row }, index: idx >= 0 ? idx : 0, didPageBack };
            this.cdr.detectChanges();
          },
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;
        this.usersService.deleteUserGroup(row.id).subscribe({
          next: (resp) => {
            if (this.isUserGroupMutationFailure(resp)) {
              if (this.pendingDeleteSnapshot?.didPageBack) {
                this.pageIndex += 1;
                this.pendingDeleteSnapshot = null;
                this.loadGroups();
              } else {
                this.rollbackOptimisticDelete();
              }
              this.snackBar.open(this.formatUserGroupMutationError(resp, 'Failed to delete user group.'), 'Close', {
                duration: 5000,
                panelClass: ['app-snackbar-error'],
              });
              return;
            }
            this.pendingDeleteSnapshot = null;
            this.snackBar.open('User group deleted successfully.', 'Close', {
              duration: 5000,
              panelClass: ['app-snackbar-success'],
            });
          },
          error: () => {
            if (this.pendingDeleteSnapshot?.didPageBack) {
              this.pageIndex += 1;
              this.pendingDeleteSnapshot = null;
              this.loadGroups();
            } else {
              this.rollbackOptimisticDelete();
            }
            this.snackBar.open('Failed to delete user group.', 'Close', {
              duration: 5000,
              panelClass: ['app-snackbar-error'],
            });
          },
        });
      });
  }

  closeCreateModal(): void {
    if (this.pendingGroupMutation) return;
    this.showCreateModal = false;
  }

  submitCreate(): void {
    if (this.createMode === 'view') {
      this.closeCreateModal();
      return;
    }
    if (this.pendingGroupMutation) {
      return;
    }
    const name = this.createModel.name.trim();
    const description = this.createModel.description.trim();
    if (!name || !description) {
      this.createError = 'Name and description are required.';
      return;
    }
    const submittedMode = this.createMode;
    if (submittedMode !== 'edit' && name.trim().toLowerCase() === 'administrator') {
      this.createError =
        'Administrator is reserved for your organisation workspace. Choose another group name.';
      return;
    }
    const submittedModel = { ...this.createModel };
    this.createError = '';
    const rowSnapshotBeforeEdit: UserGroupRow | undefined =
      submittedMode === 'edit'
        ? (() => {
            const r = this.groups.find((row) => row.id === submittedModel.id);
            return r ? { ...r } : undefined;
          })()
        : undefined;

    this.showCreateModal = false;
    this.pendingGroupMutation = true;
    if (submittedMode === 'edit') {
      this.applySuccessfulSaveToVisibleRows('edit', submittedModel, null);
    } else {
      const tempId = -Math.abs(Date.now() + Math.floor(Math.random() * 1000));
      this.pendingCreateTempId = tempId;
      this.applyOptimisticCreateRow(tempId, name, description);
    }

    const request$ =
      submittedMode === 'edit'
        ? this.usersService.updateUserGroup({ id: submittedModel.id, name, description })
        : this.usersService.createUserGroup({ name, description });
    request$.subscribe({
      next: (response) => {
        this.pendingGroupMutation = false;
        if (submittedMode === 'edit' && this.isUserGroupMutationFailure(response)) {
          if (rowSnapshotBeforeEdit) {
            const idx = this.groups.findIndex((r) => r.id === rowSnapshotBeforeEdit.id);
            if (idx >= 0) {
              const restored = [...this.groups];
              restored[idx] = { ...rowSnapshotBeforeEdit };
              this.groups = restored;
              this.persistRowsCache();
            }
          }
          this.createMode = submittedMode;
          this.createModel = submittedModel;
          this.showCreateModal = true;
          this.createError = this.formatUserGroupMutationError(response, 'Failed to update user group.');
          this.snackBar.open(this.createError, 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
          return;
        }
        const createdId = submittedMode === 'create' ? this.extractCreatedUserGroupId(response) : null;
        if (submittedMode === 'create') {
          if (createdId !== null) {
            this.finalizeOptimisticCreate(createdId, name, description);
          } else {
            this.rollbackOptimisticCreate();
            this.loadGroups();
          }
          this.pendingCreateTempId = null;
        } else {
          this.applySuccessfulSaveToVisibleRows(submittedMode, submittedModel, response, createdId);
        }
        this.snackBar.open(
          submittedMode === 'edit' ? 'User group updated successfully.' : 'User group created successfully.',
          'Close',
          {
            duration: 5000,
            panelClass: ['app-snackbar-success'],
          },
        );
      },
      error: () => {
        this.pendingGroupMutation = false;
        if (submittedMode === 'create' && this.pendingCreateTempId !== null) {
          this.rollbackOptimisticCreate();
          this.pendingCreateTempId = null;
        }
        if (submittedMode === 'edit' && rowSnapshotBeforeEdit) {
          const idx = this.groups.findIndex((r) => r.id === rowSnapshotBeforeEdit.id);
          if (idx >= 0) {
            const restored = [...this.groups];
            restored[idx] = { ...rowSnapshotBeforeEdit };
            this.groups = restored;
            this.persistRowsCache();
          }
        }
        this.createMode = submittedMode;
        this.createModel = submittedModel;
        this.showCreateModal = true;
        this.createError = submittedMode === 'edit' ? 'Failed to update user group.' : 'Failed to create user group.';
        this.snackBar.open(this.createError, 'Close', {
          duration: 5000,
          panelClass: ['app-snackbar-error'],
        });
      },
    });
  }

  private loadGroups(opts?: { suppressSpinner?: boolean }): void {
    const suppressSpinner = opts?.suppressSpinner === true;
    const loadToken = ++this.latestLoadToken;
    if (!suppressSpinner) {
      this.fetching = true;
    }
    this.usersService
      .queryUserGroups({
        page: this.pageIndex,
        size: this.pageSize,
        searchQuery: this.searchQuery,
        columnFilters: this.columnFilters,
      })
      .subscribe({
        next: ({ rows, totalElements }) => {
          if (loadToken !== this.latestLoadToken) return;
          if (rows.length === 0 && totalElements > 0 && this.pageIndex > 0) {
            this.pageIndex = 0;
            this.loadGroups(opts);
            return;
          }
          this.groups = rows.map((r) => this.mapRecordToUserGroupRow(r));
          this.applyPendingListRefreshFromRolesAfterLoad();
          this.totalRecords = totalElements;
          this.persistRowsCache();
          this.fetching = false;
          this.cdr.markForCheck();
        },
        error: () => {
          if (loadToken !== this.latestLoadToken) return;
          this.fetching = false;
          this.snackBar.open('Could not load user groups. Check your connection and try Refresh.', 'Close', {
            duration: 6000,
            panelClass: ['app-snackbar-error'],
          });
          this.cdr.markForCheck();
        },
      });
  }

  /** API rows are usually flat `UserGroupDto`; some gateways wrap as `userGroupDto`. */
  private resolveUserGroupPayload(r: Record<string, unknown>): Record<string, unknown> {
    const nested = this.asRecord(r['userGroupDto']);
    return nested ?? r;
  }

  private mapRecordToUserGroupRow(r: Record<string, unknown>): UserGroupRow {
    const src = this.resolveUserGroupPayload(r);
    const users = this.resolveMemberCount(
      src['userMemberCount'] ?? src['user_member_count'],
      Array.isArray(src['userDtoSet']) ? (src['userDtoSet'] as unknown[]).length : null,
    );
    const roles = this.resolveMemberCount(
      src['userRoleMemberCount'] ?? src['user_role_member_count'],
      Array.isArray(src['userRoleDtoSet']) ? (src['userRoleDtoSet'] as unknown[]).length : null,
    );
    return {
      id: Number(src['id'] ?? r['id'] ?? 0),
      name: String(src['name'] ?? ''),
      description: String(src['description'] ?? '—'),
      users,
      roles,
      status: 'active',
      statusLabel: 'Active',
    };
  }

  private resolveMemberCount(rawCount: unknown, legacyListLength: number | null): number {
    if (rawCount !== null && rawCount !== undefined && rawCount !== '') {
      const parsed =
        typeof rawCount === 'number' && Number.isFinite(rawCount) ? Math.trunc(rawCount) : Number(rawCount);
      if (Number.isFinite(parsed) && parsed >= 0) {
        return parsed;
      }
    }
    return legacyListLength != null ? legacyListLength : 0;
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

  private persistRowsCache(): void {
    try {
      localStorage.setItem(UsersGroupsComponent.ROWS_CACHE_KEY, JSON.stringify(this.groups));
    } catch {
      // best effort cache only
    }
  }

  /** Re-fetch one group by id after returning from the roles screen (avoids stale router state). */
  private applyPendingListRefreshFromRolesAfterLoad(): void {
    const p = this.pendingListRefreshFromRoles;
    if (!p) {
      return;
    }
    this.pendingListRefreshFromRoles = null;
    const idx = this.groups.findIndex((g) => g.id === p.id);
    if (idx < 0) {
      return;
    }
    this.usersService.getUserGroupById(p.id).subscribe({
      next: (dto) => {
        if (!dto) {
          return;
        }
        const rowIdx = this.groups.findIndex((g) => g.id === p.id);
        if (rowIdx < 0) {
          return;
        }
        const mapped = this.mapRecordToUserGroupRow(dto as Record<string, unknown>);
        const next = [...this.groups];
        next[rowIdx] = {
          ...next[rowIdx],
          name: mapped.name || next[rowIdx].name,
          description: mapped.description || next[rowIdx].description,
          users: mapped.users,
          roles: mapped.roles,
        };
        this.groups = next;
        this.persistRowsCache();
        this.cdr.markForCheck();
      },
    });
  }

  private applySuccessfulSaveToVisibleRows(
    mode: 'create' | 'edit',
    model: { id: number; name: string; description: string },
    response: unknown,
    knownCreatedId?: number | null,
  ): void {
    if (mode === 'edit') {
      const idx = this.groups.findIndex((r) => r.id === model.id);
      if (idx >= 0) {
        const updated = [...this.groups];
        updated[idx] = {
          ...updated[idx],
          name: model.name,
          description: model.description,
        };
        this.groups = updated;
        this.persistRowsCache();
      }
      return;
    }

    const createdId = knownCreatedId ?? this.extractCreatedUserGroupId(response);
    if (!createdId) {
      return;
    }
    const nextRow: UserGroupRow = {
      id: createdId,
      name: model.name.trim(),
      description: model.description.trim(),
      users: 0,
      roles: 0,
      status: 'active',
      statusLabel: 'Active',
    };
    const withoutDup = this.groups.filter((r) => r.id !== createdId);
    let next = [...withoutDup, nextRow];
    if (next.length > this.pageSize) {
      next = next.slice(next.length - this.pageSize);
    }
    this.groups = next;
    this.totalRecords += 1;
    this.persistRowsCache();
  }

  private applyOptimisticCreateRow(tempId: number, name: string, description: string): void {
    const nextRow: UserGroupRow = {
      id: tempId,
      name,
      description,
      users: 0,
      roles: 0,
      status: 'active',
      statusLabel: 'Active',
    };
    const withoutDup = this.groups.filter((r) => r.id !== tempId);
    let next = [...withoutDup, nextRow];
    if (next.length > this.pageSize) {
      next = next.slice(next.length - this.pageSize);
    }
    this.groups = next;
    this.totalRecords += 1;
    this.persistRowsCache();
    this.cdr.detectChanges();
  }

  private finalizeOptimisticCreate(createdId: number, name: string, description: string): void {
    const temp = this.pendingCreateTempId;
    if (temp === null) {
      return;
    }
    const idx = this.groups.findIndex((r) => r.id === temp);
    if (idx >= 0) {
      const next = [...this.groups];
      next[idx] = {
        id: createdId,
        name,
        description,
        users: 0,
        roles: 0,
        status: 'active',
        statusLabel: 'Active',
      };
      this.groups = next;
    } else {
      const filtered = this.groups.filter((r) => r.id !== createdId);
      let next = [
        ...filtered,
        { id: createdId, name, description, users: 0, roles: 0, status: 'active', statusLabel: 'Active' },
      ];
      if (next.length > this.pageSize) {
        next = next.slice(next.length - this.pageSize);
      }
      this.groups = next;
    }
    this.persistRowsCache();
    this.cdr.detectChanges();
  }

  private rollbackOptimisticCreate(): void {
    const temp = this.pendingCreateTempId;
    if (temp === null) {
      return;
    }
    this.groups = this.groups.filter((r) => r.id !== temp);
    this.totalRecords = Math.max(0, this.totalRecords - 1);
    this.persistRowsCache();
  }

  private rollbackOptimisticDelete(): void {
    const snap = this.pendingDeleteSnapshot;
    if (!snap) {
      return;
    }
    const restored = [...this.groups];
    const insertAt = Math.min(snap.index, restored.length);
    restored.splice(insertAt, 0, snap.row);
    this.groups = restored.slice(0, this.pageSize);
    this.totalRecords += 1;
    this.pendingDeleteSnapshot = null;
    this.persistRowsCache();
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

  private extractCreatedUserGroupId(response: unknown): number | null {
    const normalized = this.normalizeMaybeStringJson(response);
    if (!normalized || typeof normalized !== 'object') {
      return null;
    }
    const fromTree = this.findNestedUserGroupDtoId(normalized);
    if (fromTree !== null) {
      return fromTree;
    }
    const root = normalized as Record<string, unknown>;
    const data = this.asRecord(root['data']) ?? root;
    const userGroupDto = this.asRecord(data['userGroupDto']);
    const userGroup = this.asRecord(data['userGroup']);
    const candidates = [userGroupDto?.['id'], userGroup?.['id'], data['id'], root['id']];
    for (const candidate of candidates) {
      const parsed = Number(candidate);
      if (Number.isFinite(parsed) && parsed > 0) {
        return parsed;
      }
    }
    return null;
  }

  private findNestedUserGroupDtoId(value: unknown, depth = 0): number | null {
    if (depth > 12 || value === null || typeof value !== 'object') {
      return null;
    }
    const rec = value as Record<string, unknown>;
    const inner = this.asRecord(rec['userGroupDto']);
    if (inner) {
      const id = Number(inner['id']);
      if (Number.isFinite(id) && id > 0) {
        return id;
      }
    }
    for (const nested of Object.values(rec)) {
      if (nested !== null && typeof nested === 'object') {
        const found = this.findNestedUserGroupDtoId(nested, depth + 1);
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

  private isUserGroupMutationFailure(resp: unknown): boolean {
    if (resp === null || typeof resp !== 'object') {
      return false;
    }
    const r = resp as Record<string, unknown>;
    if (r['success'] === false || r['isSuccess'] === false) {
      return true;
    }
    const statusCode = r['statusCode'];
    return typeof statusCode === 'number' && statusCode >= 400;
  }

  private formatUserGroupMutationError(resp: unknown, fallback: string): string {
    if (resp !== null && typeof resp === 'object') {
      const r = resp as Record<string, unknown>;
      const messages = r['errorMessages'];
      if (Array.isArray(messages) && messages.length > 0) {
        return messages.map((m) => String(m)).join(' ');
      }
      if (typeof r['message'] === 'string' && r['message'].trim()) {
        return r['message'].trim();
      }
    }
    return fallback;
  }
}
