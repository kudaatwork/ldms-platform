import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { Subject, debounceTime } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { RegisterCustomerDialogComponent } from '../../components/register-customer-dialog/register-customer-dialog.component';
import type { OrganizationPartnerMetadata } from '../../../../shared/models/organization-metadata.model';
import {
  CustomerListRow,
  CustomerPageMetrics,
  CustomerStatusFilter,
  CustomerViewMode,
} from '../../models/customer.model';
import { CustomersPortalService } from '../../services/customers-portal.service';

@Component({
  selector: 'app-customers-list',
  templateUrl: './customers-list.component.html',
  styleUrl: './customers-list.component.scss',
  standalone: false,
})
export class CustomersListComponent implements OnInit, OnDestroy {
  readonly pageLead =
    'onboard organisations, track KYC readiness, and keep every relationship visible before the first order ships.';

  fetching = true;
  loadError = '';
  retryingOnboardingId: number | null = null;
  searchQuery = '';
  statusFilter: CustomerStatusFilter = 'ALL';
  viewMode: CustomerViewMode = 'atlas';
  selectedId: number | null = null;
  spotlightMetadata: OrganizationPartnerMetadata | null = null;
  spotlightLoading = false;
  spotlightError = '';

  allRows: CustomerListRow[] = [];
  private spotlightRequestId = 0;
  readonly ledgerTable = new MatTableDataSource<CustomerListRow>([]);
  readonly ledgerColumns = ['customer', 'contact', 'kyc', 'verified', 'joined', 'actions'];

  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly title: Title,
    private readonly customers: CustomersPortalService,
    private readonly dialog: MatDialog,
    private readonly notifications: NotificationService,
    private readonly authState: AuthStateService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Customers | LX Platform');
    this.reload$.pipe(debounceTime(120), takeUntil(this.destroy$)).subscribe(() => this.loadCustomers());
    this.reload$.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get orgName(): string {
    return this.authState.currentUser?.orgName ?? 'Your organisation';
  }

  get heroLead(): string {
    return `Your buyer network for ${this.orgName} — ${this.pageLead}`;
  }

  get isSupplier(): boolean {
    const c = this.authState.currentUser?.orgClassification;
    return !c || c === 'SUPPLIER';
  }

  get metrics(): CustomerPageMetrics {
    const rows = this.allRows;
    return {
      total: rows.length,
      verified: rows.filter((r) => r.verified).length,
      pendingKyc: rows.filter((r) => !r.verified && r.kycStatus !== 'DRAFT').length,
      draft: rows.filter((r) => r.kycStatus === 'DRAFT').length,
    };
  }

  get filteredRows(): CustomerListRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    return this.allRows.filter((row) => {
      if (this.statusFilter === 'VERIFIED' && !row.verified) {
        return false;
      }
      if (this.statusFilter === 'PENDING_KYC' && (row.verified || row.kycStatus === 'DRAFT')) {
        return false;
      }
      if (this.statusFilter === 'DRAFT' && row.kycStatus !== 'DRAFT') {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay = `${row.name} ${row.email} ${row.phoneNumber}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get selectedRow(): CustomerListRow | null {
    if (this.selectedId == null) {
      return null;
    }
    return this.filteredRows.find((r) => r.id === this.selectedId) ?? this.allRows.find((r) => r.id === this.selectedId) ?? null;
  }

  refresh(): void {
    this.reload$.next();
  }

  setViewMode(mode: CustomerViewMode): void {
    this.viewMode = mode;
    this.syncLedgerTable();
  }

  setStatusFilter(filter: CustomerStatusFilter): void {
    this.statusFilter = filter;
    this.ensureSelectionValid();
    this.syncLedgerTable();
  }

  onSearchChange(): void {
    this.ensureSelectionValid();
    this.syncLedgerTable();
  }

  selectRow(row: CustomerListRow): void {
    const nextId = row.id === this.selectedId ? null : row.id;
    this.selectedId = nextId;
    this.loadSpotlightMetadata(nextId);
  }

  openSpotlight(row: CustomerListRow, event: Event): void {
    event.stopPropagation();
    this.selectedId = row.id;
    this.loadSpotlightMetadata(row.id);
  }

  closeSpotlight(): void {
    this.selectedId = null;
    this.clearSpotlightMetadata();
  }

  onCardKeydown(row: CustomerListRow, event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.selectRow(row);
    }
  }

  openRegisterDialog(): void {
    this.openCustomerDialog();
  }

  openEditDialog(row: CustomerListRow, event: Event): void {
    event.stopPropagation();
    this.openCustomerDialog(row.id);
  }

  confirmDeleteCustomer(row: CustomerListRow, event: Event): void {
    event.stopPropagation();
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: {
          entityLabel: `customer "${row.name}"`,
        },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (!confirmed) {
          return;
        }
        this.customers
          .deleteCustomer(row.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (message) => {
              if (this.selectedId === row.id) {
                this.selectedId = null;
              }
              this.notifications.success(message);
              this.reload$.next();
            },
            error: (err: Error) => {
              this.notifications.error(err.message ?? 'Could not delete customer.');
            },
          });
      });
  }

  canRetryOnboarding(row: CustomerListRow): boolean {
    return String(row.kycStatus ?? '').toUpperCase() === 'APPROVED';
  }

  retryOnboardingEmails(row: CustomerListRow, event: Event): void {
    event.stopPropagation();
    if (!this.canRetryOnboarding(row) || this.retryingOnboardingId === row.id) {
      return;
    }
    this.retryingOnboardingId = row.id;
    this.customers
      .retryOnboardingEmails(row.id)
      .pipe(
        finalize(() => {
          this.retryingOnboardingId = null;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (message) => this.notifications.success(message),
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not resend onboarding emails.');
        },
      });
  }

  private openCustomerDialog(customerId?: number): void {
    this.dialog
      .open(RegisterCustomerDialogComponent, {
        width: '720px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
        autoFocus: 'first-tabbable',
        data: customerId ? { customerId } : undefined,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((saved) => {
        if (saved) {
          this.notifications.success(
            customerId ? `${saved.name} was updated.` : `${saved.name} is now linked to your network.`,
          );
          this.reload$.next();
        }
      });
  }

  copyEmail(row: CustomerListRow, event: Event): void {
    event.stopPropagation();
    const email = row.email?.trim();
    if (!email) {
      this.notifications.error('No email on this record.');
      return;
    }
    void navigator.clipboard?.writeText(email).then(
      () => this.notifications.success('Email copied.'),
      () => this.notifications.error('Could not copy email.'),
    );
  }

  copyPhone(row: CustomerListRow, event: Event): void {
    event.stopPropagation();
    const phone = row.phoneNumber?.trim();
    if (!phone) {
      this.notifications.error('No phone number on this record.');
      return;
    }
    void navigator.clipboard?.writeText(phone).then(
      () => this.notifications.success('Phone number copied.'),
      () => this.notifications.error('Could not copy phone number.'),
    );
  }

  trackByCustomerId(_index: number, row: CustomerListRow): number {
    return row.id;
  }

  avatarStyle(row: CustomerListRow): Record<string, string> {
    const hue = row.accentHue;
    return {
      '--cust-hue': String(hue),
      background: `linear-gradient(135deg, hsl(${hue} 72% 42%), hsl(${(hue + 48) % 360} 68% 52%))`,
    };
  }

  private loadCustomers(): void {
    this.fetching = true;
    this.loadError = '';
    this.customers
      .listCustomers()
      .pipe(
        finalize(() => {
          this.fetching = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.allRows = rows.sort((a, b) => a.name.localeCompare(b.name));
          this.ensureSelectionValid();
          this.syncLedgerTable();
        },
        error: (err: Error) => {
          this.allRows = [];
          this.ledgerTable.data = [];
          this.loadError = err.message ?? 'Could not load customers.';
        },
      });
  }

  private syncLedgerTable(): void {
    this.ledgerTable.data = this.filteredRows.slice();
  }

  private ensureSelectionValid(): void {
    if (this.selectedId == null) {
      this.clearSpotlightMetadata();
      return;
    }
    if (!this.filteredRows.some((r) => r.id === this.selectedId)) {
      const nextId = this.filteredRows[0]?.id ?? null;
      this.selectedId = nextId;
      this.loadSpotlightMetadata(nextId);
    }
  }

  private loadSpotlightMetadata(customerId: number | null): void {
    if (customerId == null) {
      this.clearSpotlightMetadata();
      return;
    }
    const requestId = ++this.spotlightRequestId;
    this.spotlightLoading = true;
    this.spotlightError = '';
    this.spotlightMetadata = null;
    this.customers
      .getCustomerMetadata(customerId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (metadata) => {
          if (requestId !== this.spotlightRequestId || this.selectedId !== customerId) {
            return;
          }
          this.spotlightMetadata = metadata;
          this.spotlightLoading = false;
          this.cdr.detectChanges();
        },
        error: (err: Error) => {
          if (requestId !== this.spotlightRequestId || this.selectedId !== customerId) {
            return;
          }
          this.spotlightMetadata = null;
          this.spotlightLoading = false;
          this.spotlightError = err.message ?? 'Could not load customer metadata.';
          this.cdr.detectChanges();
        },
      });
  }

  private clearSpotlightMetadata(): void {
    this.spotlightRequestId += 1;
    this.spotlightMetadata = null;
    this.spotlightLoading = false;
    this.spotlightError = '';
  }
}
