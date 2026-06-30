import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { filter, finalize, switchMap, takeUntil } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import {
  downloadBlob,
  exportFilename,
  exportRowsAsCsv,
  LxExportFormat,
} from '../../../../shared/utils/lx-export.util';
import {
  DepartmentDialogComponent,
  DepartmentDialogData,
} from '../../../inventory/components/department-dialog/department-dialog.component';
import { InventoryPortalService } from '../../../inventory/services/inventory-portal.service';
import type { DepartmentRow } from '../../../inventory/models/inventory.model';

@Component({
  selector: 'app-departments-workspace',
  templateUrl: './departments-workspace.component.html',
  styleUrl: './departments-workspace.component.scss',
  standalone: false,
})
export class DepartmentsWorkspaceComponent implements OnInit, OnDestroy {
  loading = true;
  refreshing = false;
  loadError = '';
  actionInProgress = false;
  exporting = false;
  importing = false;
  deletingId: number | null = null;
  departments: DepartmentRow[] = [];
  searchQuery = '';
  filterFieldsOpen = false;
  showSampleCsvInfo = false;

  columnFilters = {
    name: '',
    code: '',
    description: '',
  };

  readonly sampleCsvDescription =
    'Columns: NAME (required), DEPARTMENT_CODE (optional), DESCRIPTION (optional). Rows are scoped to your signed-in organisation.';
  readonly importCsvDisclaimer =
    'CSV import only. Duplicate names or codes within your organisation are skipped and reported in the import summary.';

  private readonly destroy$ = new Subject<void>();
  private latestLoadToken = 0;

  constructor(
    private readonly title: Title,
    private readonly dialog: MatDialog,
    private readonly inventoryService: InventoryPortalService,
    private readonly notifications: NotificationService,
    private readonly authState: AuthStateService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Departments | LX Platform');
    this.loadDepartments();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get orgName(): string {
    return this.authState.currentUser?.orgName ?? 'Your organisation';
  }

  get showInitialLoading(): boolean {
    return this.loading && !this.departments.length;
  }

  get showTable(): boolean {
    return !this.showInitialLoading;
  }

  get filteredDepartments(): DepartmentRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    const nameFilter = this.columnFilters.name.trim().toLowerCase();
    const codeFilter = this.columnFilters.code.trim().toLowerCase();
    const descriptionFilter = this.columnFilters.description.trim().toLowerCase();

    return this.departments.filter((row) => {
      const hay = `${row.name} ${row.code} ${row.description}`.toLowerCase();
      if (q && !hay.includes(q)) {
        return false;
      }
      if (nameFilter && !row.name.toLowerCase().includes(nameFilter)) {
        return false;
      }
      if (codeFilter && !(row.code ?? '').toLowerCase().includes(codeFilter)) {
        return false;
      }
      if (descriptionFilter && !(row.description ?? '').toLowerCase().includes(descriptionFilter)) {
        return false;
      }
      return true;
    });
  }

  hasActiveFilters(): boolean {
    return (
      !!this.searchQuery.trim() ||
      !!this.columnFilters.name.trim() ||
      !!this.columnFilters.code.trim() ||
      !!this.columnFilters.description.trim()
    );
  }

  refresh(): void {
    this.loadDepartments({ background: this.departments.length > 0 });
  }

  importCsv(input: HTMLInputElement): void {
    input.click();
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || this.importing) {
      return;
    }

    this.importing = true;
    this.actionInProgress = true;
    this.inventoryService
      .importDepartmentsFromCsv(file)
      .pipe(
        finalize(() => {
          this.importing = false;
          this.actionInProgress = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (summary) => {
          if (summary.imported > 0) {
            this.notifications.success(summary.message || `Imported ${summary.imported} department(s).`);
            this.loadDepartments({ background: true });
          } else {
            const detail = summary.errors[0] ?? summary.message ?? 'No rows were imported.';
            this.notifications.error(detail);
          }
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Import failed.'),
      });
  }

  downloadSampleCsv(): void {
    const blob = exportRowsAsCsv(
      [{ name: 'Finance', code: 'FIN', description: 'Accounts payable and receivable' }],
      [
        { header: 'NAME', value: (r) => r.name },
        { header: 'DEPARTMENT_CODE', value: (r) => r.code },
        { header: 'DESCRIPTION', value: (r) => r.description },
      ],
    );
    downloadBlob(blob, 'departments-import-template.csv');
    this.notifications.success('Sample CSV downloaded.');
  }

  exportAs(format: LxExportFormat): void {
    if (this.exporting) {
      return;
    }
    this.exporting = true;
    this.inventoryService
      .exportDepartments(
        format,
        this.inventoryService.buildDepartmentExportFilters(this.searchQuery, this.columnFilters),
      )
      .pipe(
        finalize(() => {
          this.exporting = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('departments', format));
          this.notifications.success(`Exported departments as ${format.toUpperCase()}.`);
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Export failed.'),
      });
  }

  openAddDepartment(): void {
    this.dialog
      .open(DepartmentDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: { mode: 'create' } satisfies DepartmentDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((row: DepartmentRow | undefined) => {
        if (row) {
          this.notifications.success(`Department "${row.name}" was created.`);
          this.loadDepartments({ background: true });
        }
      });
  }

  openEditDepartment(row: DepartmentRow): void {
    this.dialog
      .open(DepartmentDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: { mode: 'edit', department: row } satisfies DepartmentDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((updated: DepartmentRow | undefined) => {
        if (updated) {
          this.notifications.success(`Department "${updated.name}" was updated.`);
          this.loadDepartments({ background: true });
        }
      });
  }

  onDeleteDepartment(row: DepartmentRow): void {
    if (row.inUse) {
      this.notifications.error(
        'This department cannot be deleted because it is used on one or more purchase requisitions.',
      );
      return;
    }

    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: {
          entityLabel: `department "${row.name}"`,
          message:
            'Only departments that have never been used on a requisition can be deleted. This action cannot be undone.',
        },
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed === true),
        switchMap(() => {
          this.actionInProgress = true;
          this.deletingId = row.id;
          this.cdr.markForCheck();
          return this.inventoryService.deleteDepartment(row.id).pipe(
            finalize(() => {
              this.actionInProgress = false;
              this.deletingId = null;
              this.cdr.markForCheck();
            }),
          );
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success(`Department "${row.name}" was deleted.`);
          this.loadDepartments({ background: true });
        },
        error: (err: Error) => this.notifications.error(err.message ?? 'Could not delete department.'),
      });
  }

  isDeleting(row: DepartmentRow): boolean {
    return this.deletingId === row.id;
  }

  trackByDepartmentId(_i: number, row: DepartmentRow): number {
    return row.id;
  }

  private loadDepartments(opts?: { background?: boolean }): void {
    const background = opts?.background === true && this.departments.length > 0;
    const loadToken = ++this.latestLoadToken;

    if (background) {
      this.refreshing = true;
    } else {
      this.loading = true;
    }
    this.loadError = '';

    this.inventoryService
      .listDepartments()
      .pipe(
        finalize(() => {
          if (loadToken !== this.latestLoadToken) {
            return;
          }
          this.loading = false;
          this.refreshing = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          if (loadToken !== this.latestLoadToken) {
            return;
          }
          this.departments = rows;
          this.loadError = '';
        },
        error: (err: Error) => {
          if (loadToken !== this.latestLoadToken) {
            return;
          }
          this.loadError = err.message ?? 'Could not load departments.';
          if (!background) {
            this.departments = [];
          }
        },
      });
  }
}
