import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, finalize, takeUntil } from 'rxjs';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import {
  exportClientTableAsCsv,
  exportFormatLabel,
  LxExportFormat,
} from '../../../../shared/utils/lx-export.util';
import {
  AgentFormDialogComponent,
  AgentFormDialogData,
  AgentFormDialogResult,
} from '../../components/agent-form-dialog/agent-form-dialog.component';
import { AgentRow } from '../../models/org-management.model';
import { OrgManagementPortalService } from '../../services/org-management-portal.service';
import { AGENT_EXPORT_COLUMNS, downloadAgentSampleCsv } from '../../utils/org-management-export.util';

@Component({
  selector: 'app-org-agents-page',
  templateUrl: './org-agents-page.component.html',
  styleUrl: './org-agents-page.component.scss',
  standalone: false,
})
export class OrgAgentsPageComponent implements OnInit, OnDestroy {
  loading = true;
  actionInProgress = false;
  exporting = false;
  error = '';
  agents: AgentRow[] = [];

  searchQuery = '';
  filterFieldsOpen = false;
  showSampleCsvInfo = false;
  columnFilters = {
    fullName: '',
    role: '',
    region: '',
    agentKind: '' as '' | AgentRow['agentKind'],
    active: '' as boolean | '',
  };

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly orgMgmt: OrgManagementPortalService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  get sampleCsvDescription(): string {
    return 'Agent imports use AGENT KIND (INDIVIDUAL or ORGANIZATION), names, contact columns, optional ROLE, AGENT TYPE, and BRANCH ID. Rows are scoped to your organisation automatically.';
  }

  get importCsvDisclaimer(): string {
    return 'CSV import only. Leave ORGANIZATION ID empty or omit it — agents are created under your signed-in organisation.';
  }

  get filteredAgents(): AgentRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    return this.agents.filter((agent) => {
      if (this.columnFilters.agentKind && agent.agentKind !== this.columnFilters.agentKind) {
        return false;
      }
      if (this.columnFilters.active === true && !agent.active) {
        return false;
      }
      if (this.columnFilters.active === false && agent.active) {
        return false;
      }
      const nameNeedle = this.columnFilters.fullName.trim().toLowerCase();
      if (nameNeedle && !agent.fullName.toLowerCase().includes(nameNeedle)) {
        return false;
      }
      const roleNeedle = this.columnFilters.role.trim().toLowerCase();
      if (roleNeedle && !(agent.role ?? '').toLowerCase().includes(roleNeedle)) {
        return false;
      }
      const regionNeedle = this.columnFilters.region.trim().toLowerCase();
      if (regionNeedle && !(agent.assignedRegion ?? '').toLowerCase().includes(regionNeedle)) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay = [
        agent.fullName,
        agent.email,
        agent.phoneNumber,
        agent.role,
        agent.assignedRegion,
        agent.agentKind,
      ]
        .join(' ')
        .toLowerCase();
      return hay.includes(q);
    });
  }

  ngOnInit(): void {
    this.title.setTitle('Agents | Organization management');
    this.reload();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  reload(): void {
    this.loading = true;
    this.error = '';
    this.orgMgmt
      .listAgents()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (agents) => {
          this.agents = agents;
        },
        error: (err: Error) => {
          this.error = err.message ?? 'Could not load agents.';
        },
      });
  }

  hasActiveFilters(): boolean {
    return (
      !!this.searchQuery.trim() ||
      !!this.columnFilters.fullName.trim() ||
      !!this.columnFilters.role.trim() ||
      !!this.columnFilters.region.trim() ||
      this.columnFilters.agentKind !== '' ||
      this.columnFilters.active !== ''
    );
  }

  importCsv(input: HTMLInputElement): void {
    input.click();
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.actionInProgress = true;
    this.orgMgmt
      .importAgentsCsv(file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.snackBar.open(response.message ?? (response.ok ? 'Import completed.' : 'Import failed.'), 'Close', {
            duration: response.ok ? 3500 : 6000,
          });
          input.value = '';
          this.actionInProgress = false;
          if (response.ok) {
            this.reload();
          }
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Import failed.', 'Close', { duration: 5000 });
          input.value = '';
          this.actionInProgress = false;
        },
      });
  }

  downloadSampleCsv(): void {
    downloadAgentSampleCsv();
    this.snackBar.open('Sample CSV downloaded.', 'Close', { duration: 3000 });
  }

  exportAs(format: LxExportFormat): void {
    this.exporting = true;
    const rows = this.filteredAgents;
    const saved = exportClientTableAsCsv(format, rows, AGENT_EXPORT_COLUMNS, 'agents', (message) =>
      this.snackBar.open(message, 'Close', { duration: 5000 }),
      { title: 'Organisation agents' },
    );
    this.exporting = false;
    if (saved) {
      this.snackBar.open(
        `Exported ${rows.length} agent${rows.length === 1 ? '' : 's'} as ${exportFormatLabel(format)}.`,
        'Close',
        { duration: 3000 },
      );
    }
  }

  openCreate(): void {
    this.openFormDialog({ action: 'create' });
  }

  openEdit(agent: AgentRow): void {
    this.openFormDialog({ action: 'edit', row: agent });
  }

  viewAgent(agent: AgentRow): void {
    this.openFormDialog({ action: 'view', row: agent });
  }

  deleteAgent(agent: AgentRow): void {
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        data: { entityLabel: `agent "${agent.fullName}"` },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.orgMgmt.deleteAgent(agent.id).subscribe({
          next: () => {
            this.snackBar.open('Agent removed.', 'Close', { duration: 3500 });
            this.reload();
          },
          error: (err: Error) => {
            this.snackBar.open(err.message ?? 'Could not remove agent.', 'Close', { duration: 5000 });
          },
        });
      });
  }

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
        this.reload();
      }
    });
  }
}
