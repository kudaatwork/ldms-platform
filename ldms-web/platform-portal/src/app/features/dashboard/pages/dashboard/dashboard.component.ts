import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { Router } from '@angular/router';
import { OrganizationClassification } from '../../../../core/models/auth.model';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { formatWelcomeMessage } from '../../../../core/utils/welcome-message.util';
import type { LxWorkspaceHeroStatTheme } from '../../../../shared/components/lx-workspace-hero-stat/lx-workspace-hero-stat.component';
import {
  DashboardChart,
  DashboardDonutSegment,
  KpiCard,
  KpiCardTheme,
  PLATFORM_CHART_CONFIG,
  PLATFORM_KPI_CONFIG,
  SUPPLIER_SHIPMENT_MOCKS,
  SupplierShipmentCard,
  SupplierShipmentStatus,
} from '../../data/platform-mock-data';

type ShipmentFilter = 'ALL' | SupplierShipmentStatus;

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

  readonly supplierShipments = SUPPLIER_SHIPMENT_MOCKS;

  shipmentFilter: ShipmentFilter = 'ALL';
  shipmentSearch = '';
  selectedShipmentId: string | null = SUPPLIER_SHIPMENT_MOCKS[0]?.id ?? null;

  constructor(
    private readonly authState: AuthStateService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
  ) {}

  welcomeMessage = 'Welcome';

  ngOnInit(): void {
    this.authState.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.welcomeMessage = formatWelcomeMessage({
        firstName: user?.firstName,
        displayName: user?.displayName,
        email: user?.email,
      });
      this.classification = user?.orgClassification ?? '';
      this.classificationLabel = this.formatClassification(this.classification);
      this.cards = user ? PLATFORM_KPI_CONFIG[user.orgClassification] : [];
      this.charts = user ? PLATFORM_CHART_CONFIG[user.orgClassification] : [];
      if (this.isSupplier) {
        this.selectedShipmentId = this.filteredShipments[0]?.id ?? null;
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

  get heroLead(): string {
    if (this.isSupplier) {
      return 'Track outbound loads, drivers, and ETAs — demo data until live shipment APIs are connected.';
    }
    return 'Key metrics for your organisation at a glance — figures are demo data until backend wiring is complete.';
  }

  get heroEyebrow(): string {
    return this.classificationLabel ? `${this.classificationLabel} workspace` : 'Operations hub';
  }

  get shipmentCounts(): Record<ShipmentFilter, number> {
    const all = this.supplierShipments.length;
    const by = (s: SupplierShipmentStatus) => this.supplierShipments.filter((x) => x.status === s).length;
    return {
      ALL: all,
      PREPARED: by('PREPARED'),
      IN_TRANSIT: by('IN_TRANSIT'),
      COMPLETED: by('COMPLETED'),
      FAILED: by('FAILED'),
    };
  }

  get filteredShipments(): SupplierShipmentCard[] {
    const q = this.shipmentSearch.trim().toLowerCase();
    return this.supplierShipments.filter((s) => {
      if (this.shipmentFilter !== 'ALL' && s.status !== this.shipmentFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        s.shipmentNo.toLowerCase().includes(q) ||
        s.category.toLowerCase().includes(q) ||
        s.driver.toLowerCase().includes(q) ||
        s.departureLabel.toLowerCase().includes(q) ||
        s.arrivalLabel.toLowerCase().includes(q)
      );
    });
  }

  get selectedShipment(): SupplierShipmentCard | undefined {
    return this.supplierShipments.find((s) => s.id === this.selectedShipmentId);
  }

  setFilter(f: ShipmentFilter): void {
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

  selectShipment(s: SupplierShipmentCard): void {
    this.selectedShipmentId = s.id;
    this.cdr.markForCheck();
  }

  goNewShipment(): void {
    void this.router.navigate(['/shipments']);
  }

  statusClass(status: SupplierShipmentStatus): string {
    switch (status) {
      case 'PREPARED':
        return 'dash-ship-badge--prepared';
      case 'IN_TRANSIT':
        return 'dash-ship-badge--transit';
      case 'COMPLETED':
        return 'dash-ship-badge--done';
      case 'FAILED':
        return 'dash-ship-badge--fail';
      default:
        return '';
    }
  }

  statusLabel(status: SupplierShipmentStatus): string {
    switch (status) {
      case 'PREPARED':
        return 'Prepared';
      case 'IN_TRANSIT':
        return 'In transit';
      case 'COMPLETED':
        return 'Completed';
      case 'FAILED':
        return 'Failed';
      default:
        return status;
    }
  }

  trackShipment(_i: number, s: SupplierShipmentCard): string {
    return s.id;
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
    if (!values?.length) {
      return '';
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
    const baseline = h - padY;

    const coords = values.map((v, i) => {
      const x = padX + i * step;
      const norm = (v - min) / span;
      const y = h - padY - norm * innerH;
      return { x, y };
    });

    let d = `M ${coords[0].x} ${baseline} L ${coords[0].x} ${coords[0].y}`;
    for (let i = 1; i < coords.length; i++) {
      d += ` L ${coords[i].x} ${coords[i].y}`;
    }
    const last = coords[coords.length - 1];
    d += ` L ${last.x} ${baseline} Z`;
    return d;
  }

  chartLinePath(values: number[] | undefined): string {
    if (!values?.length) {
      return '';
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

    return values
      .map((v, i) => {
        const x = padX + i * step;
        const norm = (v - min) / span;
        const y = h - padY - norm * innerH;
        return `${i === 0 ? 'M' : 'L'} ${x} ${y}`;
      })
      .join(' ');
  }

  chartBarHeight(value: number, values: number[] | undefined): number {
    if (!values?.length) {
      return 0;
    }
    const max = Math.max(...values, 1);
    return Math.max(8, Math.round((value / max) * 100));
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
}
