import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  NgZone,
  OnDestroy,
  computed,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { gsap } from 'gsap';
import { environment } from '../../../../../environments/environment';
import { LandingMotionService } from '../../services/landing-motion.service';

/** Pexels CDN — logistics, distribution & supply chain (pexels.com licence). */
const px = (id: number, w = 800, h = 1000) =>
  `https://images.pexels.com/photos/${id}/pexels-photo-${id}.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=${w}&h=${h}&fit=crop`;

const PEX = {
  truckHighway: 1562324,
  warehouseForklift: 979721,
  warehousePallets: 1267979,
  warehouseInterior: 7394214,
  warehouseScanning: 6195126,
  containerTerminal: 4480502,
  containersAerial: 5668772,
  truckHighwaySide: 4484079,
  truckFleet: 4391473,
  truckLoading: 4482144,
  highwayInterchange: 267583,
  distributionCenter: 6863332,
  intermodalYard: 974314,
  yardForklifts: 5025514,
  inventoryScan: 4480362,
  logisticsDashboard: 7688339,
  truckConvoy: 6770619,
  routePlanning: 4483775,
  shippingPort: 906494,
} as const;

interface LandingMobileApp {
  id: string;
  name: string;
  tagline: string;
  icon: string;
  accent: string;
  features: string[];
  screenTitle: string;
  screenMetric: string;
  screenStatus: string;
}

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LandingComponent implements AfterViewInit, OnDestroy {
  readonly adminUrl = environment.adminPortalOrigin;
  readonly platformOrigin = environment.platformPortalOrigin;
  readonly appleAppStoreUrl = environment.mobileApps.appleAppStoreUrl;
  readonly googlePlayStoreUrl = environment.mobileApps.googlePlayStoreUrl;

  readonly activeFeatureTab = signal(0);
  readonly activeFeature = computed(() => this.featureTabs[this.activeFeatureTab()]);

  readonly showcaseVideoSrc =
    'https://videos.pexels.com/video-files/4482437/4482437-hd_1920_1080_25fps.mp4';
  readonly showcaseVideoPoster = px(PEX.containerTerminal, 1200, 675);
  readonly heroImage = px(PEX.truckHighway, 1920, 1080);
  readonly journeyImage = px(PEX.highwayInterchange, 900, 1100);
  readonly ctaBandImage = px(PEX.yardForklifts, 1200, 800);
  readonly accessPortalCardImage = px(PEX.warehousePallets, 800, 520);
  readonly accessDriverCardImage = px(PEX.truckHighwaySide, 800, 520);
  readonly accessAdminCardImage = px(PEX.distributionCenter, 800, 520);

  readonly heroStats = [
    { value: '23', suffix: '', label: 'Integrated microservices' },
    { value: '7', suffix: '+', label: 'Actor roles on the corridor' },
    { value: '12', suffix: '', label: 'On-platform lifecycle phases' },
    { value: '24', suffix: '/7', label: 'Event-driven visibility' },
  ] as const;

  readonly platformModules = [
    {
      icon: 'apartment',
      title: 'Organisation portal',
      description:
        'The cloud workspace for suppliers, customers, transporters, clearing agents, and partners — role-aware dashboards from order to settlement.',
      cta: 'Create account',
      action: 'signup' as const,
    },
    {
      icon: 'admin_panel_settings',
      title: 'Administrator console',
      description:
        'Platform operations, KYC review, organisation oversight, and cross-tenant monitoring — isolated for LX stewards.',
      cta: 'Open admin',
      action: 'admin' as const,
    },
    {
      icon: 'phone_android',
      title: 'Driver mobile',
      description:
        'Trips, GPS stops, fuel requests, and proof of delivery — built for crews on long-haul and cross-border lanes.',
      cta: 'Get the app',
      action: 'mobile' as const,
    },
    {
      icon: 'map',
      title: 'Ops mobile',
      description:
        'Live map, trip approvals, and exception handling for dispatchers who need the full corridor in their pocket.',
      cta: 'Get the app',
      action: 'mobile' as const,
    },
  ] as const;

  readonly benefits = [
    {
      icon: 'hub',
      title: 'Built for any corridor',
      body: 'From local distribution to multi-country bulk programmes — orders, trips, compliance, and finance in one narrative.',
    },
    {
      icon: 'devices',
      title: 'Hardware-ready tracking',
      body: 'Fleet assets, GPS devices, and telematics integrations designed for trucks, trailers, and mixed fleets on the road.',
    },
    {
      icon: 'integration_instructions',
      title: 'Open integrations',
      body: 'Event-driven architecture with API gateway access — connect ERP, BI, accounting, and partner systems without brittle chains.',
    },
    {
      icon: 'support_agent',
      title: 'Guided implementation',
      body: 'A twelve-phase on-platform path from organisation setup through go-live, settlements, and steady-state KPIs.',
    },
  ] as const;

  readonly featureTabs = [
    {
      icon: 'my_location',
      label: 'Live tracking',
      title: 'Track every asset on the map',
      body: 'Monitor trucks, trailers, and trips in near real time. Geofences, border posts, and weighbridges surface as events — not inbox noise.',
      image: px(PEX.routePlanning, 960, 640),
      imageAlt: 'Route planning and live corridor tracking',
    },
    {
      icon: 'assignment',
      label: 'Orders & inventory',
      title: 'From purchase order to dispatch',
      body: 'Reserve stock, approve programmes, and release shipments when documents and payments align — one connected inventory story.',
      image: px(PEX.warehouseScanning, 960, 640),
      imageAlt: 'Warehouse scanning and order fulfilment',
    },
    {
      icon: 'local_shipping',
      label: 'Trips & fleet',
      title: 'Assign, comply, and depart',
      body: 'Match drivers and assets, validate compliance, and start trips with confidence. Fuel, expenses, and roadside stops stay on the same thread.',
      image: px(PEX.truckFleet, 960, 640),
      imageAlt: 'Commercial fleet ready for corridor dispatch',
    },
    {
      icon: 'verified_user',
      label: 'Compliance & KYC',
      title: 'Trust baked into every lane',
      body: 'Organisation verification, document packs, and permit tracking — staged before cargo moves and auditable when regulators ask.',
      image: px(PEX.containersAerial, 960, 640),
      imageAlt: 'Container yard and compliance oversight',
    },
    {
      icon: 'payments',
      label: 'Billing & settlement',
      title: 'Finance that follows the trip',
      body: 'Invoices from proof of delivery, wallet settlements, and expense reconciliation — money moves with the operational story.',
      image: px(PEX.logisticsDashboard, 960, 640),
      imageAlt: 'Logistics finance and settlement dashboards',
    },
    {
      icon: 'notifications_active',
      label: 'Smart alerts',
      title: 'Respond before exceptions escalate',
      body: 'SLA breaches, approval reminders, and trip events routed to the right role — email, in-app, and mobile without duplicate logic.',
      image: px(PEX.distributionCenter, 960, 640),
      imageAlt: 'Distribution control centre with live notifications',
    },
  ] as const;

  readonly pillars = [
    { icon: 'assignment', label: 'Order management' },
    { icon: 'local_shipping', label: 'Transport & trips' },
    { icon: 'verified_user', label: 'Compliance & KYC' },
    { icon: 'map', label: 'Maps & tracking' },
    { icon: 'payment', label: 'Payments & settlement' },
    { icon: 'dashboard', label: 'Dashboards & KPIs' },
    { icon: 'inventory_2', label: 'Inventory & GRV' },
    { icon: 'gavel', label: 'Clearing & borders' },
  ] as const;

  readonly pillarsMarquee = [...this.pillars, ...this.pillars, ...this.pillars];

  readonly industries = [
    {
      title: 'Bulk commodities',
      body: 'Cement, fuel, minerals — high-volume lanes with weighbridge and border choreography.',
      image: px(PEX.truckConvoy, 640, 480),
      imageAlt: 'Bulk commodity trucks on a freight corridor',
    },
    {
      title: 'Packaged goods',
      body: 'Warehouse-to-customer programmes with reservations, GRV, and invoice alignment.',
      image: px(PEX.warehousePallets, 640, 480),
      imageAlt: 'Packaged goods staged in a distribution warehouse',
    },
    {
      title: 'Cross-border freight',
      body: 'Clearing agents, customs docs, and multi-jurisdiction compliance in one thread.',
      image: px(PEX.intermodalYard, 640, 480),
      imageAlt: 'Intermodal yard for cross-border logistics',
    },
    {
      title: 'Fleet operators',
      body: 'Asset registry, driver compliance, GPS devices, and utilisation across your network.',
      image: px(PEX.truckHighwaySide, 640, 480),
      imageAlt: 'Fleet vehicle on a long-haul highway route',
    },
    {
      title: 'Fuel & roadside',
      body: 'Operational fund requests, fuel sessions, and mechanic stops tied to active trips.',
      image: px(PEX.truckLoading, 640, 480),
      imageAlt: 'Truck loading at a fuel and roadside stop',
    },
    {
      title: 'Public & institutional',
      body: 'Government partners and regulated programmes with audit trails and role isolation.',
      image: px(PEX.shippingPort, 640, 480),
      imageAlt: 'Port and institutional logistics operations',
    },
  ] as const;

  readonly personas = [
    {
      role: 'Suppliers',
      detail: 'Orchestrate demand, inventory, and customer promises from one portal.',
      image: px(PEX.warehousePallets, 640, 480),
      imageAlt: 'Supplier distribution warehouse',
    },
    {
      role: 'Customers',
      detail: 'Track orders, shipments, and proof of delivery with full transparency.',
      image: px(PEX.warehouseInterior, 640, 480),
      imageAlt: 'Customer-facing distribution operations',
    },
    {
      role: 'Transporters',
      detail: 'Show capacity, compliance, and trip performance as craft — not commodity tonnage.',
      image: px(PEX.truckConvoy, 640, 480),
      imageAlt: 'Transporter fleet on the corridor',
    },
    {
      role: 'Drivers',
      detail: 'Mobile-first trips, stops, fuel, and delivery confirmation on the road.',
      image: px(PEX.truckHighwaySide, 640, 480),
      imageAlt: 'Driver on a distribution route',
    },
    {
      role: 'Clearing agents',
      detail: 'Border documentation and status handoffs without chasing version chaos.',
      image: px(PEX.containersAerial, 640, 480),
      imageAlt: 'Clearing and customs at container yard',
    },
    {
      role: 'Platform admin',
      detail: 'KYC, organisations, and ecosystem health from a dedicated console.',
      image: px(PEX.distributionCenter, 640, 480),
      imageAlt: 'Platform operations control centre',
    },
  ] as const;

  readonly journeySteps = [
    {
      n: 1,
      title: 'Organisation onboarding',
      detail: 'Register, verify email, complete KYC, and invite corridor partners.',
    },
    {
      n: 2,
      title: 'Master data & inventory',
      detail: 'Warehouses, products, stock, and customer organisations in place.',
    },
    {
      n: 3,
      title: 'Orders & reservations',
      detail: 'Purchase orders, approvals, and stock reserved before dispatch.',
    },
    {
      n: 4,
      title: 'Shipments & documents',
      detail: 'Dispatch, compliance packs, and ready-for-release when finance aligns.',
    },
    {
      n: 5,
      title: 'Trips & tracking',
      detail: 'Assign fleet, start trips, record GPS stops, and manage expenses.',
    },
    {
      n: 6,
      title: 'Delivery & billing',
      detail: 'GRV, proof of delivery, invoicing, and settlement — audit-ready.',
    },
  ] as const;

  readonly testimonials = [
    {
      quote:
        'LDMS gives us one workspace from PO through trip to invoice. Our commercial and operations teams finally read the same story.',
      name: 'Operations director',
      org: 'Regional bulk distributor',
    },
    {
      quote:
        'Cross-border used to mean spreadsheets and phone trees. Now border posts, clearing, and finance stay on the same trip thread.',
      name: 'Logistics manager',
      org: 'Multi-country transporter',
    },
    {
      quote:
        'The event-driven alerts mean we respond to SLA breaches before customers call — without drowning dispatch in noise.',
      name: 'Supply chain lead',
      org: 'Packaged goods supplier',
    },
  ] as const;

  readonly trustItems = [
    { icon: 'security', title: 'Role-based access', detail: 'Isolated portals for operators and platform stewards.' },
    { icon: 'cloud_done', title: 'Event-driven core', detail: 'RabbitMQ backbone — no brittle synchronous chains.' },
    { icon: 'history', title: 'Audit trail ready', detail: 'Entity history and soft deletes across services.' },
  ] as const;

  readonly mobileApps: LandingMobileApp[] = [
    {
      id: 'driver',
      name: 'Driver',
      tagline: 'Trips on the road',
      icon: 'local_shipping',
      accent: '#3b82f6',
      features: ['GPS stops & border posts', 'Fuel & fund requests', 'Proof of delivery'],
      screenTitle: 'Active trip',
      screenMetric: '847 km',
      screenStatus: 'En route · Beitbridge',
    },
    {
      id: 'ops',
      name: 'Ops',
      tagline: 'Corridor command',
      icon: 'map',
      accent: '#f97316',
      features: ['Live fleet map', 'Trip approvals', 'Exception handling'],
      screenTitle: 'Live corridor',
      screenMetric: '47 trips',
      screenStatus: '12 border crossings today',
    },
    {
      id: 'receiver',
      name: 'Receiver',
      tagline: 'Delivery confirmation',
      icon: 'qr_code_scanner',
      accent: '#22c55e',
      features: ['QR scan & GRV', 'Delivery sign-off', 'Discrepancy capture'],
      screenTitle: 'Goods receipt',
      screenMetric: 'GRV-2041',
      screenStatus: 'Ready to confirm',
    },
  ];

  readonly activeMobileApp = signal(0);
  readonly activeMobileAppData = computed(() => this.mobileApps[this.activeMobileApp()]);

  readonly faqs = [
    {
      q: 'Where do our teams sign in?',
      a: 'Day-to-day users work in the organisation portal. LX administrators use the separate admin console for KYC, organisations, and monitoring.',
    },
    {
      q: 'Is LDMS only for one commodity type?',
      a: 'No. LDMS supports bulk, packaged, and specialised road programmes — especially corridors that cross borders and regulatory boundaries.',
    },
    {
      q: 'How does implementation work?',
      a: 'Follow the six-phase path on this page — from onboarding through master data, orders, trips, and billing. Your programme can align workshops to each phase.',
    },
    {
      q: 'Can we integrate with existing systems?',
      a: 'Yes. Services expose APIs through the gateway and publish domain events for ERP, BI, accounting, and partner integrations.',
    },
  ] as const;

  private readonly onWindowScroll = (): void => {
    // Scroll chrome handled by landing shell.
  };

  constructor(
    private readonly router: Router,
    private readonly el: ElementRef<HTMLElement>,
    private readonly ngZone: NgZone,
    private readonly motion: LandingMotionService,
  ) {}

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      this.initHeroReveal();
      this.initCounters();
      this.initFeatureTabMotion();
    });
  }

  ngOnDestroy(): void {
    window.removeEventListener('scroll', this.onWindowScroll);
  }

  selectFeatureTab(index: number): void {
    this.activeFeatureTab.set(index);
    this.ngZone.runOutsideAngular(() => {
      requestAnimationFrame(() => this.animateFeaturePanel());
    });
  }

  selectMobileApp(index: number): void {
    this.activeMobileApp.set(index);
    this.ngZone.runOutsideAngular(() => {
      requestAnimationFrame(() => this.animateMobilePreview());
    });
  }

  onModuleAction(action: 'signup' | 'admin' | 'features' | 'mobile'): void {
    if (action === 'signup') {
      this.goSignup();
      return;
    }
    if (action === 'admin') {
      window.open(`${this.adminUrl}/auth/login`, '_blank', 'noopener');
      return;
    }
    if (action === 'mobile') {
      this.scrollTo('ldms-mobile');
      return;
    }
    this.scrollTo('ldms-features');
  }

  private animateFeaturePanel(): void {
    const panel = this.el.nativeElement.querySelector('.ldms-feature-panel__visual');
    if (panel) {
      gsap.fromTo(panel, { opacity: 0, x: 24 }, { opacity: 1, x: 0, duration: 0.45, ease: 'power2.out' });
    }
  }

  private animateMobilePreview(): void {
    const screen = this.el.nativeElement.querySelector('.ldms-mobile__screen-content');
    if (screen) {
      gsap.fromTo(screen, { opacity: 0, y: 12 }, { opacity: 1, y: 0, duration: 0.4, ease: 'power2.out' });
    }
  }

  private initScrollBar(): void {
    // noop — shell owns scroll bar
  }

  private initHeroReveal(): void {
    this.motion.initHeroReveal(
      this.el.nativeElement,
      '.ldms-hero__eyebrow, .ldms-hero__title, .ldms-hero__lead, .ldms-hero__cta, .ldms-hero__apps, .ldms-hero__visual',
    );
  }

  private initSectionReveals(): void {
    // noop — shell binds section reveals for projected content
  }

  private initCounters(): void {
    const counters = this.el.nativeElement.querySelectorAll('.ldms-stat__num[data-count]');
    if (!counters.length) {
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (!e.isIntersecting) {
            return;
          }
          const targetEl = e.target as HTMLElement;
          const target = parseFloat(targetEl.dataset['count'] ?? '0');
          const obj = { val: 0 };
          gsap.to(obj, {
            val: target,
            duration: 2,
            ease: 'power2.out',
            onUpdate: () => {
              targetEl.textContent = String(Math.round(obj.val));
            },
          });
          io.unobserve(targetEl);
        });
      },
      { threshold: 0.5 },
    );
    counters.forEach((element: Element) => io.observe(element));
  }

  private initNavScrollState(): void {
    // noop — shell owns nav scroll state
  }

  private initFeatureTabMotion(): void {
    const tabs = this.el.nativeElement.querySelectorAll('.ldms-feature-tab');
    tabs.forEach((tab: Element, i: number) => {
      gsap.fromTo(
        tab,
        { opacity: 0, y: 12 },
        { opacity: 1, y: 0, duration: 0.4, delay: 0.5 + i * 0.05, ease: 'power2.out' },
      );
    });
  }

  private initPrepaidDemoMotion(): void {
    // prepaid demo lives on /pricing
  }

  private updateScrollChrome(): void {
    // noop
  }

  scrollTo(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  goSignup(): void {
    void this.router.navigate(['/signup']);
  }

  goDriverSignup(): void {
    void this.router.navigate(['/driver/signup']);
  }

  goDemo(): void {
    void this.router.navigate(['/demo']);
  }

  goBookDemo(): void {
    void this.router.navigate(['/contact']);
  }
}
