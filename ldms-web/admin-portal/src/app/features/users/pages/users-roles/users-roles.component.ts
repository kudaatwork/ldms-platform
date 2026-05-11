import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UsersAdminService } from '../../services/users-admin.service';
import { Subject, Subscription, catchError, debounceTime, map, of, switchMap, throwError } from 'rxjs';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';

export interface RoleRow {
  id: number;
  role: string;
  description: string;
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
  fetching = false;
  groupId: string | null = null;
  /** Filled when viewing `/users/groups/:id/roles` from `getUserGroupById` for the banner. */
  contextGroupName = '';
  contextGroupDescription = '';
  private routeParamsSub?: Subscription;

  displayedColumns = ['role', 'description', 'status', 'actions'];

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
  pageSize = 10;
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
    private readonly usersService: UsersAdminService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  get isViewMode(): boolean {
    return this.createMode === 'view';
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
  }

  refresh(): void {
    this.reload$.next();
  }

  stubImport(): void {}

  stubExport(): void {}

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
    this.createError = '';
    this.createModel = { id: 0, role: '', description: '' };
  }

  viewRow(row: RoleRow): void {
    this.createMode = 'view';
    this.showCreateModal = true;
    this.createError = '';
    this.createModel = { id: row.id, role: row.role, description: row.description };
  }

  editRow(row: RoleRow): void {
    this.createMode = 'edit';
    this.showCreateModal = true;
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
          this.contextGroupName = String(group?.['name'] ?? '').trim() || `Group #${id}`;
          this.contextGroupDescription = String(group?.['description'] ?? '').trim();
          const roles = (group?.['userRoleDtoSet'] as Record<string, unknown>[] | undefined) ?? [];
          this.dataSource = roles.map((r) => this.mapRecordToRoleRow(r));
          this.totalRecords = this.dataSource.length;
          this.fetching = false;
          this.title.setTitle(`${this.contextGroupName} — Roles | LX Admin`);
          this.cdr.markForCheck();
        },
        error: () => {
          if (loadToken !== this.latestLoadToken) return;
          this.contextGroupName = `Group #${id}`;
          this.contextGroupDescription = '';
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
        searchQuery: this.searchQuery,
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

  private mapRecordToRoleRow(r: Record<string, unknown>): RoleRow {
    return {
      id: Number(r['id'] ?? 0),
      role: String(r['role'] ?? ''),
      description: String(r['description'] ?? '—'),
      status: String(r['entityStatus'] ?? 'active').toLowerCase(),
      statusLabel: String(r['entityStatus'] ?? 'Active'),
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
    const nextRow: RoleRow = {
      id: createdId,
      role: model.role.trim(),
      description: model.description.trim(),
      status: 'active',
      statusLabel: 'Active',
    };
    this.appendRoleRowBottom(nextRow);
  }

  private applyOptimisticCreateRow(tempId: number, role: string, description: string): void {
    const nextRow: RoleRow = {
      id: tempId,
      role,
      description,
      status: 'active',
      statusLabel: 'Active',
    };
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
      next[idx] = {
        id: createdId,
        role,
        description,
        status: 'active',
        statusLabel: 'Active',
      };
      this.dataSource = next;
      if (this.isEmbeddedGroupRolesView()) {
        this.totalRecords = this.dataSource.length;
      }
    } else {
      const row: RoleRow = { id: createdId, role, description, status: 'active', statusLabel: 'Active' };
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
}
