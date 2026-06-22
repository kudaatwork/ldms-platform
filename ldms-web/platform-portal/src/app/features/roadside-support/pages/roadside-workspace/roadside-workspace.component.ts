import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import * as L from 'leaflet';
import { Subject, catchError, of, takeUntil } from 'rxjs';
import { NotificationService } from '../../../../core/services/notification.service';
import type { OperationalFundRequestRow } from '../../../trip-tracking/models/fuel-expenses.model';
import { FuelExpensesPortalService } from '../../../trip-tracking/services/fuel-expenses-portal.service';
import {
  RoadsideProviderService,
  type RoadsideProviderRow,
  type RoadsideProviderType,
} from '../../../trip-tracking/services/roadside-provider.service';

export type RoadsideSection = 'visits' | 'fuel-log' | 'incidents' | 'service-log';

interface SectionConfig {
  eyebrow: string;
  title: string;
  lead: string;
  hubIcon: string;
  providerTypes: RoadsideProviderType[];
  requestType?: 'FUEL_TOP_UP' | 'MECHANIC' | 'FUNDS';
  emptyHint: string;
}

const SECTION_CONFIG: Record<RoadsideSection, SectionConfig> = {
  visits: {
    eyebrow: 'Fuel corridor',
    title: 'Truck visits',
    lead: 'See trucks heading your way, approve fuel stops, and keep the corridor moving.',
    hubIcon: 'local_gas_station',
    providerTypes: ['FUEL_STATION'],
    requestType: 'FUEL_TOP_UP',
    emptyHint: 'When drivers plan fuel stops on live trips, visit requests appear here for your station.',
  },
  'fuel-log': {
    eyebrow: 'Dispense history',
    title: 'Fuel log',
    lead: 'Approved top-ups, litres dispensed, and corridor fuel activity in one ledger.',
    hubIcon: 'oil_barrel',
    providerTypes: ['FUEL_STATION'],
    requestType: 'FUEL_TOP_UP',
    emptyHint: 'Fuel dispense records from approved driver requests will show here.',
  },
  incidents: {
    eyebrow: 'Roadside response',
    title: 'Incidents & repairs',
    lead: 'Mechanic call-outs, breakdown holds, and urgent roadside support along the route.',
    hubIcon: 'car_crash',
    providerTypes: ['MECHANIC', 'ROADSIDE_SUPPORT'],
    requestType: 'MECHANIC',
    emptyHint: 'Mechanic fund requests from drivers in ROADSIDE_HOLD appear here for your workshop.',
  },
  'service-log': {
    eyebrow: 'Service history',
    title: 'Service log',
    lead: 'Completed fuel stops, repairs, and support visits — your operational audit trail.',
    hubIcon: 'build',
    providerTypes: ['FUEL_STATION', 'MECHANIC', 'ROADSIDE_SUPPORT'],
    emptyHint: 'Completed roadside visits and approved requests are listed here.',
  },
};

@Component({
  selector: 'app-roadside-workspace',
  templateUrl: './roadside-workspace.component.html',
  styleUrl: './roadside-workspace.component.scss',
  standalone: false,
})
export class RoadsideWorkspaceComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapHost') mapHost?: ElementRef<HTMLDivElement>;

  section: RoadsideSection = 'visits';
  loading = true;
  loadError = '';
  providers: RoadsideProviderRow[] = [];
  filteredProviders: RoadsideProviderRow[] = [];
  requests: OperationalFundRequestRow[] = [];
  selectedProvider: RoadsideProviderRow | null = null;
  mapScrollEnabled = false;

  private map?: L.Map;
  private baseTileLayer?: L.TileLayer;
  private providerMarkers: L.Marker[] = [];
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly title: Title,
    private readonly roadsideProvidersApi: RoadsideProviderService,
    private readonly fuelExpenses: FuelExpensesPortalService,
    private readonly notifications: NotificationService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.route.data.pipe(takeUntil(this.destroy$)).subscribe((data) => {
      this.section = (data['section'] as RoadsideSection) ?? 'visits';
      this.title.setTitle(`${this.config.title} | LX Platform`);
      this.selectedProvider = null;
      this.reload();
    });
  }

  ngAfterViewInit(): void {
    requestAnimationFrame(() => this.ensureMap());
  }

  ngOnDestroy(): void {
    this.providerMarkers.forEach((m) => m.remove());
    this.providerMarkers = [];
    this.map?.remove();
    this.map = undefined;
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.map?.invalidateSize({ animate: false });
  }

  get config(): SectionConfig {
    return SECTION_CONFIG[this.section];
  }

  get metrics() {
    const pending = this.requests.filter((r) => r.status === 'PENDING').length;
    const approved = this.requests.filter((r) => r.status === 'APPROVED').length;
    return {
      providers: this.filteredProviders.length,
      pending,
      approved,
      open24: this.filteredProviders.filter((p) => p.open24Hours).length,
    };
  }

  reload(): void {
    this.loading = true;
    this.loadError = '';
    this.roadsideProvidersApi
      .listAll()
      .pipe(takeUntil(this.destroy$), catchError((err: Error) => {
        this.loadError = err.message;
        return of([]);
      }))
      .subscribe((rows: RoadsideProviderRow[]) => {
        this.providers = rows;
        this.applyProviderFilter();
        this.renderProviderMarkers();
        this.loading = false;
        this.cdr.markForCheck();
      });

    const requestFilter: {
      page?: number;
      size?: number;
      requestType?: 'FUEL_TOP_UP' | 'MECHANIC' | 'FUNDS';
      status?: string;
    } = {
      page: 0,
      size: 40,
    };
    if (this.config.requestType) {
      requestFilter.requestType = this.config.requestType;
    }
    if (this.section === 'service-log') {
      requestFilter.status = 'APPROVED';
    }

    this.fuelExpenses
      .findFundRequests(requestFilter)
      .pipe(takeUntil(this.destroy$), catchError(() => of([] as OperationalFundRequestRow[])))
      .subscribe((rows: OperationalFundRequestRow[]) => {
        this.requests = rows;
        this.cdr.markForCheck();
      });
  }

  selectProvider(provider: RoadsideProviderRow): void {
    this.selectedProvider = provider;
    if (this.map) {
      this.map.setView([provider.latitude, provider.longitude], 11, { animate: true });
    }
    this.cdr.markForCheck();
  }

  clearSelection(): void {
    this.selectedProvider = null;
    this.fitAllProviders();
    this.cdr.markForCheck();
  }

  providerGlyph(type: string): string {
    switch (String(type).toUpperCase()) {
      case 'MECHANIC':
        return '🔧';
      case 'ROADSIDE_SUPPORT':
        return '🛟';
      default:
        return '⛽';
    }
  }

  statusClass(status: string): string {
    switch (String(status).toUpperCase()) {
      case 'APPROVED':
        return 'approved';
      case 'REJECTED':
        return 'rejected';
      case 'CANCELLED':
        return 'inactive';
      default:
        return 'pending';
    }
  }

  enableMapScroll(): void {
    if (!this.map || this.mapScrollEnabled) {
      return;
    }
    this.map.scrollWheelZoom.enable();
    this.mapScrollEnabled = true;
  }

  mapZoomIn(): void {
    this.map?.zoomIn();
  }

  mapZoomOut(): void {
    this.map?.zoomOut();
  }

  recenterMap(): void {
    this.fitAllProviders();
  }

  private applyProviderFilter(): void {
    const allowed = new Set(this.config.providerTypes.map((t) => String(t).toUpperCase()));
    this.filteredProviders = this.providers.filter((p) => allowed.has(String(p.providerType).toUpperCase()));
  }

  private ensureMap(): void {
    const host = this.mapHost?.nativeElement;
    if (!host || this.map) {
      return;
    }
    if (host.offsetWidth < 2 || host.offsetHeight < 2) {
      requestAnimationFrame(() => this.ensureMap());
      return;
    }

    this.map = L.map(host, {
      zoomControl: false,
      scrollWheelZoom: false,
      attributionControl: true,
    }).setView([-19.015438, 29.154857], 7);

    this.baseTileLayer = L.tileLayer(
      'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
      {
        attribution: '&copy; OpenStreetMap &copy; CARTO',
        subdomains: 'abcd',
        maxZoom: 19,
      },
    ).addTo(this.map);

    host.addEventListener('click', () => this.enableMapScroll());
    this.map.on('mouseout', () => {
      this.map?.scrollWheelZoom.disable();
      this.mapScrollEnabled = false;
    });

    this.renderProviderMarkers();
    requestAnimationFrame(() => this.map?.invalidateSize({ animate: false }));
  }

  private renderProviderMarkers(): void {
    if (!this.map) {
      return;
    }
    this.providerMarkers.forEach((m) => m.remove());
    this.providerMarkers = this.filteredProviders.map((p) =>
      L.marker([p.latitude, p.longitude], {
        icon: this.providerIcon(p),
        zIndexOffset: this.selectedProvider?.id === p.id ? 500 : 300,
      })
        .bindTooltip(
          `<strong>${p.name}</strong><br>${p.providerTypeLabel}${p.verified ? ' · Verified' : ''}`,
          { direction: 'top', sticky: true },
        )
        .on('click', () => this.selectProvider(p))
        .addTo(this.map!),
    );
    this.fitAllProviders();
  }

  private fitAllProviders(): void {
    if (!this.map || !this.filteredProviders.length) {
      return;
    }
    const bounds = L.latLngBounds(this.filteredProviders.map((p) => [p.latitude, p.longitude]));
    try {
      this.map.fitBounds(bounds, { padding: [48, 48], maxZoom: 9 });
    } catch {
      /* ignore empty bounds */
    }
  }

  private providerIcon(p: RoadsideProviderRow): L.DivIcon {
    const tone =
      String(p.providerType).toUpperCase() === 'MECHANIC'
        ? 'mechanic'
        : String(p.providerType).toUpperCase() === 'ROADSIDE_SUPPORT'
          ? 'support'
          : 'fuel';
    const selected = this.selectedProvider?.id === p.id;
    return L.divIcon({
      className: `rs-map-pin rs-map-pin--${tone}${selected ? ' rs-map-pin--selected' : ''}`,
      html: `<span class="rs-map-pin__glyph">${this.providerGlyph(p.providerType)}</span>`,
      iconSize: [38, 38],
      iconAnchor: [19, 19],
    });
  }
}
