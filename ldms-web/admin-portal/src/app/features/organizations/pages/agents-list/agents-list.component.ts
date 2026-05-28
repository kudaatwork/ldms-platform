import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableDataSource } from '@angular/material/table';
import { Title } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  debounceTime,
  finalize,
  merge,
  of,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs';
import { DEFAULT_TABLE_PAGE_SIZE } from '@shared/constants/table-pagination';
import { downloadBlob, exportFilename } from '@shared/utils/lx-export.util';
import { DeleteConfirmDialogComponent } from '@shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { OrganizationsAdminService } from '../../services/organizations-admin.service';
import type { AgentListRow } from '../../models/organization-directory.model';
import {
  AgentFormDialogComponent,
  type AgentFormDialogData,
  type AgentFormDialogResult,
} from '../agent-form-dialog/agent-form-dialog.component';

@Component({
  selector: 'app-agents-list',
  templateUrl: './agents-list.component.html',
  styleUrl: './agents-list.component.scss',
  standalone: false,
})
export class AgentsListComponent implements OnInit, OnDestroy {
  readonly pageLead =
    'Clearing agents, transport representatives, and organisation delegates linked to parent organisations and branches.';

  fetching = false;
  actionInProgress = false;
  exporting = false;
  loadError = '';
  searchQuery = '';
  filterFieldsOpen = false;
  agentKindFilter = '';
  columnFilters = { organizationName: '', role: '' };

  showSampleCsvInfo = false;
  readonly sampleCsvDescription =
    'Agent imports use ORGANIZATION_ID, agent kind, names, role, type, and contact columns. Leave ID empty when creating new rows.';
  readonly importCsvTooltip =
    'Import accepts CSV only (UTF-8). ORGANIZATION_ID must match an existing organisation.';
  readonly importCsvDisclaimer =
    'CSV import only. Provide ORGANIZATION_ID that matches an existing organisation. Leave ID column empty on create.';

  readonly agentKindOptions = [
    { value: '', label: 'All kinds' },
    { value: 'INDIVIDUAL', label: 'Individual' },
    { value: 'ORGANIZATION', label: 'Organisation' },
  ];

  displayedColumns = ['agent', 'organization', 'kind', 'role', 'contact', 'status', 'actions'];
  dataSource = new MatTableDataSource<AgentListRow>([]);
  totalRecords = 0;
  pageIndex = 0;
  pageSize = DEFAULT_TABLE_PAGE_SIZE;

  private readonly destroy$ = new Subject<void>();
  private readonly filterReload$ = new Subject<void>();
  private latestLoadToken = 0;
  private lastFilterSignature = '';

  constructor(
    private readonly title: Title,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly orgService: OrganizationsAdminService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Agents | LX Admin');
    this.lastFilterSignature = this.currentFilterSignature();
    merge(of(undefined as void), this.filterReload$.pipe(debounceTime(150)))
      .pipe(
        switchMap(() => {
          this.pageIndex = 0;
          return this.runTableQuery({ background: false });
        }),
        takeUntil(this.destroy$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  applyFilters(): void {
    const next = this.currentFilterSignature();
    if (next === this.lastFilterSignature) return;
    this.lastFilterSignature = next;
    this.filterReload$.next();
  }

  onPage(e: PageEvent): void {
    if (e.pageIndex === this.pageIndex && e.pageSize === this.pageSize) return;
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
    this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  refresh(): void {
    this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
  }

  kindLabel(kind: string): string {
    if (kind === 'INDIVIDUAL') return 'Individual';
    if (kind === 'ORGANIZATION') return 'Organisation';
    return kind || '—';
  }

  hasActiveFilters(): boolean {
    return (
      !!this.searchQuery.trim() ||
      !!this.agentKindFilter ||
      !!this.columnFilters.organizationName.trim() ||
      !!this.columnFilters.role.trim()
    );
  }

  // ─── CRUD Actions ────────────────────────────────────────────────────────────

  openCreate(): void {
    this.openFormDialog({ action: 'create' });
  }

  openEdit(row: AgentListRow): void {
    this.openFormDialog({ action: 'edit', row });
  }

  viewRow(row: AgentListRow): void {
    this.openFormDialog({ action: 'view', row });
  }

  confirmDelete(row: AgentListRow): void {
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: { entityLabel: `agent "${row.displayName}"` },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;
        this.actionInProgress = true;
        this.orgService
          .deleteAgent(row.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.showSuccess('Agent deleted.');
              this.actionInProgress = false;
              this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
            },
            error: (err: Error) => {
              this.showError(err.message ?? 'Delete failed.');
              this.actionInProgress = false;
            },
          });
      });
  }

  // ─── Import / Export / Sample CSV ────────────────────────────────────────────

  importCsv(input: HTMLInputElement): void {
    input.click();
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.actionInProgress = true;
    this.orgService
      .importAgentsCsv(file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          if (response.ok) {
            this.showSuccess(response.message ?? 'Import completed successfully.');
          } else {
            this.showError(response.message ?? 'Import failed.');
          }
          input.value = '';
          this.actionInProgress = false;
          setTimeout(
            () => this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe(),
            250,
          );
        },
        error: (err: Error) => {
          this.showError(err.message ?? 'Import failed.');
          input.value = '';
          this.actionInProgress = false;
          setTimeout(
            () => this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe(),
            250,
          );
        },
      });
  }

  downloadSampleCsv(): void {
    const template = this.orgService.getAgentSampleCsv();
    this.download(template.blob, template.filename);
    this.showSuccess('Sample CSV downloaded.');
  }

  exportAs(format: 'csv' | 'xlsx' | 'pdf'): void {
    this.exporting = true;
    this.orgService
      .exportAgents(format, {
        page: 0,
        size: this.pageSize,
        searchQuery: [this.searchQuery.trim(), this.columnFilters.role.trim()].filter(Boolean).join(' '),
        organizationName: this.columnFilters.organizationName,
        agentKind: this.agentKindFilter,
        role: this.columnFilters.role,
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.exporting = false)),
      )
      .subscribe({
        next: (blob) => {
          downloadBlob(blob, exportFilename('agents', format));
          this.showSuccess(`Exported agents as ${format.toUpperCase()}.`);
        },
        error: (err: Error) => {
          this.showError(err.message ?? 'Export failed.');
        },
      });
  }

  // ─── Private helpers ──────────────────────────────────────────────────────────

  private openFormDialog(data: AgentFormDialogData): void {
    const ref = this.dialog.open(AgentFormDialogComponent, {
      width: '640px',
      maxWidth: '95vw',
      panelClass: 'lx-location-dialog-panel',
      autoFocus: 'first-tabbable',
      disableClose: true,
      data,
    });
    ref.afterClosed().subscribe((result: AgentFormDialogResult | undefined) => {
      if (result?.saved) {
        this.runTableQuery({ background: false }).pipe(takeUntil(this.destroy$)).subscribe();
      }
    });
  }

  private runTableQuery(opts?: {
    background?: boolean;
  }): Observable<{ rows: AgentListRow[]; totalElements: number }> {
    const loadToken = ++this.latestLoadToken;
    if (!opts?.background || this.totalRecords === 0) {
      this.fetching = true;
    }
    this.loadError = '';
    return this.orgService
      .queryAgentsPage({
        page: this.pageIndex,
        size: this.pageSize,
        searchQuery: [this.searchQuery.trim(), this.columnFilters.role.trim()].filter(Boolean).join(' '),
        organizationName: this.columnFilters.organizationName,
        agentKind: this.agentKindFilter,
        role: this.columnFilters.role,
      })
      .pipe(
        tap(({ rows, totalElements }) => this.applyLoadedPage(loadToken, rows, totalElements)),
        catchError((err: Error) => {
          if (loadToken !== this.latestLoadToken) return EMPTY;
          this.loadError = err.message ?? 'Failed to load agents';
          this.dataSource.data = [];
          this.totalRecords = 0;
          return EMPTY;
        }),
        finalize(() => {
          if (loadToken === this.latestLoadToken) {
            this.fetching = false;
            this.cdr.markForCheck();
          }
        }),
      );
  }

  private applyLoadedPage(loadToken: number, rows: AgentListRow[], totalElements: number): void {
    if (loadToken !== this.latestLoadToken) return;
    if (rows.length === 0 && totalElements > 0 && this.pageIndex > 0) {
      this.pageIndex = 0;
      this.runTableQuery({ background: true }).pipe(takeUntil(this.destroy$)).subscribe();
      return;
    }
    this.totalRecords = totalElements > 0 ? totalElements : rows.length;
    this.dataSource.data = rows;
    this.cdr.markForCheck();
  }

  private currentFilterSignature(): string {
    return JSON.stringify({
      q: this.searchQuery.trim(),
      kind: this.agentKindFilter,
      filters: this.columnFilters,
    });
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', { duration: 4000, panelClass: ['app-snackbar-success'] });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', { duration: 5000, panelClass: ['app-snackbar-error'] });
  }
}
