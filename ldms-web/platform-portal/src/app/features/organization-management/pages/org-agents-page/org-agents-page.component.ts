import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, finalize, takeUntil } from 'rxjs';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import {
  AgentFormDialogComponent,
  AgentFormDialogData,
  AgentFormDialogResult,
} from '../../components/agent-form-dialog/agent-form-dialog.component';
import { AgentRow } from '../../models/org-management.model';
import { OrgManagementPortalService } from '../../services/org-management-portal.service';

@Component({
  selector: 'app-org-agents-page',
  templateUrl: './org-agents-page.component.html',
  styleUrl: './org-agents-page.component.scss',
  standalone: false,
})
export class OrgAgentsPageComponent implements OnInit, OnDestroy {
  loading = true;
  error = '';
  agents: AgentRow[] = [];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly orgMgmt: OrgManagementPortalService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {}

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
