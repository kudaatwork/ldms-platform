import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { UsersPortalService } from '../../services/users-portal.service';
import { PageEvent } from '@angular/material/paginator';
import { Subject, debounceTime, finalize } from 'rxjs';
import {
  LxExportFormat,
  downloadBlob,
  exportFilename,
} from '@shared/utils/lx-export.util';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';

interface UserTypeRow {
  id: number;
  name: string;
  description: string;
  status: string;
  statusLabel: string;
}

@Component({
  selector: 'app-user-types',
  templateUrl: './user-types.component.html',
  styleUrl: './user-types.component.scss',
  standalone: false,
})
export class UserTypesComponent implements OnInit {
  readonly pageLead =
    'Classify users by persona (admin, driver, receiver, etc.) to drive onboarding flows and default permissions.';

  private static readonly ROWS_CACHE_KEY = 'lx.admin.users.userTypes.rows.v2';
  fetching = false;
  exporting = false;
  displayedColumns = ['name', 'description', 'status', 'actions'];
  searchQuery = '';
  filterFieldsOpen = false;
  showSampleCsvInfo = false;
  readonly sampleCsvDescription =
    'Use this template to prepare user type imports. Keep the userTypeName and description headers unchanged and provide one type per row.';
  readonly importCsvDisclaimer =
    'CSV import only. Keep `userTypeName` and `description` headers unchanged and ensure type names are unique.';
  columnFilters = { userTypeName: '', description: '' };
  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;
  totalRecords = 0;
  private readonly reload$ = new Subject<void>();
  private latestLoadToken = 0;
  rows: UserTypeRow[] = [];
  showCreateModal = false;
  creating = false;
  /** Prevents overlapping save requests while modal is dismissed and HTTP is in flight */
  private pendingUserTypeMutation = false;
  /** Negative id for optimistic create row until POST returns real id */
  private pendingCreateTempId: number | null = null;
  /** Row removed optimistically during delete; restored on HTTP error */
  private pendingDeleteSnapshot: { row: UserTypeRow; index: number; didPageBack: boolean } | null = null;
  createError = '';
  createMode: 'create' | 'view' | 'edit' = 'create';
  createModel = { id: 0, userTypeName: '', description: '' };

  get isViewMode(): boolean {
    return this.createMode === 'view';
  }

  constructor(
    private readonly title: Title,
    private readonly usersService: UsersPortalService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('User Types | LX Admin');
    this.restoreCachedRows();
    this.reload$.pipe(debounceTime(150)).subscribe(() => this.loadRows());
    this.reload$.next();
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
      .exportUserTypes(
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
          downloadBlob(blob, exportFilename('user-types', format));
          this.snackBar.open(`Exported user types as ${format.toUpperCase()}.`, 'Close', {
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
      'userTypeName,description',
      'Driver,Mobile application driver account',
      'Receiver,Warehouse goods receiving account',
    ].join('\n');
    this.downloadCsv('user-types-sample.csv', rows);
  }

  openCreateModal(): void {
    this.createMode = 'create';
    this.showCreateModal = true;
    this.createError = '';
    this.createModel = { id: 0, userTypeName: '', description: '' };
  }

  viewRow(row: UserTypeRow): void {
    this.createMode = 'view';
    this.showCreateModal = true;
    this.createError = '';
    this.createModel = { id: row.id, userTypeName: row.name, description: row.description };
  }

  editRow(row: UserTypeRow): void {
    this.createMode = 'edit';
    this.showCreateModal = true;
    this.createError = '';
    this.createModel = { id: row.id, userTypeName: row.name, description: row.description };
  }

  deleteRow(row: UserTypeRow): void {
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: {
          entityLabel: 'user type',
          onConfirm: () => {
            const idx = this.rows.findIndex((r) => r.id === row.id);
            let didPageBack = false;
            this.rows = this.rows.filter((r) => r.id !== row.id);
            this.totalRecords = Math.max(0, this.totalRecords - 1);
            if (this.rows.length === 0 && this.totalRecords > 0 && this.pageIndex > 0) {
              this.pageIndex -= 1;
              didPageBack = true;
              this.loadRows({ suppressSpinner: true });
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
        this.usersService.deleteUserType(row.id).subscribe({
          next: () => {
            this.pendingDeleteSnapshot = null;
            this.snackBar.open('User type deleted successfully.', 'Close', {
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
            this.snackBar.open('Failed to delete user type.', 'Close', {
              duration: 5000,
              panelClass: ['app-snackbar-error'],
            });
          },
        });
      });
  }

  closeCreateModal(): void {
    if (this.creating) return;
    this.showCreateModal = false;
  }

  submitCreate(): void {
    if (this.createMode === 'view') {
      this.closeCreateModal();
      return;
    }
    if (this.pendingUserTypeMutation) {
      return;
    }
    const userTypeName = this.createModel.userTypeName.trim();
    const description = this.createModel.description.trim();
    if (!userTypeName || !description) {
      this.createError = 'Type name and description are required.';
      return;
    }
    const submittedMode = this.createMode;
    const submittedModel = { ...this.createModel };
    this.createError = '';
    const rowSnapshotBeforeEdit: UserTypeRow | undefined =
      submittedMode === 'edit'
        ? (() => {
            const r = this.rows.find((row) => row.id === submittedModel.id);
            return r ? { ...r } : undefined;
          })()
        : undefined;

    // Dismiss modal immediately so the UI is not blocked on backend latency ("Saving..." state).
    this.showCreateModal = false;
    this.pendingUserTypeMutation = true;
    if (submittedMode === 'edit') {
      this.applySuccessfulSaveToVisibleRows('edit', submittedModel, null);
    } else {
      const tempId = -Math.abs(Date.now() + Math.floor(Math.random() * 1000));
      this.pendingCreateTempId = tempId;
      this.applyOptimisticCreateRow(tempId, userTypeName, description);
    }

    const request$ =
      submittedMode === 'edit'
        ? this.usersService.updateUserType({ id: submittedModel.id, userTypeName, description })
        : this.usersService.createUserType({ userTypeName, description });
    request$.subscribe({
      next: (response) => {
        this.pendingUserTypeMutation = false;
        const createdId = submittedMode === 'create' ? this.extractCreatedUserTypeId(response) : null;
        if (submittedMode === 'create') {
          if (createdId !== null) {
            this.finalizeOptimisticCreate(createdId, userTypeName, description);
          } else {
            this.rollbackOptimisticCreate();
            this.loadRows();
          }
          this.pendingCreateTempId = null;
        } else {
          this.applySuccessfulSaveToVisibleRows(submittedMode, submittedModel, response, createdId);
        }
        this.snackBar.open(
          submittedMode === 'edit' ? 'User type updated successfully.' : 'User type created successfully.',
          'Close',
          {
            duration: 5000,
            panelClass: ['app-snackbar-success'],
          },
        );
      },
      error: () => {
        this.pendingUserTypeMutation = false;
        if (submittedMode === 'create' && this.pendingCreateTempId !== null) {
          this.rollbackOptimisticCreate();
          this.pendingCreateTempId = null;
        }
        if (submittedMode === 'edit' && rowSnapshotBeforeEdit) {
          const idx = this.rows.findIndex((r) => r.id === rowSnapshotBeforeEdit.id);
          if (idx >= 0) {
            const restored = [...this.rows];
            restored[idx] = { ...rowSnapshotBeforeEdit };
            this.rows = restored;
            this.persistRowsCache();
          }
        }
        this.createMode = submittedMode;
        this.createModel = submittedModel;
        this.showCreateModal = true;
        this.createError = submittedMode === 'edit' ? 'Failed to update user type.' : 'Failed to create user type.';
        this.snackBar.open(this.createError, 'Close', {
          duration: 5000,
          panelClass: ['app-snackbar-error'],
        });
      },
    });
  }

  private loadRows(opts?: { suppressSpinner?: boolean }): void {
    const suppressSpinner = opts?.suppressSpinner === true;
    const loadToken = ++this.latestLoadToken;
    if (!suppressSpinner) {
      this.fetching = true;
    }
    this.usersService
      .queryUserTypes({
        page: this.pageIndex,
        size: this.pageSize,
        searchQuery: this.searchQuery,
        columnFilters: this.columnFilters,
      })
      .subscribe({
        next: ({ rows, totalElements }) => {
          if (loadToken !== this.latestLoadToken) {
            return;
          }
          if (rows.length === 0 && totalElements > 0 && this.pageIndex > 0) {
            this.pageIndex = 0;
            this.loadRows(opts);
            return;
          }
          this.rows = rows.map((r) => this.mapRecordToUserTypeRow(r));
          this.totalRecords = totalElements;
          this.persistRowsCache();
          this.fetching = false;
        },
        error: () => {
          if (loadToken !== this.latestLoadToken) {
            return;
          }
          this.fetching = false;
        },
      });
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
      localStorage.setItem(UserTypesComponent.ROWS_CACHE_KEY, JSON.stringify(this.rows));
    } catch {
      // best effort cache only
    }
  }

  private restoreCachedRows(): void {
    try {
      const raw = localStorage.getItem(UserTypesComponent.ROWS_CACHE_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw) as unknown;
      if (!Array.isArray(parsed)) return;
      const restored = parsed
        .map((r) => {
          const rec = r as Record<string, unknown>;
          const statusRaw = String(rec['status'] ?? rec['entityStatus'] ?? 'active').toLowerCase();
          return {
            id: Number(rec['id'] ?? 0),
            name: String(rec['name'] ?? ''),
            description: String(rec['description'] ?? '—'),
            status: statusRaw,
            statusLabel: String(rec['statusLabel'] ?? this.readableStatus(statusRaw)),
          };
        })
        .filter((r) => Number.isFinite(r.id) && r.id > 0 && r.name.trim().length > 0);
      if (restored.length === 0) return;
      this.rows = restored;
      this.totalRecords = Math.max(this.totalRecords, restored.length);
    } catch {
      // ignore cache parse errors
    }
  }

  private applySuccessfulSaveToVisibleRows(
    mode: 'create' | 'edit',
    model: { id: number; userTypeName: string; description: string },
    response: unknown,
    knownCreatedId?: number | null,
  ): void {
    if (mode === 'edit') {
      const idx = this.rows.findIndex((r) => r.id === model.id);
      if (idx >= 0) {
        const updated = [...this.rows];
        updated[idx] = {
          ...updated[idx],
          name: model.userTypeName,
          description: model.description,
        };
        this.rows = updated;
        this.persistRowsCache();
      }
      return;
    }

    const createdId = knownCreatedId ?? this.extractCreatedUserTypeId(response);
    // For create, update the visible table immediately when we can resolve a backend id.
    if (!createdId) {
      return;
    }
    const nextRow: UserTypeRow = {
      id: createdId,
      name: model.userTypeName.trim(),
      description: model.description.trim(),
      status: 'active',
      statusLabel: 'Active',
    };
    const withoutDup = this.rows.filter((r) => r.id !== createdId);
    let next = [...withoutDup, nextRow];
    if (next.length > this.pageSize) {
      next = next.slice(next.length - this.pageSize);
    }
    this.rows = next;
    this.totalRecords += 1;
    this.persistRowsCache();
  }

  private applyOptimisticCreateRow(tempId: number, name: string, description: string): void {
    const nextRow: UserTypeRow = { id: tempId, name, description, status: 'active', statusLabel: 'Active' };
    const withoutDup = this.rows.filter((r) => r.id !== tempId);
    let next = [...withoutDup, nextRow];
    if (next.length > this.pageSize) {
      next = next.slice(next.length - this.pageSize);
    }
    this.rows = next;
    this.totalRecords += 1;
    this.persistRowsCache();
    this.cdr.detectChanges();
  }

  private finalizeOptimisticCreate(createdId: number, name: string, description: string): void {
    const temp = this.pendingCreateTempId;
    if (temp === null) {
      return;
    }
    const idx = this.rows.findIndex((r) => r.id === temp);
    if (idx >= 0) {
      const next = [...this.rows];
      next[idx] = { id: createdId, name, description, status: 'active', statusLabel: 'Active' };
      this.rows = next;
    } else {
      const filtered = this.rows.filter((r) => r.id !== createdId);
      let next = [...filtered, { id: createdId, name, description, status: 'active', statusLabel: 'Active' }];
      if (next.length > this.pageSize) {
        next = next.slice(next.length - this.pageSize);
      }
      this.rows = next;
    }
    this.persistRowsCache();
    this.cdr.detectChanges();
  }

  private rollbackOptimisticCreate(): void {
    const temp = this.pendingCreateTempId;
    if (temp === null) {
      return;
    }
    this.rows = this.rows.filter((r) => r.id !== temp);
    this.totalRecords = Math.max(0, this.totalRecords - 1);
    this.persistRowsCache();
  }

  private rollbackOptimisticDelete(): void {
    const snap = this.pendingDeleteSnapshot;
    if (!snap) {
      return;
    }
    const restored = [...this.rows];
    const insertAt = Math.min(snap.index, restored.length);
    restored.splice(insertAt, 0, snap.row);
    this.rows = restored.slice(0, this.pageSize);
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

  /** Same payload shapes as queries: flat `UserTypeResponse`, `{ data: ... }`, nested JSON, sometimes stringified bodies. */
  private extractCreatedUserTypeId(response: unknown): number | null {
    const normalized = this.normalizeMaybeStringJson(response);
    if (!normalized || typeof normalized !== 'object') {
      return null;
    }
    const fromTree = this.findNestedUserTypeDtoId(normalized);
    if (fromTree !== null) {
      return fromTree;
    }
    const root = normalized as Record<string, unknown>;
    const data = this.asRecord(root['data']) ?? root;
    const userTypeDto = this.asRecord(data['userTypeDto']);
    const userType = this.asRecord(data['userType']);
    const candidates = [userTypeDto?.['id'], userType?.['id'], data['id'], root['id']];
    for (const candidate of candidates) {
      const parsed = Number(candidate);
      if (Number.isFinite(parsed) && parsed > 0) {
        return parsed;
      }
    }
    return null;
  }

  private findNestedUserTypeDtoId(value: unknown, depth = 0): number | null {
    if (depth > 12 || value === null || typeof value !== 'object') {
      return null;
    }
    const rec = value as Record<string, unknown>;
    const inner = this.asRecord(rec['userTypeDto']);
    if (inner) {
      const id = Number(inner['id']);
      if (Number.isFinite(id) && id > 0) {
        return id;
      }
    }
    for (const nested of Object.values(rec)) {
      if (nested !== null && typeof nested === 'object') {
        const found = this.findNestedUserTypeDtoId(nested, depth + 1);
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

  private mapRecordToUserTypeRow(r: Record<string, unknown>): UserTypeRow {
    const statusRaw = String(r['entityStatus'] ?? 'ACTIVE').toLowerCase();
    return {
      id: Number(r['id'] ?? 0),
      name: String(r['userTypeName'] ?? ''),
      description: String(r['description'] ?? '—'),
      status: statusRaw,
      statusLabel: this.readableStatus(statusRaw),
    };
  }

  private readableStatus(raw: string): string {
    if (raw === 'active') return 'Active';
    if (raw === 'inactive') return 'Inactive';
    if (raw === 'deleted') return 'Deleted';
    return 'Pending';
  }
}
