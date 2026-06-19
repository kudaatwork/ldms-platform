import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Title } from '@angular/platform-browser';
import { Subject, catchError, finalize, of, takeUntil } from 'rxjs';
import { OrganizationService, TradingPartner } from '../../../../core/services/organization.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import {
  TradingPartnerDialogComponent,
  TradingPartnerDialogData,
} from '../../components/trading-partner-dialog/trading-partner-dialog.component';

@Component({
  selector: 'app-trading-partners-workspace',
  templateUrl: './trading-partners-workspace.component.html',
  styleUrl: './trading-partners-workspace.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class TradingPartnersWorkspaceComponent implements OnInit, OnDestroy {
  partners: TradingPartner[] = [];
  filteredPartners: TradingPartner[] = [];
  loading = false;
  error = '';
  searchTerm = '';
  activeRole: TradingPartner['role'] | 'ALL' = 'ALL';

  readonly roles: Array<{ value: TradingPartner['role'] | 'ALL'; label: string; icon: string }> = [
    { value: 'ALL', label: 'All partners', icon: 'groups' },
    { value: 'CUSTOMER', label: 'Customers', icon: 'person' },
    { value: 'SUPPLIER', label: 'Suppliers', icon: 'storefront' },
    { value: 'TRANSPORTER', label: 'Transporters', icon: 'local_shipping' },
    { value: 'OTHER', label: 'Other', icon: 'more_horiz' },
  ];

  get standaloneMode(): boolean {
    return this.authState.currentUser?.standaloneMode ?? false;
  }

  get counterpartyEngagementMode(): 'RECORD_ONLY' | 'PLATFORM_ORG' {
    return this.authState.currentUser?.counterpartyEngagementMode ?? 'PLATFORM_ORG';
  }

  get counterpartyLabel(): string {
    return this.authState.currentUser?.orgClassification === 'CUSTOMER' ? 'suppliers' : 'customers';
  }

  get showRecordOnlyHint(): boolean {
    return this.standaloneMode || this.counterpartyEngagementMode === 'RECORD_ONLY';
  }

  get customerCount(): number {
    return this.partners.filter((p) => p.role === 'CUSTOMER').length;
  }

  get supplierCount(): number {
    return this.partners.filter((p) => p.role === 'SUPPLIER').length;
  }

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly orgService: OrganizationService,
    private readonly authState: AuthStateService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
  ) {
    this.title.setTitle('Trading Partners | LX Platform');
  }

  ngOnInit(): void {
    this.loadPartners();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadPartners(): void {
    this.loading = true;
    this.error = '';
    this.orgService
      .listTradingPartners()
      .pipe(
        catchError((err: Error) => {
          this.error = err.message ?? 'Could not load trading partners.';
          return of([]);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((partners) => {
        this.partners = partners;
        this.applyFilter();
        this.cdr.markForCheck();
      });
  }

  onSearch(term: string): void {
    this.searchTerm = term;
    this.applyFilter();
    this.cdr.markForCheck();
  }

  setRoleFilter(role: TradingPartner['role'] | 'ALL'): void {
    this.activeRole = role;
    this.applyFilter();
    this.cdr.markForCheck();
  }

  openAddDialog(): void {
    const ref = this.dialog.open<TradingPartnerDialogComponent, TradingPartnerDialogData, TradingPartner>(
      TradingPartnerDialogComponent,
      { data: {}, width: '520px', panelClass: 'lx-dialog-panel' },
    );
    ref
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((result) => {
        if (result) {
          this.partners = [...this.partners, result];
          this.applyFilter();
          this.snackBar.open('Trading partner added.', 'Close', { duration: 3000 });
          this.cdr.markForCheck();
        }
      });
  }

  openEditDialog(partner: TradingPartner): void {
    const ref = this.dialog.open<TradingPartnerDialogComponent, TradingPartnerDialogData, TradingPartner>(
      TradingPartnerDialogComponent,
      { data: { partner }, width: '520px', panelClass: 'lx-dialog-panel' },
    );
    ref
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((result) => {
        if (result) {
          this.partners = this.partners.map((p) => (p.id === result.id ? result : p));
          this.applyFilter();
          this.snackBar.open('Trading partner updated.', 'Close', { duration: 3000 });
          this.cdr.markForCheck();
        }
      });
  }

  deletePartner(partner: TradingPartner): void {
    this.orgService
      .deleteTradingPartner(partner.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.partners = this.partners.filter((p) => p.id !== partner.id);
          this.applyFilter();
          this.snackBar.open('Trading partner removed.', 'Close', { duration: 3000 });
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not remove partner.', 'Close', { duration: 4000 });
        },
      });
  }

  roleLabel(role: TradingPartner['role']): string {
    return this.roles.find((r) => r.value === role)?.label ?? role;
  }

  roleIcon(role: TradingPartner['role']): string {
    return this.roles.find((r) => r.value === role)?.icon ?? 'groups';
  }

  private applyFilter(): void {
    let result = [...this.partners];
    if (this.activeRole !== 'ALL') {
      result = result.filter((p) => p.role === this.activeRole);
    }
    const term = this.searchTerm.trim().toLowerCase();
    if (term) {
      result = result.filter(
        (p) =>
          p.name.toLowerCase().includes(term) ||
          (p.email ?? '').toLowerCase().includes(term) ||
          (p.phoneNumber ?? '').includes(term),
      );
    }
    this.filteredPartners = result;
  }
}
