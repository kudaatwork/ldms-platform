import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { UsersAdminService } from '../../services/users-admin.service';
import { PageEvent } from '@angular/material/paginator';
import { Subject, Subscription, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { AssignRolesDialogComponent } from '../../components/assign-roles-dialog/assign-roles-dialog.component';

interface UserGroupRow {
  id: number;
  name: string;
  description: string;
  users: number;
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
  private static readonly ROWS_CACHE_KEY = 'lx.admin.users.userGroups.rows';
  private viewGroupQuerySub?: Subscription;
  fetching = false;
  displayedColumns = ['name', 'description', 'users', 'status', 'actions'];
  searchQuery = '';
  filterFieldsOpen = false;
  showSampleCsvInfo = false;
  readonly sampleCsvDescription =
    'Use this template to prepare user group imports. Keep the name and description headers unchanged and provide one group per row.';
  readonly importCsvDisclaimer =
    'CSV import only. Keep `name` and `description` headers unchanged and avoid duplicate group names.';
  columnFilters = { name: '', description: '' };
  pageIndex = 0;
  pageSize = 10;
  totalRecords = 0;
  private readonly reload$ = new Subject<void>();
  private latestLoadToken = 0;

  groups: UserGroupRow[] = [];
  showCreateModal = false;
  creating = false;
  private pendingGroupMutation = false;
  private pendingCreateTempId: number | null = null;
  private pendingDeleteSnapshot: { row: UserGroupRow; index: number; didPageBack: boolean } | null = null;
  createError = '';
  createMode: 'create' | 'view' | 'edit' = 'create';
  createModel = { id: 0, name: '', description: '' };

  get isViewMode(): boolean {
    return this.createMode === 'view';
  }

  constructor(
    private readonly title: Title,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly usersService: UsersAdminService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('User Groups | LX Admin');
    this.restoreCachedRows();
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
            this.viewRow({
              id: Number(dto['id'] ?? id),
              name,
              description,
              users: 0,
              status: 'active',
              statusLabel: 'Active',
            });
            this.cdr.markForCheck();
          });
      });
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

  stubExport(): void {}

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

  openAssignRolesDialog(row: UserGroupRow): void {
    this.dialog.open(AssignRolesDialogComponent, {
      width: '560px',
      maxWidth: '94vw',
      data: { userGroupId: row.id, groupLabel: row.name },
    });
  }

  deleteRow(row: UserGroupRow): void {
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
          next: () => {
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
          this.totalRecords = totalElements;
          this.persistRowsCache();
          this.fetching = false;
        },
        error: () => {
          if (loadToken !== this.latestLoadToken) return;
          this.fetching = false;
        },
      });
  }

  private mapRecordToUserGroupRow(r: Record<string, unknown>): UserGroupRow {
    return {
      id: Number(r['id'] ?? 0),
      name: String(r['name'] ?? ''),
      description: String(r['description'] ?? '—'),
      users: Array.isArray(r['userDtoSet']) ? (r['userDtoSet'] as unknown[]).length : 0,
      status: 'active',
      statusLabel: 'Active',
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

  private persistRowsCache(): void {
    try {
      localStorage.setItem(UsersGroupsComponent.ROWS_CACHE_KEY, JSON.stringify(this.groups));
    } catch {
      // best effort cache only
    }
  }

  private restoreCachedRows(): void {
    try {
      const raw = localStorage.getItem(UsersGroupsComponent.ROWS_CACHE_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) return;
      const restored = parsed
        .map((r) => {
          const rec = r as Record<string, unknown>;
          return {
            id: Number(rec['id'] ?? 0),
            name: String(rec['name'] ?? ''),
            description: String(rec['description'] ?? '—'),
            users: Number(rec['users'] ?? 0),
            status: String(rec['status'] ?? 'active'),
            statusLabel: String(rec['statusLabel'] ?? 'Active'),
          };
        })
        .filter((r) => Number.isFinite(r.id) && r.id > 0 && r.name.trim().length > 0);
      if (restored.length === 0) return;
      this.groups = restored;
      this.totalRecords = Math.max(this.totalRecords, restored.length);
    } catch {
      // ignore cache parse errors
    }
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
        status: 'active',
        statusLabel: 'Active',
      };
      this.groups = next;
    } else {
      const filtered = this.groups.filter((r) => r.id !== createdId);
      let next = [
        ...filtered,
        { id: createdId, name, description, users: 0, status: 'active', statusLabel: 'Active' },
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
}
