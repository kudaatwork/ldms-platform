import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { Subject, debounceTime, finalize, takeUntil } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { RegisterTransporterDialogComponent } from '../../../fleet/components/register-transporter-dialog/register-transporter-dialog.component';
import { TransporterPartnerRow } from '../../../fleet/models/fleet.model';
import { FleetPortalService } from '../../../fleet/services/fleet-portal.service';
import { OrgManagementPortalService } from '../../services/org-management-portal.service';

@Component({
  selector: 'app-org-transporters-page',
  templateUrl: './org-transporters-page.component.html',
  styleUrl: './org-transporters-page.component.scss',
  standalone: false,
})
export class OrgTransportersPageComponent implements OnInit, OnDestroy {
  loading = true;
  error = '';
  search = '';
  transporters: TransporterPartnerRow[] = [];

  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly orgMgmt: OrgManagementPortalService,
    private readonly fleet: FleetPortalService,
    private readonly dialog: MatDialog,
    private readonly notifications: NotificationService,
    private readonly authState: AuthStateService,
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Transporters | Organization management');
    this.reload$.pipe(debounceTime(120), takeUntil(this.destroy$)).subscribe(() => this.loadTransporters());
    this.reload$.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get canRegisterTransporter(): boolean {
    const c = this.authState.currentUser?.orgClassification;
    return !c || c === 'SUPPLIER';
  }

  get filteredTransporters(): TransporterPartnerRow[] {
    const q = this.search.trim().toLowerCase();
    if (!q) {
      return this.transporters;
    }
    return this.transporters.filter((row) =>
      `${row.name} ${row.email} ${row.phoneNumber}`.toLowerCase().includes(q),
    );
  }

  refresh(): void {
    this.reload$.next();
  }

  openRegisterTransporter(partnerId?: number): void {
    this.dialog
      .open(RegisterTransporterDialogComponent, {
        width: '720px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        data: partnerId ? { partnerId } : {},
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((saved) => {
        if (saved) {
          this.notifications.success(partnerId ? 'Transporter updated.' : 'Transporter registered.');
          this.reload$.next();
        }
      });
  }

  confirmRemove(partner: TransporterPartnerRow): void {
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: { entityLabel: `transporter "${partner.name}"` },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.fleet.deleteTransporter(partner.id).subscribe({
          next: (message) => {
            this.notifications.success(message);
            this.reload$.next();
          },
          error: (err: Error) => {
            this.notifications.error(err.message ?? 'Could not remove transporter.');
          },
        });
      });
  }

  private loadTransporters(): void {
    this.loading = true;
    this.error = '';
    this.orgMgmt
      .listTransporters()
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.transporters = rows;
        },
        error: (err: Error) => {
          this.error = err.message ?? 'Could not load transporters.';
        },
      });
  }
}
