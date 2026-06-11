import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Observable, Subject, debounceTime, of } from 'rxjs';
import { finalize, switchMap, takeUntil } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { BillingSettingsService } from '../../../settings/services/billing-settings.service';
import { CreateRequisitionDialogComponent } from '../../components/create-requisition-dialog/create-requisition-dialog.component';
import { ProcurementStageTimelineDialogComponent } from '../../components/procurement-stage-timeline-dialog/procurement-stage-timeline-dialog.component';
import type { ProcurementStageTimelineDialogData } from '../../components/procurement-stage-timeline-dialog/procurement-stage-timeline-dialog.component';
import { ReceiveGoodsDialogComponent } from '../../components/receive-goods-dialog/receive-goods-dialog.component';
import type { ReceiveGoodsDialogData } from '../../components/receive-goods-dialog/receive-goods-dialog.component';
import { ViewSupplierQuoteDialogComponent } from '../../components/view-supplier-quote-dialog/view-supplier-quote-dialog.component';
import type { ViewSupplierQuoteDialogData } from '../../components/view-supplier-quote-dialog/view-supplier-quote-dialog.component';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import {
  purchaseOrderStatusCssClass,
  requisitionStatusCssClass,
  salesOrderStatusCssClass,
} from '../../utils/inventory-status.util';
import {
  buildPurchaseOrderJourney,
  buildRequisitionJourney,
  buildSalesOrderJourney,
} from '../../utils/procurement-journey.util';
import {
  OrdersWorkspaceTab,
  PurchaseOrderRow,
  PurchaseOrderStatus,
  PurchaseRequisitionRow,
  SalesOrderRow,
  SupplierQuoteRow,
  WarehouseRow,
} from '../../models/inventory.model';

@Component({
  selector: 'app-orders-workspace',
  templateUrl: './orders-workspace.component.html',
  styleUrl: './orders-workspace.component.scss',
  standalone: false,
})
export class OrdersWorkspaceComponent implements OnInit, OnDestroy {
  fetching = true;
  loadError = '';
  activeTab: OrdersWorkspaceTab = 'requisitions';

  requisitions: PurchaseRequisitionRow[] = [];
  requisitionsLoading = false;
  requisitionsError = '';

  quotations: SupplierQuoteRow[] = [];
  quotationsLoading = false;
  quotationsError = '';

  purchaseOrders: PurchaseOrderRow[] = [];
  poLoading = false;
  poError = '';

  salesOrders: SalesOrderRow[] = [];
  salesOrdersLoading = false;
  salesOrdersError = '';

  deliverableOrders: PurchaseOrderRow[] = [];

  paymentPoId: number | null = null;
  paymentCaptureMode: 'SYSTEM_GENERATED' | 'EXTERNAL_UPLOAD' = 'SYSTEM_GENERATED';
  paymentAmount = '';
  paymentReference = '';
  paymentNotes = '';
  paymentProofFile: File | null = null;
  paymentSubmitting = false;

  private userId = 0;
  private warehousesLoaded = false;
  private customerWarehouses: WarehouseRow[] = [];

  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly title: Title,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly inventoryService: InventoryPortalService,
    private readonly billingService: BillingSettingsService,
    private readonly notifications: NotificationService,
    private readonly authState: AuthStateService,
    private readonly userProfile: UserProfileService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('My Orders | LX Platform');
    this.userProfile
      .fetchCurrentUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe((profile) => {
        this.userId = profile?.id ?? Number(this.authState.currentUser?.userId ?? 0);
        this.cdr.markForCheck();
      });
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const tab = params.get('tab') as OrdersWorkspaceTab | null;
      if (tab && this.isValidTab(tab)) {
        this.activeTab = tab;
        this.ensureTabDataLoaded(tab);
        this.cdr.markForCheck();
      }
    });
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const tab = params.get('tab') as OrdersWorkspaceTab | null;
      if (tab && this.isValidTab(tab)) {
        this.activeTab = tab;
        this.ensureTabDataLoaded(tab);
        this.cdr.markForCheck();
      }
    });
    this.reload$.pipe(debounceTime(120), takeUntil(this.destroy$)).subscribe(() => this.loadWorkspace());
    this.reload$.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get orgName(): string {
    return this.authState.currentUser?.orgName ?? 'Your organisation';
  }

  get pendingCount(): number {
    return this.requisitions.filter(
      (r) =>
        r.status === 'SUBMITTED' ||
        r.status === 'SUPPLIER_CONFIRMED' ||
        r.status === 'PUBLISHED_TO_SUPPLIER',
    ).length;
  }

  get activePoCount(): number {
    return this.purchaseOrders.filter(
      (o) => o.status === 'SUBMITTED' || o.status === 'APPROVED' || o.status === 'PARTIALLY_RECEIVED',
    ).length;
  }

  refresh(): void {
    this.reload$.next();
  }

  setTab(tab: OrdersWorkspaceTab): void {
    this.activeTab = tab;
    this.ensureTabDataLoaded(tab);
    void this.router.navigate(['/my-orders', tab]);
  }

  // ── Requisition actions ───────────────────────────────────────────────────

  openCreateRequisition(): void {
    this.dialog
      .open(CreateRequisitionDialogComponent, {
        width: '780px',
        maxWidth: '95vw',
        maxHeight: '95vh',
        disableClose: true,
        panelClass: 'lx-location-dialog-panel',
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((req: PurchaseRequisitionRow | undefined) => {
        if (req) {
          this.notifications.success(`Draft requisition ${req.requisitionNumber} created.`);
          this.loadRequisitions();
        }
      });
  }

  onSubmitRequisition(row: PurchaseRequisitionRow): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    this.inventoryService
      .submitRequisition(row.id, this.userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.notifications.success(`Requisition ${updated.requisitionNumber} submitted for approval.`);
          this.loadRequisitions();
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not submit requisition.');
        },
      });
  }

  // ── Procurement workflow actions (customer) ───────────────────────────────

  /** Advance an internal approval stage on a SUBMITTED requisition. */
  onApproveRequisitionStage(row: PurchaseRequisitionRow): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    this.inventoryService
      .approveRequisition({ id: row.id, approvedByUserId: this.userId })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.notifications.success(`Requisition ${updated.requisitionNumber} stage approved.`);
          this.loadRequisitions();
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not approve requisition stage.');
        },
      });
  }

  openViewQuote(requisitionId: number, requisitionNumber: string): void {
    const data: ViewSupplierQuoteDialogData = { requisitionId, requisitionNumber };
    this.dialog.open(ViewSupplierQuoteDialogComponent, {
      width: '760px',
      maxWidth: '95vw',
      maxHeight: '92vh',
      data,
      panelClass: 'lx-location-dialog-panel',
    });
  }

  openRequisitionJourney(row: PurchaseRequisitionRow): void {
    const data: ProcurementStageTimelineDialogData = {
      title: `Requisition ${row.requisitionNumber}`,
      subtitle: row.stageProgressLabel,
      steps: buildRequisitionJourney(row),
    };
    this.dialog.open(ProcurementStageTimelineDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data,
      panelClass: 'lx-location-dialog-panel',
    });
  }

  openPurchaseOrderJourney(order: PurchaseOrderRow): void {
    const data: ProcurementStageTimelineDialogData = {
      title: `Purchase order ${order.orderNumber}`,
      subtitle: order.stageProgressLabel,
      steps: buildPurchaseOrderJourney(order),
    };
    this.dialog.open(ProcurementStageTimelineDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data,
      panelClass: 'lx-location-dialog-panel',
    });
  }

  openSalesOrderJourney(order: SalesOrderRow): void {
    const data: ProcurementStageTimelineDialogData = {
      title: `Sales order ${order.salesOrderNumber}`,
      subtitle: order.stageProgressLabel,
      steps: buildSalesOrderJourney(order),
    };
    this.dialog.open(ProcurementStageTimelineDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data,
      panelClass: 'lx-location-dialog-panel',
    });
  }

  salesOrderStatusClass(status: SalesOrderRow['status']): string {
    return salesOrderStatusCssClass(status);
  }

  hasQuote(row: PurchaseRequisitionRow): boolean {
    return !!row.supplierQuoteId || row.status === 'SUPPLIER_CONFIRMED' || row.status === 'CUSTOMER_ACKNOWLEDGED';
  }

  /** Customer acknowledges supplier quote — moves status from SUPPLIER_CONFIRMED → CUSTOMER_ACKNOWLEDGED. */
  onAcknowledgeQuote(row: PurchaseRequisitionRow): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    if (!window.confirm(`Acknowledge the supplier quote for ${row.requisitionNumber} and proceed?`)) {
      return;
    }
    this.inventoryService
      .acknowledgeQuote({ purchaseRequisitionId: row.id, acknowledgedByUserId: this.userId })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.notifications.success(`Quote for ${updated.requisitionNumber} acknowledged.`);
          this.loadRequisitions();
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not acknowledge quote.');
        },
      });
  }

  /** Customer approves a PO customer-side stage. */
  onApprovePoCustomerStage(order: PurchaseOrderRow): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    this.inventoryService
      .approvePoCustomerStage({ purchaseOrderId: order.id, approvedByUserId: this.userId })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.notifications.success(`PO ${updated.orderNumber} customer stage approved.`);
          this.loadPurchaseOrders();
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not approve PO customer stage.');
        },
      });
  }

  // ── Payment upload ────────────────────────────────────────────────────────

  openPaymentPanel(order: PurchaseOrderRow): void {
    this.paymentPoId = order.id;
    this.paymentCaptureMode = 'SYSTEM_GENERATED';
    this.paymentAmount = String(order.totalAmount > 0 ? order.totalAmount : '');
    this.paymentReference = '';
    this.paymentNotes = '';
    this.paymentProofFile = null;
    this.cdr.markForCheck();
  }

  setPaymentCaptureMode(mode: 'SYSTEM_GENERATED' | 'EXTERNAL_UPLOAD'): void {
    this.paymentCaptureMode = mode;
    this.cdr.markForCheck();
  }

  closePaymentPanel(): void {
    this.paymentPoId = null;
    this.cdr.markForCheck();
  }

  onPaymentProofSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    this.paymentProofFile = file;
    this.cdr.markForCheck();
  }

  onSubmitPayment(order: PurchaseOrderRow): void {
    const amount = Number(String(this.paymentAmount).trim());
    const referenceNumber = this.paymentReference.trim();
    if (!Number.isFinite(amount) || amount <= 0) {
      this.notifications.error('Enter a valid payment amount.');
      return;
    }
    if (!referenceNumber) {
      this.notifications.error('Payment reference number is required.');
      return;
    }
    if (this.paymentCaptureMode === 'EXTERNAL_UPLOAD' && !this.paymentProofFile) {
      this.notifications.error('Upload proof of payment for the external upload option.');
      return;
    }
    if (this.paymentSubmitting) {
      return;
    }
    const organizationId = Number(this.authState.currentUser?.organizationId ?? 0);
    this.paymentSubmitting = true;
    const upload$: Observable<number | undefined> =
      this.paymentCaptureMode === 'EXTERNAL_UPLOAD' && this.paymentProofFile && organizationId > 0
        ? this.billingService.uploadPaymentProof(organizationId, this.paymentProofFile)
        : of<number | undefined>(undefined);

    upload$
      .pipe(
        switchMap((proofDocumentId: number | undefined) =>
          this.billingService.submitPayment({
            purchaseOrderId: order.id,
            amount,
            currency: 'USD',
            paymentMethod: 'BANK_TRANSFER',
            referenceNumber,
            notes: this.paymentNotes.trim() || undefined,
            proofSource: this.paymentCaptureMode,
            proofDocumentId,
          }),
        ),
        finalize(() => {
          this.paymentSubmitting = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (result: { id: number; status: string; message: string }) => {
          this.notifications.success(result.message || `Payment submitted for PO ${order.orderNumber}.`);
          this.closePaymentPanel();
          this.loadPurchaseOrders();
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not submit payment.');
        },
      });
  }

  // ── Delivery (GRV) action ─────────────────────────────────────────────────

  onReceiveGoods(order: PurchaseOrderRow): void {
    if (!this.userId) {
      this.notifications.error('Your user profile could not be loaded.');
      return;
    }
    const openDialog = (warehouses: WarehouseRow[]) => {
      this.dialog
        .open(ReceiveGoodsDialogComponent, {
          width: '720px',
          maxWidth: '95vw',
          maxHeight: '95vh',
          disableClose: true,
          panelClass: 'lx-location-dialog-panel',
          data: {
            order,
            warehouses,
            receivedByUserId: this.userId,
          } satisfies ReceiveGoodsDialogData,
        })
        .afterClosed()
        .pipe(takeUntil(this.destroy$))
        .subscribe((message: string | undefined) => {
          if (message) {
            this.notifications.success(message);
            this.loadPurchaseOrders();
          }
        });
    };

    if (this.warehousesLoaded) {
      openDialog(this.customerWarehouses);
      return;
    }

    this.inventoryService
      .listWarehouses()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rows) => {
          this.customerWarehouses = rows;
          this.warehousesLoaded = true;
          openDialog(this.customerWarehouses);
        },
        error: (err: Error) =>
          this.notifications.error(err.message ?? 'Could not load warehouses for goods receipt.'),
      });
  }

  // ── CSS helpers ───────────────────────────────────────────────────────────

  readonly requisitionStatusClass = requisitionStatusCssClass;
  readonly purchaseOrderStatusClass = purchaseOrderStatusCssClass;

  canReceive(status: PurchaseOrderStatus): boolean {
    return status === 'SUBMITTED' || status === 'APPROVED' || status === 'PARTIALLY_RECEIVED';
  }

  trackByRequisitionId(_i: number, row: PurchaseRequisitionRow): number {
    return row.id;
  }

  trackByPoId(_i: number, row: PurchaseOrderRow): number {
    return row.id;
  }

  trackByQuoteId(_i: number, row: SupplierQuoteRow): number {
    return row.id;
  }

  trackBySalesOrderId(_i: number, row: SalesOrderRow): number {
    return row.id;
  }

  // ── Private loaders ───────────────────────────────────────────────────────

  private loadWorkspace(): void {
    this.fetching = true;
    this.loadError = '';
    this.loadRequisitions(false);
    this.inventoryService
      .listPurchaseOrders()
      .pipe(
        finalize(() => {
          this.fetching = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.purchaseOrders = rows;
          this.deliverableOrders = rows.filter((o) => this.canReceive(o.status));
        },
        error: (err: Error) => {
          this.poError = err.message ?? 'Could not load purchase orders.';
          this.loadError = this.poError;
        },
      });
  }

  private loadRequisitions(showLoading = true): void {
    if (showLoading) {
      this.requisitionsLoading = true;
      this.requisitionsError = '';
    }
    this.inventoryService
      .listRequisitions()
      .pipe(
        finalize(() => {
          this.requisitionsLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.requisitions = rows;
        },
        error: (err: Error) => {
          this.requisitionsError = err.message ?? 'Could not load requisitions.';
        },
      });
  }

  private loadPurchaseOrders(): void {
    this.poLoading = true;
    this.poError = '';
    this.inventoryService
      .listPurchaseOrders()
      .pipe(
        finalize(() => {
          this.poLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.purchaseOrders = rows;
          this.deliverableOrders = rows.filter((o) => this.canReceive(o.status));
        },
        error: (err: Error) => (this.poError = err.message ?? 'Could not load purchase orders.'),
      });
  }

  private loadSalesOrders(showLoading = true): void {
    const organizationId = Number(this.authState.currentUser?.organizationId ?? 0);
    if (organizationId <= 0) {
      this.salesOrdersError = 'Organisation context is missing.';
      return;
    }
    if (showLoading) {
      this.salesOrdersLoading = true;
      this.salesOrdersError = '';
    }
    this.inventoryService
      .listCustomerSalesOrders(organizationId)
      .pipe(
        finalize(() => {
          this.salesOrdersLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.salesOrders = rows;
        },
        error: (err: Error) => {
          this.salesOrdersError = err.message ?? 'Could not load sales orders.';
        },
      });
  }

  private loadQuotations(showLoading = true): void {
    const organizationId = Number(this.authState.currentUser?.organizationId ?? 0);
    if (organizationId <= 0) {
      this.quotationsError = 'Organisation context is missing.';
      return;
    }
    if (showLoading) {
      this.quotationsLoading = true;
      this.quotationsError = '';
    }
    this.inventoryService
      .listCustomerQuotes(organizationId)
      .pipe(
        finalize(() => {
          this.quotationsLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.quotations = rows;
        },
        error: (err: Error) => {
          this.quotationsError = err.message ?? 'Could not load quotations.';
        },
      });
  }

  private ensureTabDataLoaded(tab: OrdersWorkspaceTab): void {
    if (tab === 'purchase-orders' && !this.purchaseOrders.length && !this.poLoading) {
      this.loadPurchaseOrders();
    }
    if (tab === 'deliveries' && !this.deliverableOrders.length && !this.poLoading) {
      this.loadPurchaseOrders();
    }
    if (tab === 'requisitions' && !this.requisitions.length && !this.requisitionsLoading) {
      this.loadRequisitions();
    }
    if (tab === 'quotations' && !this.quotations.length && !this.quotationsLoading) {
      this.loadQuotations();
    }
    if (tab === 'sales-orders' && !this.salesOrders.length && !this.salesOrdersLoading) {
      this.loadSalesOrders();
    }
  }

  private isValidTab(tab: string): tab is OrdersWorkspaceTab {
    return ['requisitions', 'quotations', 'purchase-orders', 'sales-orders', 'deliveries'].includes(tab);
  }

}
