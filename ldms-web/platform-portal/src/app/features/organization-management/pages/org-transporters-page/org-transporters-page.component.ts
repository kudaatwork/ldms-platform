import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { Subject, debounceTime, finalize, takeUntil } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  exportClientTableAsCsv,
  exportFormatLabel,
  LxExportFormat,
} from '../../../../shared/utils/lx-export.util';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { RegisterTransporterDialogComponent } from '../../../fleet/components/register-transporter-dialog/register-transporter-dialog.component';
import { LinkTransporterDialogComponent } from '../../../fleet/components/link-transporter-dialog/link-transporter-dialog.component';
import { TransporterContractStatus, TransporterPartnerRow } from '../../../fleet/models/fleet.model';
import { FleetPortalService } from '../../../fleet/services/fleet-portal.service';
import {
  TRANSPORTER_EXPORT_COLUMNS,
  downloadFleetSampleCsv,
} from '../../../fleet/utils/fleet-export.util';
import { OrgManagementPortalService } from '../../services/org-management-portal.service';

@Component({
  selector: 'app-org-transporters-page',
  templateUrl: './org-transporters-page.component.html',
  styleUrl: './org-transporters-page.component.scss',
  standalone: false,
})
export class OrgTransportersPageComponent implements OnInit, OnDestroy {
  loading = true;
  exporting = false;
  error = '';
  searchQuery = '';
  filterFieldsOpen = false;
  showSampleCsvInfo = false;
  contractFilter: '' | TransporterContractStatus = '';
  verifiedFilter: '' | 'yes' | 'no' = '';
  transporters: TransporterPartnerRow[] = [];

  readonly contractOptions: { id: '' | TransporterContractStatus; label: string }[] = [
    { id: '', label: 'Any contract' },
    { id: 'active', label: 'Active' },
    { id: 'open_ended', label: 'Open-ended' },
    { id: 'upcoming', label: 'Starts soon' },
    { id: 'expired', label: 'Ended' },
  ];

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

  get sampleCsvDescription(): string {
    return 'Sample shows organisation, contract dates, and primary contact columns. Full registration (KYC, identity scans) uses Register transporter.';
  }

  get importCsvDisclaimer(): string {
    return 'Bulk transporter registration requires identity documents — use Register transporter per organisation. Export and sample CSV support planning and reporting.';
  }

  get hasActiveFilters(): boolean {
    return this.contractFilter !== '' || this.verifiedFilter !== '' || !!this.searchQuery.trim();
  }

  get filteredTransporters(): TransporterPartnerRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    return this.transporters.filter((row) => {
      if (this.contractFilter && row.contractStatus !== this.contractFilter) {
        return false;
      }
      if (this.verifiedFilter === 'yes' && !row.verified) {
        return false;
      }
      if (this.verifiedFilter === 'no' && row.verified) {
        return false;
      }
      if (!q) {
        return true;
      }
      return `${row.name} ${row.email} ${row.phoneNumber} ${row.kycStatusLabel} ${row.contractStatusLabel}`
        .toLowerCase()
        .includes(q);
    });
  }

  refresh(): void {
    this.reload$.next();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.contractFilter = '';
    this.verifiedFilter = '';
  }

  importCsv(input: HTMLInputElement): void {
    input.click();
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = '';
    this.notifications.show(
      'Transporter bulk import is not available yet — identity documents and KYC are required. Use Register transporter for each organisation.',
    );
  }

  downloadSampleCsv(): void {
    downloadFleetSampleCsv('transporters');
    this.notifications.success('Transporter sample CSV downloaded.');
  }

  exportAs(format: LxExportFormat): void {
    this.exporting = true;
    const rows = this.filteredTransporters;
    const saved = exportClientTableAsCsv(format, rows, TRANSPORTER_EXPORT_COLUMNS, 'transporters', (message) =>
      this.notifications.show(message),
      { title: 'Contracted transporters' },
    );
    this.exporting = false;
    if (saved) {
      this.notifications.success(
        `Exported ${rows.length} transporter${rows.length === 1 ? '' : 's'} as ${exportFormatLabel(format)}.`,
      );
    }
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

  openLinkTransporter(): void {
    this.dialog
      .open(LinkTransporterDialogComponent, {
        width: '640px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((message?: string) => {
        if (message) {
          this.notifications.success(message);
          this.reload$.next();
        }
      });
  }

  cancelOffer(partner: TransporterPartnerRow): void {
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: {
          entityLabel: `the pending offer to "${partner.name}"`,
          confirmLabel: 'Cancel offer',
          title: 'Cancel transporter offer',
        },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.fleet.cancelTransporterOffer(partner.id).subscribe({
          next: (message) => {
            this.notifications.success(message);
            this.reload$.next();
          },
          error: (err: Error) => {
            this.notifications.error(err.message ?? 'Could not cancel the offer.');
          },
        });
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
