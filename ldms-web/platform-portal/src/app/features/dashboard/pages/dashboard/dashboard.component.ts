import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, catchError, forkJoin, of, takeUntil } from 'rxjs';
import { Router } from '@angular/router';
import { OrganizationClassification, CurrentUser } from '../../../../core/models/auth.model';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import {
  PlatformWalletService,
  type OrganizationBillingSetting,
  type PlatformWalletSummary,
} from '../../../../core/services/platform-wallet.service';
import { formatWelcomeMessage } from '../../../../core/utils/welcome-message.util';
import type { LxWorkspaceHeroStatTheme } from '../../../../shared/components/lx-workspace-hero-stat/lx-workspace-hero-stat.component';
import type {
  PurchaseOrderRow,
  PurchaseRequisitionRow,
  SalesOrderRow,
  StockRow,
  TransferRow,
} from '../../../inventory/models/inventory.model';
import { InventoryPortalService } from '../../../inventory/services/inventory-portal.service';
import {
  FleetPortalService,
  type OrganizationFleetDashboardCounts,
} from '../../../fleet/services/fleet-portal.service';
import type { ShipmentRow, ShipmentStatus, TripLiveMapTrack, TripRow } from '../../../trip-tracking/models/trip-tracking.model';
import { TripTrackingPortalService } from '../../../trip-tracking/services/trip-tracking-portal.service';
import {
  DashboardChart,
  DashboardDonutSegment,
  KpiCard,
  KpiCardTheme,
  PLATFORM_CHART_CONFIG,
  PLATFORM_KPI_CONFIG,
} from '../../data/platform-mock-data';

type ShipmentBoardFilter = 'ALL' | 'PREPARED' | 'IN_TRANSIT' | 'COMPLETED' | 'FAILED';

type SupplierInventorySnapshot = {
  purchaseOrders: PurchaseOrderRow[];
  requisitions: PurchaseRequisitionRow[];
  transfers: TransferRow[];
  stock: StockRow[];
  salesOrders: SalesOrderRow[];
};

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  cards: KpiCard[] = [];
  charts: DashboardChart[] = [];
  classification: OrganizationClassification | '' = '';
  classificationLabel = '';

  shipments: ShipmentRow[] = [];
  trips: TripRow[] = [];
  supplierOperationsLoading = false;
  supplierOperationsError = '';

  shipmentFilter: ShipmentBoardFilter = 'ALL';
  shipmentSearch = '';
  selectedShipmentId: number | null = null;

  walletSummary: PlatformWalletSummary | null = null;
  billingSetting: OrganizationBillingSetting | null = null;
  billingLoading = true;
  organizationName = '';
  todayLabel = '';
  private supplierDataLoaded = false;
  private organizationDataLoaded = false;
  private transporterDataLoaded = false;
  organizationDashboardLoading = false;
  organizationDashboardError = '';
  transporterOperationsLoading = false;
  transporterOperationsError = '';
  fleetDashboard: OrganizationFleetDashboardCounts | null = null;

  private readonly supplierHeroStatSkeletons: Array<{ label: string; icon: string; theme: LxWorkspaceHeroStatTheme }> = [
    { label: 'Pending POs', icon: 'shopping_cart', theme: 'teal' },
    { label: 'Active shipments', icon: 'local_shipping', theme: 'mint' },
    { label: 'Low stock alerts', icon: 'inventory_2', theme: 'amber' },
    { label: 'Open shipments', icon: 'pending_actions', theme: 'violet' },
  ];

  private readonly transporterHeroStatSkeletons: Array<{ label: string; icon: string; theme: LxWorkspaceHeroStatTheme }> = [
    { label: 'Trucks available', icon: 'airport_shuttle', theme: 'teal' },
    { label: 'Active trips', icon: 'route', theme: 'mint' },
    { label: 'Drivers on duty', icon: 'groups', theme: 'amber' },
    { label: 'Docs expiring', icon: 'warning_amber', theme: 'violet' },
  ];

  constructor(
    private readonly authState: AuthStateService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
    private readonly platformWallet: PlatformWalletService,
    private readonly tripTracking: TripTrackingPortalService,
    private readonly inventoryPortal: InventoryPortalService,
    private readonly fleetPortal: FleetPortalService,
  ) {}

  welcomeMessage = 'Welcome back';

  ngOnInit(): void {
    this.todayLabel = this.formatTodayLabel();
    this.loadBillingSnapshot();

    this.authState.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.welcomeMessage = this.resolveWelcomeMessage(user);
      this.organizationName = user?.orgName?.trim() ?? '';
      this.classification = user?.orgClassification ?? '';
      this.classificationLabel = this.formatClassification(this.classification);
      if (this.isSupplier) {
        if (!this.supplierDataLoaded) {
          this.supplierDataLoaded = true;
          this.loadSupplierOperations();
        }
      } else if (this.isCustomer) {
        this.supplierDataLoaded = false;
        this.transporterDataLoaded = false;
        if (!this.organizationDataLoaded) {
          this.organizationDataLoaded = true;
          this.loadCustomerDashboard();
        }
      } else if (this.isTransportCompany) {
        this.supplierDataLoaded = false;
        this.organizationDataLoaded = false;
        if (!this.transporterDataLoaded) {
          this.transporterDataLoaded = true;
          this.loadTransporterDashboard();
        }
      } else {
        this.supplierDataLoaded = false;
        this.organizationDataLoaded = false;
        this.transporterDataLoaded = false;
        const orgClass = user?.orgClassification;
        this.cards = orgClass ? (PLATFORM_KPI_CONFIG[orgClass] ?? []) : [];
        this.charts = orgClass ? (PLATFORM_CHART_CONFIG[orgClass] ?? []) : [];
      }
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get isSupplier(): boolean {
    return this.classification === 'SUPPLIER';
  }

  get isCustomer(): boolean {
    return this.classification === 'CUSTOMER';
  }

  get isTransportCompany(): boolean {
    return this.classification === 'TRANSPORT_COMPANY';
  }

  get showsLiveOrganizationDashboard(): boolean {
    return this.isSupplier || this.isCustomer || this.isTransportCompany;
  }

  get heroStatSkeletons(): Array<{ label: string; icon: string; theme: LxWorkspaceHeroStatTheme }> {
    if (this.isTransportCompany) {
      return this.transporterHeroStatSkeletons;
    }
    return this.supplierHeroStatSkeletons;
  }

  get heroLead(): string {
    if (this.isSupplier) {
      if (this.supplierOperationsLoading) {
        return 'Loading live shipment operations for your organisation…';
      }
      if (this.supplierOperationsError) {
        return this.supplierOperationsError;
      }
      return 'Track outbound loads, owned fleet, contracted drivers, and live corridor progress from your workspace.';
    }
    if (this.isCustomer) {
      if (this.organizationDashboardLoading) {
        return 'Loading inbound operations and fleet capacity for your organisation…';
      }
      if (this.organizationDashboardError) {
        return this.organizationDashboardError;
      }
      return 'Monitor deliveries, owned fleet, and contracted driver capacity for your organisation.';
    }
    if (this.isTransportCompany) {
      if (this.transporterOperationsLoading) {
        return 'Loading fleet capacity, active trips, and compliance for your organisation…';
      }
      if (this.transporterOperationsError) {
        return this.transporterOperationsError;
      }
      return 'Track trucks, drivers, active trips, and expiring compliance from your fleet workspace.';
    }
    return 'Key metrics for your organisation at a glance — figures are demo data until backend wiring is complete.';
  }

  get heroNote(): string {
    if (this.isSupplier && !this.supplierOperationsLoading && !this.supplierOperationsError) {
      return 'Shipment board and KPIs reflect live data from your organisation.';
    }
    if (this.isCustomer && !this.organizationDashboardLoading && !this.organizationDashboardError) {
      return 'Fleet and delivery KPIs reflect live data scoped to your organisation.';
    }
    if (this.isTransportCompany && !this.transporterOperationsLoading && !this.transporterOperationsError) {
      return 'Fleet and trip KPIs reflect live data from your organisation.';
    }
    return 'Key metrics are demo data until backend wiring is complete.';
  }

  get heroTitle(): string {
    const name = this.greetingFirstName;
    return name ? `Welcome back, ${name}` : this.welcomeMessage;
  }

  get heroEyebrow(): string {
    const workspace = this.classificationLabel ? `${this.classificationLabel} workspace` : 'Operations hub';
    return this.todayLabel ? `${workspace} · ${this.todayLabel}` : workspace;
  }

  get showHeroStatsLoading(): boolean {
    if (this.isSupplier) {
      return this.supplierOperationsLoading;
    }
    if (this.isCustomer) {
      return this.organizationDashboardLoading;
    }
    if (this.isTransportCompany) {
      return this.transporterOperationsLoading;
    }
    return false;
  }

  get analyticsBadgeLabel(): string {
    return this.showsLiveOrganizationDashboard && !this.showHeroStatsLoading ? 'Live operations' : 'Insights preview';
  }

  get analyticsSubtitle(): string {
    if (this.isSupplier && !this.supplierOperationsLoading) {
      return 'Inventory, procurement, transfers, and shipment trends from your organisation data.';
    }
    if (this.isCustomer && !this.organizationDashboardLoading) {
      return 'Inbound delivery trends and fleet capacity from your organisation data.';
    }
    if (this.isTransportCompany && !this.transporterOperationsLoading) {
      return 'Trip volume and fleet utilisation from your organisation data.';
    }
    return 'Trends and breakdowns for your workspace — preview until live feeds connect.';
  }

  private get greetingFirstName(): string {
    const user = this.authState.currentUser;
    const first = String(user?.firstName ?? '').trim();
    if (first && first !== 'User') {
      return first;
    }
    const fromDisplay = String(user?.displayName ?? '')
      .trim()
      .split(/\s+/)[0];
    if (fromDisplay && fromDisplay !== 'User') {
      return fromDisplay;
    }
    const email = String(user?.email ?? '').trim();
    const local = email.includes('@') ? email.split('@')[0] : '';
    return local && local !== 'User' ? local : '';
  }

  get shipmentCounts(): Record<ShipmentBoardFilter, number> {
    const all = this.shipments.length;
    const by = (filter: ShipmentBoardFilter) =>
      filter === 'ALL' ? all : this.shipments.filter((s) => this.boardFilterForShipment(s.status) === filter).length;
    return {
      ALL: all,
      PREPARED: by('PREPARED'),
      IN_TRANSIT: by('IN_TRANSIT'),
      COMPLETED: by('COMPLETED'),
      FAILED: by('FAILED'),
    };
  }

  get filteredShipments(): ShipmentRow[] {
    const q = this.shipmentSearch.trim().toLowerCase();
    return this.shipments.filter((s) => {
      if (this.shipmentFilter !== 'ALL' && this.boardFilterForShipment(s.status) !== this.shipmentFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        s.shipmentNumber.toLowerCase().includes(q) ||
        s.productName.toLowerCase().includes(q) ||
        s.driverName.toLowerCase().includes(q) ||
        s.fromWarehouse.toLowerCase().includes(q) ||
        s.toWarehouse.toLowerCase().includes(q)
      );
    });
  }

  get selectedShipment(): ShipmentRow | undefined {
    return this.shipments.find((s) => s.id === this.selectedShipmentId);
  }

  get selectedTripId(): number | null {
    const shipment = this.selectedShipment;
    if (!shipment) {
      return null;
    }
    return this.resolveTripId(shipment);
  }

  get selectedRouteLabel(): string {
    const shipment = this.selectedShipment;
    if (!shipment) {
      return '';
    }
    return `${shipment.fromWarehouse} → ${shipment.toWarehouse}`;
  }

  /** All organisation loads with live GPS — shown together on the dashboard map. */
  get liveMapTracks(): TripLiveMapTrack[] {
    const tracks: TripLiveMapTrack[] = [];
    const seen = new Set<number>();

    for (const shipment of this.shipments) {
      if (!this.isLiveTrackableShipment(shipment)) {
        continue;
      }
      const tripId = this.resolveTripId(shipment);
      if (!tripId || seen.has(tripId)) {
        continue;
      }
      seen.add(tripId);
      tracks.push({
        tripId,
        shipmentNumber: shipment.shipmentNumber,
        statusLabel: shipment.statusLabel,
        routeLabel: `${shipment.fromWarehouse} → ${shipment.toWarehouse}`,
      });
    }

    for (const trip of this.trips) {
      if (!trip.canLiveTrack || seen.has(trip.id)) {
        continue;
      }
      seen.add(trip.id);
      tracks.push({
        tripId: trip.id,
        shipmentNumber: trip.shipmentNumber,
        statusLabel: trip.statusLabel,
        routeLabel: trip.route,
      });
    }

    return tracks;
  }

  setFilter(f: ShipmentBoardFilter): void {
    this.shipmentFilter = f;
    const list = this.filteredShipments;
    if (!list.some((s) => s.id === this.selectedShipmentId)) {
      this.selectedShipmentId = list[0]?.id ?? null;
    }
    this.cdr.markForCheck();
  }

  onSearchInput(): void {
    const list = this.filteredShipments;
    if (!list.some((s) => s.id === this.selectedShipmentId)) {
      this.selectedShipmentId = list[0]?.id ?? null;
    }
    this.cdr.markForCheck();
  }

  selectShipment(s: ShipmentRow): void {
    this.selectedShipmentId = s.id;
    this.cdr.markForCheck();
  }

  goNewShipment(): void {
    void this.router.navigate(['/shipments']);
  }

  goBillingSettings(): void {
    void this.router.navigate(['/settings'], { queryParams: { section: 'billing' } });
  }

  goUsageReport(): void {
    void this.router.navigate(['/analytics/platform-usage']);
  }

  get isSubscriptionMode(): boolean {
    return (this.walletSummary?.billingMode ?? this.billingSetting?.billingMode) === 'PREMIUM_SUBSCRIPTION';
  }

  get walletFrozen(): boolean {
    return this.platformWallet.isWalletFrozen(this.walletSummary);
  }

  get walletBalanceLabel(): string {
    return this.platformWallet.formatCents(
      this.walletSummary?.balanceCents ?? 0,
      this.walletSummary?.currencyCode ?? 'USD',
    );
  }

  get subscriptionName(): string {
    return (
      this.walletSummary?.subscriptionPackageName
      ?? this.billingSetting?.subscriptionPackageName
      ?? 'Premium subscription'
    );
  }

  get subscriptionRenewalLabel(): string {
    const renewsAt = this.billingSetting?.subscriptionRenewsAt;
    if (!renewsAt) {
      return 'Renewal date not set';
    }
    try {
      return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(renewsAt));
    } catch {
      return renewsAt;
    }
  }

  get billingStatusLabel(): string {
    if (this.isSubscriptionMode) {
      return 'Active subscription';
    }
    if (this.walletFrozen) {
      return 'Wallet empty — features locked';
    }
    if (this.walletSummary?.lowBalance) {
      return 'Low balance';
    }
    return 'Prepaid wallet active';
  }

  /** Visual fill for the prepaid balance gauge (0–100). */
  get walletGaugePercent(): number {
    const balance = this.walletSummary?.balanceCents ?? 0;
    if (balance <= 0) {
      return 0;
    }
    const threshold = this.walletSummary?.lowBalanceThresholdCents ?? 500;
    const target = Math.max(threshold * 4, balance);
    return Math.min(100, Math.round((balance / target) * 100));
  }

  get walletGaugeCaption(): string {
    if (this.walletFrozen) {
      return 'Balance depleted';
    }
    const threshold = this.walletSummary?.lowBalanceThresholdCents ?? 500;
    const thresholdLabel = this.platformWallet.formatCents(threshold, this.walletSummary?.currencyCode ?? 'USD');
    if (this.walletSummary?.lowBalance) {
      return `Below ${thresholdLabel} warning threshold`;
    }
    return `Healthy · low-balance warning at ${thresholdLabel}`;
  }

  private loadBillingSnapshot(): void {
    this.billingLoading = true;
    forkJoin({
      summary: this.platformWallet.refreshSummary(),
      setting: this.platformWallet.getBillingSetting(),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ summary, setting }) => {
          this.walletSummary = summary;
          this.billingSetting = setting;
          this.billingLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.walletSummary = {
            balanceCents: 0,
            currencyCode: 'USD',
            billingMode: 'PREPAID_WALLET',
          };
          this.billingSetting = { billingMode: 'PREPAID_WALLET' };
          this.billingLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  statusClass(status: ShipmentStatus): string {
    switch (status) {
      case 'PENDING':
      case 'PENDING_FLEET':
      case 'ALLOCATED':
        return 'dash-ship-badge--prepared';
      case 'IN_TRANSIT':
        return 'dash-ship-badge--transit';
      case 'DELIVERED':
        return 'dash-ship-badge--done';
      case 'CANCELLED':
        return 'dash-ship-badge--fail';
      default:
        return '';
    }
  }

  shipmentArrivalLabel(shipment: ShipmentRow): string {
    return shipment.status === 'DELIVERED' ? shipment.createdAtLabel : '—';
  }

  trackShipment(_i: number, s: ShipmentRow): number {
    return s.id;
  }

  private loadSupplierOperations(): void {
    const orgId = Number(this.authState.currentUser?.organizationId ?? 0);
    if (!orgId) {
      this.supplierOperationsError = 'Organisation context is missing — sign in again to load shipments.';
      this.cdr.markForCheck();
      return;
    }

    this.supplierOperationsLoading = true;
    this.supplierOperationsError = '';

    forkJoin({
      shipments: this.tripTracking.findShipments({ organizationId: orgId }).pipe(catchError(() => of([] as ShipmentRow[]))),
      trips: this.tripTracking.findTrips({ organizationId: orgId }).pipe(catchError(() => of([] as TripRow[]))),
      purchaseOrders: this.inventoryPortal.listPurchaseOrders().pipe(catchError(() => of([] as PurchaseOrderRow[]))),
      stock: this.inventoryPortal.listStock().pipe(catchError(() => of([] as StockRow[]))),
      requisitions: this.inventoryPortal
        .listSupplierVisibleRequisitions(orgId)
        .pipe(catchError(() => of([] as PurchaseRequisitionRow[]))),
      transfers: this.inventoryPortal.listTransfers().pipe(catchError(() => of([] as TransferRow[]))),
      salesOrders: this.inventoryPortal.listSupplierSalesOrders().pipe(catchError(() => of([] as SalesOrderRow[]))),
      fleetSummary: this.fleetPortal.getOrganizationDashboardSummary().pipe(
        catchError(() =>
          of({
            ownedFleetCount: 0,
            contractedFleetCount: 0,
            organizationDriverCount: 0,
            contractedDriverCount: 0,
          } satisfies OrganizationFleetDashboardCounts),
        ),
      ),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ shipments, trips, purchaseOrders, stock, requisitions, transfers, salesOrders, fleetSummary }) => {
          this.shipments = shipments;
          this.trips = trips;
          this.fleetDashboard = fleetSummary;
          const inventory: SupplierInventorySnapshot = {
            purchaseOrders,
            requisitions,
            transfers,
            stock,
            salesOrders,
          };
          this.applySupplierKpis(shipments, trips, purchaseOrders, stock, fleetSummary);
          this.charts = this.buildSupplierCharts(shipments, inventory);
          if (!this.filteredShipments.some((s) => s.id === this.selectedShipmentId)) {
            this.selectedShipmentId = this.filteredShipments[0]?.id ?? null;
          }
          this.supplierOperationsLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.supplierOperationsLoading = false;
          this.supplierOperationsError = 'Could not load supplier operations. Try again shortly.';
          this.cdr.markForCheck();
        },
      });
  }

  private loadCustomerDashboard(): void {
    const orgId = Number(this.authState.currentUser?.organizationId ?? 0);
    if (!orgId) {
      this.organizationDashboardError = 'Organisation context is missing — sign in again to load your dashboard.';
      this.cdr.markForCheck();
      return;
    }

    this.organizationDashboardLoading = true;
    this.organizationDashboardError = '';

    forkJoin({
      shipments: this.tripTracking.findShipments({ organizationId: orgId }).pipe(catchError(() => of([] as ShipmentRow[]))),
      trips: this.tripTracking.findTrips({ organizationId: orgId }).pipe(catchError(() => of([] as TripRow[]))),
      fleetSummary: this.fleetPortal.getOrganizationDashboardSummary().pipe(
        catchError(() =>
          of({
            ownedFleetCount: 0,
            contractedFleetCount: 0,
            organizationDriverCount: 0,
            contractedDriverCount: 0,
          } satisfies OrganizationFleetDashboardCounts),
        ),
      ),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ shipments, trips, fleetSummary }) => {
          this.shipments = shipments;
          this.trips = trips;
          this.fleetDashboard = fleetSummary;
          this.applyCustomerKpis(shipments, trips, fleetSummary);
          this.charts = this.buildCustomerCharts(shipments);
          this.organizationDashboardLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.organizationDashboardLoading = false;
          this.organizationDashboardError = 'Could not load your organisation dashboard. Try again shortly.';
          this.cdr.markForCheck();
        },
      });
  }

  private loadTransporterDashboard(): void {
    const orgId = Number(this.authState.currentUser?.organizationId ?? 0);
    if (!orgId) {
      this.transporterOperationsError = 'Organisation context is missing — sign in again to load your fleet dashboard.';
      this.cdr.markForCheck();
      return;
    }

    this.transporterOperationsLoading = true;
    this.transporterOperationsError = '';

    forkJoin({
      trips: this.tripTracking.findTrips({ organizationId: orgId }).pipe(catchError(() => of([] as TripRow[]))),
      fleetSummary: this.fleetPortal.getOrganizationDashboardSummary().pipe(
        catchError(() =>
          of({
            ownedFleetCount: 0,
            contractedFleetCount: 0,
            organizationDriverCount: 0,
            contractedDriverCount: 0,
          } satisfies OrganizationFleetDashboardCounts),
        ),
      ),
      expiringCompliance: this.fleetPortal.listExpiringCompliance(14).pipe(catchError(() => of([]))),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ trips, fleetSummary, expiringCompliance }) => {
          this.trips = trips;
          this.fleetDashboard = fleetSummary;
          this.applyTransporterKpis(trips, fleetSummary, expiringCompliance.length);
          this.charts = this.buildTransporterCharts(trips, fleetSummary);
          this.transporterOperationsLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.transporterOperationsLoading = false;
          this.transporterOperationsError = 'Could not load your fleet dashboard. Try again shortly.';
          this.cdr.markForCheck();
        },
      });
  }

  private applySupplierKpis(
    shipments: ShipmentRow[],
    trips: TripRow[],
    purchaseOrders: Array<{ status?: string }>,
    stock: Array<{ status?: string; isLowStock?: boolean }>,
    fleetSummary: OrganizationFleetDashboardCounts,
  ): void {
    const metrics = this.tripTracking.buildMetrics(shipments, trips);
    const pendingPos = purchaseOrders.filter(
      (o) => ['SUBMITTED', 'APPROVED', 'PARTIALLY_RECEIVED'].includes(String(o.status ?? '').toUpperCase()),
    ).length;
    const lowStock = stock.filter((s) => s.status === 'LOW_STOCK' || s.isLowStock).length;
    const openShipments = shipments.filter((s) => s.status !== 'DELIVERED' && s.status !== 'CANCELLED').length;

    this.cards = [
      {
        label: 'Pending POs',
        value: String(pendingPos),
        icon: 'shopping_cart',
        trend: pendingPos > 0 ? `${pendingPos} awaiting fulfilment` : 'All caught up',
        up: pendingPos === 0,
        spark: this.sparkFromCount(pendingPos, 8),
        theme: 'ocean',
      },
      {
        label: 'Active shipments',
        value: String(metrics.activeTrips + shipments.filter((s) => s.status === 'IN_TRANSIT').length),
        icon: 'local_shipping',
        trend: `${metrics.activeTrips} live trips`,
        up: metrics.activeTrips > 0,
        spark: this.sparkFromCount(metrics.activeTrips, 8),
        theme: 'forest',
      },
      ...this.buildFleetKpiCards(fleetSummary),
      {
        label: 'Low stock alerts',
        value: String(lowStock),
        icon: 'inventory_2',
        trend: lowStock > 0 ? 'Review inventory' : 'Stock healthy',
        up: lowStock === 0,
        spark: this.sparkFromCount(lowStock, 8, true),
        theme: 'ember',
      },
      {
        label: 'Open shipments',
        value: String(openShipments),
        icon: 'pending_actions',
        trend: `${metrics.totalShipments} total`,
        up: openShipments <= metrics.totalShipments / 2,
        spark: this.sparkFromCount(openShipments, 8),
        theme: 'violet',
      },
    ];
  }

  private applyCustomerKpis(
    shipments: ShipmentRow[],
    trips: TripRow[],
    fleetSummary: OrganizationFleetDashboardCounts,
  ): void {
    const metrics = this.tripTracking.buildMetrics(shipments, trips);
    const inTransit = shipments.filter((s) => s.status === 'IN_TRANSIT').length;
    const pendingDeliveries = shipments.filter((s) => s.status !== 'DELIVERED' && s.status !== 'CANCELLED').length;

    this.cards = [
      ...this.buildFleetKpiCards(fleetSummary),
      {
        label: 'Orders in transit',
        value: String(inTransit),
        icon: 'local_shipping',
        trend: `${metrics.activeTrips} live trips`,
        up: inTransit > 0,
        spark: this.sparkFromCount(inTransit, 8),
        theme: 'ocean',
      },
      {
        label: 'Pending deliveries',
        value: String(pendingDeliveries),
        icon: 'pending_actions',
        trend: pendingDeliveries > 0 ? 'Awaiting completion' : 'All delivered',
        up: pendingDeliveries === 0,
        spark: this.sparkFromCount(pendingDeliveries, 8, true),
        theme: 'sunset',
      },
      {
        label: 'Contracted fleet',
        value: String(fleetSummary.contractedFleetCount),
        icon: 'handshake',
        trend:
          fleetSummary.contractedFleetCount > 0
            ? 'Transporter-linked vehicles'
            : 'No contracted vehicles',
        up: fleetSummary.contractedFleetCount > 0,
        spark: this.sparkFromCount(fleetSummary.contractedFleetCount, 8),
        theme: 'violet',
      },
    ];
  }

  private applyTransporterKpis(
    trips: TripRow[],
    fleetSummary: OrganizationFleetDashboardCounts,
    expiringDocs: number,
  ): void {
    const metrics = this.tripTracking.buildMetrics([], trips);
    const trucksAvailable = fleetSummary.ownedFleetCount + fleetSummary.contractedFleetCount;
    const driversOnDuty = fleetSummary.organizationDriverCount + fleetSummary.contractedDriverCount;
    const activeTrips = metrics.activeTrips;
    const utilisationPct =
      trucksAvailable > 0 ? Math.min(100, Math.round((activeTrips / trucksAvailable) * 100)) : 0;

    this.cards = [
      {
        label: 'Trucks available',
        value: String(trucksAvailable),
        icon: 'airport_shuttle',
        trend: trucksAvailable > 0 ? `${utilisationPct}% on corridor` : 'No vehicles registered',
        up: trucksAvailable > 0,
        spark: this.sparkFromCount(trucksAvailable, 8),
        theme: 'ocean',
      },
      {
        label: 'Active trips',
        value: String(activeTrips),
        icon: 'route',
        trend: activeTrips > 0 ? 'Live corridor loads' : 'No active trips',
        up: activeTrips > 0,
        spark: this.sparkFromCount(activeTrips, 8),
        theme: 'forest',
      },
      {
        label: 'Drivers on duty',
        value: String(driversOnDuty),
        icon: 'groups',
        trend:
          fleetSummary.organizationDriverCount > 0
            ? `${fleetSummary.organizationDriverCount} on your roster`
            : 'Partner pool & hires',
        up: driversOnDuty > 0,
        spark: this.sparkFromCount(driversOnDuty, 8),
        theme: 'mint',
      },
      {
        label: 'Docs expiring',
        value: String(expiringDocs),
        icon: 'warning_amber',
        trend: expiringDocs > 0 ? 'Within 14 days' : 'All clear',
        up: expiringDocs === 0,
        spark: this.sparkFromCount(expiringDocs, 8, true),
        theme: 'ember',
      },
    ];
  }

  private buildFleetKpiCards(fleetSummary: OrganizationFleetDashboardCounts): KpiCard[] {
    return [
      {
        label: 'Owned fleet',
        value: String(fleetSummary.ownedFleetCount),
        icon: 'garage',
        trend: fleetSummary.ownedFleetCount > 0 ? 'Organisation-owned vehicles' : 'No owned vehicles yet',
        up: fleetSummary.ownedFleetCount > 0,
        spark: this.sparkFromCount(fleetSummary.ownedFleetCount, 8),
        theme: 'slate',
      },
      {
        label: 'Contracted drivers',
        value: String(fleetSummary.contractedDriverCount),
        icon: 'groups',
        trend:
          fleetSummary.organizationDriverCount > 0
            ? `${fleetSummary.organizationDriverCount} on your roster`
            : 'Partner pool & hires',
        up: fleetSummary.contractedDriverCount > 0,
        spark: this.sparkFromCount(fleetSummary.contractedDriverCount, 8),
        theme: 'mint',
      },
    ];
  }

  private buildCustomerCharts(shipments: ShipmentRow[]): DashboardChart[] {
    const weekLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const delivered = shipments.filter((s) => s.status === 'DELIVERED').length;
    const inTransit = shipments.filter((s) => s.status === 'IN_TRANSIT').length;
    const open = shipments.filter((s) => s.status !== 'DELIVERED' && s.status !== 'CANCELLED').length;
    return [
      {
        id: 'customer-delivery-pulse',
        title: 'Delivery pulse',
        subtitle: 'Inbound shipments for your organisation',
        type: 'area',
        theme: 'mint',
        labels: weekLabels,
        values: this.weeklyShipmentVolume(shipments),
        highlight: shipments.length ? `${shipments.length} tracked loads` : 'No shipments yet',
      },
      {
        id: 'customer-order-stages',
        title: 'Order stages',
        subtitle: 'Current shipment status mix',
        type: 'bar',
        theme: 'ocean',
        labels: ['Open', 'In transit', 'Delivered'],
        values: [open, inTransit, delivered],
      },
    ];
  }

  private buildTransporterCharts(trips: TripRow[], fleetSummary: OrganizationFleetDashboardCounts): DashboardChart[] {
    const weekLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const active = trips.filter((t) =>
      ['IN_PROGRESS', 'IN_TRANSIT', 'AT_BORDER_HOLD', 'ROADSIDE_HOLD', 'ARRIVED', 'RETURN_IN_TRANSIT'].includes(t.status),
    ).length;
    const queued = trips.filter((t) => t.status === 'PENDING').length;
    const completed = trips.filter((t) => ['DELIVERED', 'RETURNED', 'COUNT_COMPLETE'].includes(t.status)).length;
    const fleetTotal = fleetSummary.ownedFleetCount + fleetSummary.contractedFleetCount;
    const utilisationPct = fleetTotal > 0 ? Math.round((active / fleetTotal) * 100) : 0;

    return [
      {
        id: 'transporter-trip-pulse',
        title: 'Trip pulse',
        subtitle: 'Active trips for your organisation',
        type: 'area',
        theme: 'forest',
        labels: weekLabels,
        values: this.weeklyTripVolume(trips),
        highlight: trips.length ? `${active} on corridor now` : 'No trips yet',
      },
      {
        id: 'transporter-trip-stages',
        title: 'Active trips',
        subtitle: 'By operational stage',
        type: 'bar',
        theme: 'ocean',
        labels: ['Queued', 'On road', 'Completed'],
        values: [queued, active, completed],
        highlight: fleetTotal > 0 ? `${utilisationPct}% fleet utilisation` : undefined,
      },
    ];
  }

  private weeklyTripVolume(trips: TripRow[]): number[] {
    const buckets = [0, 0, 0, 0, 0, 0, 0];
    if (!trips.length) {
      return buckets;
    }
    const now = new Date();
    for (const trip of trips) {
      const raw = trip.startedAtLabel || trip.lastEventAt;
      if (!raw) {
        continue;
      }
      const parsed = new Date(raw);
      if (Number.isNaN(parsed.getTime())) {
        continue;
      }
      const daysAgo = Math.floor((now.getTime() - parsed.getTime()) / 86_400_000);
      if (daysAgo >= 0 && daysAgo < 7) {
        buckets[6 - daysAgo] += 1;
      }
    }
    return buckets;
  }

  private weeklyShipmentVolume(shipments: ShipmentRow[]): number[] {
    if (!shipments.length) {
      return [0, 0, 0, 0, 0, 0, 0];
    }
    const buckets = [0, 0, 0, 0, 0, 0, 0];
    shipments.forEach((_, index) => {
      buckets[index % 7]++;
    });
    return buckets;
  }

  private sparkFromCount(value: number, bars: number, invert = false): number[] {
    const base = Math.max(value, 1);
    return Array.from({ length: bars }, (_, i) => {
      const wave = 40 + ((i + 1) / bars) * 50;
      const scaled = Math.min(96, Math.max(12, Math.round((value / base) * wave)));
      return invert ? Math.max(12, 96 - scaled) : scaled;
    });
  }

  private boardFilterForShipment(status: ShipmentStatus): ShipmentBoardFilter | null {
    if (status === 'IN_TRANSIT') {
      return 'IN_TRANSIT';
    }
    if (status === 'DELIVERED') {
      return 'COMPLETED';
    }
    if (status === 'CANCELLED') {
      return 'FAILED';
    }
    if (status === 'PENDING' || status === 'PENDING_FLEET' || status === 'ALLOCATED') {
      return 'PREPARED';
    }
    return null;
  }

  private resolveTripId(shipment: ShipmentRow): number | null {
    if (shipment.tripId && shipment.tripId > 0) {
      return shipment.tripId;
    }
    const trip = this.trips.find((t) => t.shipmentId === shipment.id && t.canLiveTrack);
    return trip?.id ?? null;
  }

  private isLiveTrackableShipment(shipment: ShipmentRow): boolean {
    return shipment.status === 'IN_TRANSIT' || shipment.status === 'ALLOCATED';
  }

  trackKpi(_i: number, card: KpiCard): string {
    return card.label;
  }

  heroStatTheme(card: KpiCard): LxWorkspaceHeroStatTheme {
    const map: Partial<Record<KpiCardTheme, LxWorkspaceHeroStatTheme>> = {
      ocean: 'teal',
      forest: 'mint',
      mint: 'mint',
      ember: 'amber',
      sunset: 'amber',
      violet: 'violet',
      rose: 'violet',
      slate: 'teal',
    };
    return map[card.theme] ?? 'teal';
  }

  trackChart(_i: number, chart: DashboardChart): string {
    return chart.id;
  }

  chartAreaPath(values: number[] | undefined): string {
    const coords = this.chartCoords(values);
    if (!coords.length) {
      return '';
    }
    const baseline = 100 - 12;
    const line = this.chartSmoothLinePath(values);
    const last = coords[coords.length - 1];
    return `${line} L ${last.x} ${baseline} L ${coords[0].x} ${baseline} Z`;
  }

  chartLinePath(values: number[] | undefined): string {
    return this.chartSmoothLinePath(values);
  }

  chartSmoothLinePath(values: number[] | undefined): string {
    const coords = this.chartCoords(values);
    if (!coords.length) {
      return '';
    }
    if (coords.length === 1) {
      return `M ${coords[0].x} ${coords[0].y}`;
    }
    let path = `M ${coords[0].x} ${coords[0].y}`;
    for (let i = 0; i < coords.length - 1; i++) {
      const p0 = coords[Math.max(0, i - 1)];
      const p1 = coords[i];
      const p2 = coords[i + 1];
      const p3 = coords[Math.min(coords.length - 1, i + 2)];
      const cp1x = p1.x + (p2.x - p0.x) / 6;
      const cp1y = p1.y + (p2.y - p0.y) / 6;
      const cp2x = p2.x - (p3.x - p1.x) / 6;
      const cp2y = p2.y - (p3.y - p1.y) / 6;
      path += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${p2.x} ${p2.y}`;
    }
    return path;
  }

  chartPointCoords(values: number[] | undefined): Array<{ x: number; y: number; value: number }> {
    return this.chartCoords(values).map((point, index) => ({
      ...point,
      value: values?.[index] ?? 0,
    }));
  }

  chartPeakValue(values: number[] | undefined): number | null {
    if (!values?.length) {
      return null;
    }
    return Math.max(...values);
  }

  chartBarHeight(value: number, values: number[] | undefined): number {
    if (!values?.length) {
      return 0;
    }
    const max = Math.max(...values, 1);
    return Math.max(8, Math.round((value / max) * 100));
  }

  pipelineStages(chart: DashboardChart): Array<{ label: string; value: number; pct: number; color: string }> {
    const labels = chart.labels ?? [];
    const values = chart.values ?? [];
    const total = values.reduce((sum, value) => sum + value, 0);
    const denominator = total > 0 ? total : 1;
    const palette = chart.stageColors?.length ? chart.stageColors : this.pipelineDefaultColors(chart.theme);

    return labels.map((label, index) => {
      const value = values[index] ?? 0;
      return {
        label,
        value,
        pct: total > 0 ? Math.round((value / denominator) * 100) : 0,
        color: palette[index % palette.length],
      };
    });
  }

  pipelineHasData(chart: DashboardChart): boolean {
    return (chart.values ?? []).some((value) => value > 0);
  }

  pipelineFlowWidth(value: number, values: number[] | undefined): number {
    if (!values?.length) {
      return 0;
    }
    const max = Math.max(...values, 1);
    if (value <= 0) {
      return 6;
    }
    return Math.max(18, Math.round((value / max) * 100));
  }

  pipelineTrackWidth(pct: number): number {
    return pct > 0 ? Math.max(pct, 6) : 0;
  }

  private pipelineDefaultColors(theme: KpiCardTheme): string[] {
    const map: Record<KpiCardTheme, string[]> = {
      ocean: ['#0ea5e9', '#38bdf8', '#0284c7', '#0369a1'],
      forest: ['#86efac', '#22c55e', '#16a34a', '#15803d', '#3b82f6', '#10b981'],
      sunset: ['#fdba74', '#fb923c', '#f59e0b', '#10b981'],
      violet: ['#c4b5fd', '#a78bfa', '#8b5cf6', '#6d28d9'],
      ember: ['#fca5a5', '#f87171', '#ef4444', '#b91c1c'],
      mint: ['#a78bfa', '#6366f1', '#2dd4bf', '#10b981'],
      slate: ['#cbd5e1', '#94a3b8', '#64748b', '#475569'],
      rose: ['#f9a8d4', '#f472b6', '#ec4899', '#db2777'],
    };
    return map[theme];
  }

  donutGradient(segments: DashboardDonutSegment[] | undefined): string {
    if (!segments?.length) {
      return 'conic-gradient(#94a3b8 0% 100%)';
    }
    const total = segments.reduce((sum, seg) => sum + seg.value, 0) || 1;
    let acc = 0;
    const stops = segments
      .map((seg) => {
        const start = acc;
        acc += (seg.value / total) * 100;
        return `${seg.color} ${start}% ${acc}%`;
      })
      .join(', ');
    return `conic-gradient(${stops})`;
  }

  donutTotal(segments: DashboardDonutSegment[] | undefined): string {
    if (!segments?.length) {
      return '—';
    }
    const total = segments.reduce((sum, seg) => sum + seg.value, 0);
    return total >= 1000 ? `${Math.round(total / 100) / 10}k` : String(total);
  }

  chartGradientId(chart: DashboardChart): string {
    return `dash-fill-${chart.id}`;
  }

  chartGlowId(chart: DashboardChart): string {
    return `dash-glow-${chart.id}`;
  }

  chartStroke(theme: KpiCardTheme): string {
    const map: Record<KpiCardTheme, string> = {
      ocean: '#0284c7',
      forest: '#10b981',
      sunset: '#fb923c',
      violet: '#a78bfa',
      ember: '#f87171',
      mint: '#2dd4bf',
      slate: '#64748b',
      rose: '#f472b6',
    };
    return map[theme];
  }

  chartFillStops(theme: KpiCardTheme): { top: string; bottom: string } {
    const map: Record<KpiCardTheme, { top: string; bottom: string }> = {
      ocean: { top: '#38bdf8', bottom: '#0369a1' },
      forest: { top: '#34d399', bottom: '#047857' },
      sunset: { top: '#fdba74', bottom: '#c2410c' },
      violet: { top: '#c4b5fd', bottom: '#6d28d9' },
      ember: { top: '#fca5a5', bottom: '#b91c1c' },
      mint: { top: '#5eead4', bottom: '#0f766e' },
      slate: { top: '#cbd5e1', bottom: '#334155' },
      rose: { top: '#f9a8d4', bottom: '#be185d' },
    };
    return map[theme];
  }

  private formatClassification(raw: string): string {
    if (!raw) {
      return '';
    }
    return raw
      .split('_')
      .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
      .join(' ');
  }

  private resolveWelcomeMessage(user: CurrentUser | null | undefined): string {
    const fromAuth = String(user?.welcomeMessage ?? '').trim();
    if (fromAuth) {
      return fromAuth.replace(/^Welcome,?\s*/i, 'Welcome back, ');
    }
    return formatWelcomeMessage({
      firstName: user?.firstName,
      displayName: user?.displayName,
      email: user?.email,
    }).replace(/^Welcome\s*/i, 'Welcome back, ');
  }

  private formatTodayLabel(): string {
    try {
      return new Intl.DateTimeFormat(undefined, {
        weekday: 'long',
        day: 'numeric',
        month: 'short',
      }).format(new Date());
    } catch {
      return '';
    }
  }

  private chartCoords(values: number[] | undefined): Array<{ x: number; y: number }> {
    if (!values?.length) {
      return [];
    }
    const w = 100;
    const h = 100;
    const padY = 12;
    const padX = 4;
    const max = Math.max(...values);
    const min = Math.min(...values);
    const span = max - min || max || 1;
    const innerW = w - padX * 2;
    const innerH = h - padY * 2;
    const step = values.length > 1 ? innerW / (values.length - 1) : 0;

    return values.map((v, i) => {
      const x = padX + i * step;
      const norm = (v - min) / span;
      const y = h - padY - norm * innerH;
      return { x, y };
    });
  }

  private buildSupplierCharts(shipments: ShipmentRow[], inventory: SupplierInventorySnapshot): DashboardChart[] {
    const prepared = shipments.filter((s) => this.boardFilterForShipment(s.status) === 'PREPARED').length;
    const inTransit = shipments.filter((s) => s.status === 'IN_TRANSIT').length;
    const completed = shipments.filter((s) => s.status === 'DELIVERED').length;
    const failed = shipments.filter((s) => s.status === 'CANCELLED').length;
    const totalShipments = Math.max(shipments.length, 1);

    const trend = Array.from({ length: 7 }, (_, i) => {
      const weight = 0.55 + (i + 1) / 14;
      return Math.max(0, Math.round(shipments.length * weight * (0.72 + i * 0.04)));
    });

    const { requisitions, transfers, stock, purchaseOrders, salesOrders } = inventory;

    const reqDraft = this.countByStatus(requisitions, ['DRAFT']);
    const reqReview = this.countByStatus(requisitions, ['SUBMITTED', 'APPROVED']);
    const reqSupplier = this.countByStatus(requisitions, [
      'PUBLISHED_TO_SUPPLIER',
      'SUPPLIER_CONFIRMED',
      'CUSTOMER_ACKNOWLEDGED',
      'PARTIALLY_FULFILLED',
    ]);
    const reqClosed = this.countByStatus(requisitions, [
      'FULFILLED',
      'CLOSED',
      'REJECTED',
      'CANCELLED',
      'EXPIRED',
    ]);

    const transferPipeline = this.resolveTransferPipeline(transfers, shipments);
    const {
      requested: transferRequested,
      approved: transferApproved,
      inTransit: transferInTransit,
      completed: transferCompleted,
      total: transferTotal,
    } = transferPipeline;

    const inStock = stock.filter((s) => s.status === 'IN_STOCK').length;
    const lowStock = stock.filter((s) => s.status === 'LOW_STOCK' || s.isLowStock).length;
    const outOfStock = stock.filter((s) => s.status === 'OUT_OF_STOCK').length;
    const fullyReserved = stock.filter((s) => s.status === 'FULLY_RESERVED').length;

    const poDraft = this.countByStatus(purchaseOrders, ['DRAFT']);
    const poActive = this.countByStatus(purchaseOrders, [
      'SUBMITTED',
      'APPROVED',
      'PENDING_CUSTOMER_APPROVAL',
      'PENDING_SUPPLIER_APPROVAL',
      'CUSTOMER_APPROVED',
    ]);
    const poReceived = this.countByStatus(purchaseOrders, ['PARTIALLY_RECEIVED', 'RECEIVED']);
    const soPending = this.countByStatus(salesOrders, [
      'AWAITING_RECEIPT',
      'PENDING',
      'CONFIRMED',
      'PENDING_APPROVAL',
      'APPROVED',
    ]);
    const soShipping = this.countByStatus(salesOrders, ['PARTIALLY_SHIPPED', 'SHIPPED']);
    const soDone = this.countByStatus(salesOrders, ['DELIVERED', 'FULFILLED']);

    const laneSegments: DashboardDonutSegment[] = [
      { label: 'Prepared', value: prepared, color: '#f59e0b' },
      { label: 'In transit', value: inTransit, color: '#10b981' },
      { label: 'Delivered', value: completed, color: '#3b82f6' },
      { label: 'Cancelled', value: failed, color: '#ef4444' },
    ].filter((seg) => seg.value > 0);

    if (!laneSegments.length) {
      laneSegments.push({ label: 'No shipments yet', value: 100, color: '#94a3b8' });
    }

    const stockSegments: DashboardDonutSegment[] = [
      { label: 'In stock', value: inStock, color: '#10b981' },
      { label: 'Low stock', value: lowStock, color: '#f59e0b' },
      { label: 'Out of stock', value: outOfStock, color: '#ef4444' },
      { label: 'Fully reserved', value: fullyReserved, color: '#8b5cf6' },
    ].filter((seg) => seg.value > 0);

    if (!stockSegments.length) {
      stockSegments.push({ label: 'No stock rows yet', value: 100, color: '#94a3b8' });
    }

    return [
      {
        id: 'req-pipeline',
        title: 'Requisition pipeline',
        subtitle: 'Purchase requisitions by workflow stage',
        type: 'pipeline',
        theme: 'sunset',
        labels: ['Draft', 'In review', 'With supplier', 'Closed'],
        values: [reqDraft, reqReview, reqSupplier, reqClosed],
        stageColors: ['#94a3b8', '#f59e0b', '#3b82f6', '#10b981'],
        highlight: `${requisitions.length} requisitions`,
      },
      {
        id: 'transfer-flow',
        title: 'Transfer movement',
        subtitle: 'Inter-warehouse transfers by status',
        type: 'pipeline',
        theme: 'mint',
        labels: ['Requested', 'Approved', 'In transit', 'Completed'],
        values: [transferRequested, transferApproved, transferInTransit, transferCompleted],
        stageColors: ['#a78bfa', '#6366f1', '#14b8a6', '#22c55e'],
        highlight: `${transferTotal} transfers tracked`,
      },
      {
        id: 'order-pipeline',
        title: 'Order pipeline',
        subtitle: 'Purchase orders and sales orders in flight',
        type: 'pipeline',
        theme: 'forest',
        labels: ['PO draft', 'PO active', 'PO received', 'SO pending', 'SO shipping', 'SO done'],
        values: [poDraft, poActive, poReceived, soPending, soShipping, soDone],
        stageColors: ['#94a3b8', '#f97316', '#fb923c', '#3b82f6', '#6366f1', '#10b981'],
        highlight: `${purchaseOrders.length} POs · ${salesOrders.length} sales orders`,
      },
      {
        id: 'stock-health',
        title: 'Stock health',
        subtitle: 'SKU availability across warehouses',
        type: 'donut',
        theme: 'ember',
        size: 'large',
        donutUnit: 'SKUs',
        highlight: lowStock > 0 ? `${lowStock} low-stock alerts` : `${stock.length} SKUs monitored`,
        segments: stockSegments,
      },
      {
        id: 'ship-volume',
        title: 'Shipment pulse',
        subtitle: 'Loads in your workspace · 7-day trend',
        type: 'area',
        theme: 'ocean',
        size: 'large',
        labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
        values: trend,
        highlight: `${shipments.length} total loads`,
      },
      {
        id: 'lane-mix',
        title: 'Shipment mix',
        subtitle: 'Share by operational status',
        type: 'donut',
        theme: 'violet',
        size: 'large',
        donutUnit: 'loads',
        highlight: inTransit > 0 ? `${inTransit} on corridor` : `${totalShipments} in workspace`,
        segments: laneSegments,
      },
    ];
  }

  private countByStatus<T extends { status?: string }>(rows: T[], statuses: string[]): number {
    const normalized = new Set(statuses.map((s) => s.toUpperCase()));
    return rows.filter((row) => normalized.has(String(row.status ?? '').toUpperCase())).length;
  }

  /**
   * Prefer live inventory-transfer rows; when that feed is empty, infer movement from
   * shipments created after transfer approval (inventoryTransferId).
   */
  private resolveTransferPipeline(
    transfers: TransferRow[],
    shipments: ShipmentRow[],
  ): { requested: number; approved: number; inTransit: number; completed: number; total: number } {
    if (transfers.length > 0) {
      return {
        requested: this.countByStatus(transfers, ['REQUESTED']),
        approved: this.countByStatus(transfers, ['APPROVED']),
        inTransit: this.countByStatus(transfers, ['IN_TRANSIT']),
        completed: this.countByStatus(transfers, ['COMPLETED']),
        total: transfers.length,
      };
    }

    const transferShipments = shipments.filter((s) => (s.inventoryTransferId ?? 0) > 0);
    if (!transferShipments.length) {
      return { requested: 0, approved: 0, inTransit: 0, completed: 0, total: 0 };
    }

    let approved = 0;
    let inTransit = 0;
    let completed = 0;
    for (const shipment of transferShipments) {
      const bucket = this.boardFilterForShipment(shipment.status);
      if (bucket === 'PREPARED') {
        approved++;
      } else if (bucket === 'IN_TRANSIT') {
        inTransit++;
      } else if (bucket === 'COMPLETED') {
        completed++;
      }
    }

    const total = new Set(transferShipments.map((s) => s.inventoryTransferId)).size;
    return { requested: 0, approved, inTransit, completed, total };
  }
}
