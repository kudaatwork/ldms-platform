import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  NgZone,
  OnDestroy,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { gsap } from 'gsap';
import { environment } from '../../../../../environments/environment';
import { ThemeService } from '../../../../core/services/theme.service';

/** Pexels CDN — logistics, distribution & supply chain (pexels.com licence). */
const px = (id: number, w = 800, h = 1000) =>
  `https://images.pexels.com/photos/${id}/pexels-photo-${id}.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=${w}&h=${h}&fit=crop`;

/** Curated Pexels photo IDs — road freight, warehouses, containers, distribution. */
const PEX = {
  truckHighway: 1562324,
  warehouseForklift: 979721,
  warehousePallets: 1267979,
  warehouseAisle: 7688460,
  warehouseInterior: 7394214,
  warehouseScanning: 6195126,
  warehouseCartons: 6347793,
  shippingPort: 906494,
  containerStack: 4483616,
  containerTerminal: 4480502,
  containersAerial: 5668772,
  truckHighwaySide: 4484079,
  truckFleet: 4391473,
  truckLoading: 4482144,
  semiTruck: 4481923,
  highwayInterchange: 267583,
  distributionCenter: 6863332,
  supplyChainCartons: 1095814,
  intermodalYard: 974314,
  yardForklifts: 5025514,
  inventoryScan: 4480362,
  freightDocuments: 3635874,
  logisticsDashboard: 7688339,
  packagingLine: 7658355,
  loadingDock: 2802373,
  truckConvoy: 6770619,
  routePlanning: 4483775,
} as const;

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

  /** Touch / keyboard flip: which purpose cards are flipped open. */
  private readonly flippedPurpose = signal<Set<number>>(new Set());
  private readonly flippedMore = signal<Set<number>>(new Set());
  readonly mobileNavOpen = signal(false);

  readonly showcaseVideoSrc =
    'https://videos.pexels.com/video-files/4482437/4482437-hd_1920_1080_25fps.mp4';
  readonly showcaseVideoPoster = px(PEX.containerTerminal, 1200, 675);

  readonly heroImage = px(PEX.truckHighway, 1600, 1067);

  readonly introImage = px(PEX.warehouseInterior, 960, 1200);
  readonly automationImage = px(PEX.warehouseForklift, 960, 960);

  readonly splitNotifyImage = px(PEX.warehouseScanning, 960, 640);
  readonly splitImpactImage = px(PEX.truckConvoy, 960, 640);

  readonly journeyImage = px(PEX.highwayInterchange, 900, 1100);
  readonly faqAsideImage = px(PEX.intermodalYard, 900, 1100);
  readonly ctaBandImage = px(PEX.yardForklifts, 1200, 800);

  readonly heroChips = [
    'Orders through trips in one narrative',
    'Built for busy corridors and borders',
    'Status without inbox overload',
    'Scales from one lane to many',
  ] as const;

  readonly pillars = [
    { icon: 'assignment', label: 'Order management' },
    { icon: 'local_shipping', label: 'Transport & trips' },
    { icon: 'verified_user', label: 'Compliance & KYC' },
    { icon: 'map', label: 'Maps & tracking' },
    { icon: 'payment', label: 'Payments & settlement' },
    { icon: 'dashboard', label: 'Dashboards & KPIs' },
  ] as const;

  /** Duplicated for seamless marquee loop. */
  readonly pillarsMarquee = [...this.pillars, ...this.pillars, ...this.pillars];

  readonly introProof = [
    'Fewer detours through spreadsheets, calls, and “who has the latest version?”',
    'Leadership sees load, margin, and risk across partners and regions',
    'Drivers, yards, and finance read the same trip and settlement story',
    'Enterprise delivery today with room to grow as your programmes expand',
  ] as const;

  readonly purposePalettes: ReadonlyArray<{
    title: string;
    accent: string;
    tagline: string;
    story: string;
    image: string;
    imageAlt: string;
  }> = [
    {
      title: 'Relationships',
      accent: '& revenue',
      tagline: 'Winning lanes start with relationships that feel effortless.',
      story:
        'Give commercial teams a confident workspace where accounts, commitments, and promises stay aligned—so every load feels personal, not procedural.',
      image: px(PEX.logisticsDashboard, 800, 1000),
      imageAlt: 'Logistics planners reviewing distribution schedules on screen',
    },
    {
      title: 'Confidence',
      accent: 'on paper',
      tagline: 'Compliance becomes a quiet superpower—not a Sunday scramble.',
      story:
        'Turn filings, permits, and approvals into a polished narrative partners trust. LDMS keeps the backstage tidy so your front stage shines.',
      image: px(PEX.containerStack, 800, 1000),
      imageAlt: 'Stacked shipping containers at an intermodal freight yard',
    },
    {
      title: 'Fleet',
      accent: 'in flow',
      tagline: 'Capacity that feels orchestrated, not improvised.',
      story:
        'Blend crews, assets, and timing into one graceful motion. The platform celebrates your operators while protecting the standards you cannot bend.',
      image: px(PEX.truckFleet, 800, 1000),
      imageAlt: 'Commercial freight truck ready for corridor dispatch',
    },
    {
      title: 'Live',
      accent: 'perspective',
      tagline: 'Everyone watches the same horizon—not their own guesswork.',
      story:
        'Bring near-real awareness to every stakeholder without drowning them in noise. Calm maps, crisp moments, and human-readable progress.',
      image: px(PEX.routePlanning, 800, 1000),
      imageAlt: 'Supply chain route planning and corridor visibility',
    },
    {
      title: 'Together',
      accent: 'on mission',
      tagline: 'Agents, yards, and HQ finally speak the same language.',
      story:
        'Break silos with shared rituals: quick huddles, transparent handoffs, and celebrations when milestones land—because logistics is a team sport.',
      image: px(PEX.warehouseScanning, 800, 1000),
      imageAlt: 'Warehouse team scanning outbound distribution orders',
    },
    {
      title: 'Capital',
      accent: 'with clarity',
      tagline: 'Money moves with the story—not against it.',
      story:
        'Match commercial creativity with guardrails finance loves. Flexible rhythms for how you collect, release, and reconcile—without losing the plot.',
      image: px(PEX.freightDocuments, 800, 1000),
      imageAlt: 'Freight documentation and customs paperwork for settlement',
    },
  ];

  readonly personas: ReadonlyArray<{ role: string; detail: string; image: string; imageAlt: string }> = [
    {
      role: 'Suppliers',
      detail: 'Shape demand, orchestrate promises, and look heroic to buyers.',
      image: px(PEX.warehousePallets, 640, 480),
      imageAlt: 'Supplier pallets staged in a distribution warehouse',
    },
    {
      role: 'Customers',
      detail: 'Know what is coming, when it lands, and who stands behind it.',
      image: px(PEX.supplyChainCartons, 640, 480),
      imageAlt: 'Packaged goods prepared for customer delivery in the supply chain',
    },
    {
      role: 'Drivers',
      detail: 'Stay focused on the road with guidance that respects their day.',
      image: px(PEX.truckHighwaySide, 640, 480),
      imageAlt: 'Heavy goods vehicle on a long-haul distribution route',
    },
    {
      role: 'Transport partners',
      detail: 'Show capacity as craft—not just commodity tonnage.',
      image: px(PEX.truckConvoy, 640, 480),
      imageAlt: 'Fleet of freight trucks moving cargo along a corridor',
    },
    {
      role: 'Clearing allies',
      detail: 'Turn complexity into choreography everyone can follow.',
      image: px(PEX.containersAerial, 640, 480),
      imageAlt: 'Aerial view of container yard and customs checkpoint activity',
    },
    {
      role: 'Roadside partners',
      detail: 'Be discoverable exactly when crews need you most.',
      image: px(PEX.loadingDock, 640, 480),
      imageAlt: 'Loading dock and yard operations supporting roadside logistics',
    },
    {
      role: 'Platform stewards',
      detail: 'Keep the ecosystem trustworthy without slowing innovators.',
      image: px(PEX.distributionCenter, 640, 480),
      imageAlt: 'Distribution control hub monitoring network-wide freight flow',
    },
  ];

  readonly deepDives: ReadonlyArray<{
    kicker: string;
    title: string;
    lead: string;
    bullets: readonly string[];
    image: string;
    imageAlt: string;
  }> = [
    {
      kicker: 'Digital',
      title: 'Orders that feel inevitable',
      lead:
        'Design a front office experience where quotes, confirmations, and commitments glide together—so customers feel certainty from the first hello.',
      bullets: [
        'Beautifully simple capture moments for repeat programmes',
        'Guardrails that feel helpful—not heavy—to busy desks',
        'Packaging of offers that mirrors how your best reps already sell',
      ],
      image: px(PEX.warehouseCartons, 900, 1100),
      imageAlt: 'Cartons queued in a warehouse for order fulfilment',
    },
    {
      kicker: 'Motion',
      title: 'Trips that tell a story',
      lead:
        'Let dispatch paint the journey in vivid strokes: who moves, what moves, and why it matters—without losing the poetry of a well-run corridor.',
      bullets: [
        'Cinematic visibility tuned for executives and crews alike',
        'Moments that celebrate progress—not just ping status',
        'Breathing room for partners to add colour when reality shifts',
      ],
      image: px(PEX.truckLoading, 900, 1100),
      imageAlt: 'Cargo being loaded onto a distribution truck at the yard',
    },
    {
      kicker: 'Trust',
      title: 'Paperwork with personality',
      lead:
        'Transform compliance into a signature experience: polished, predictable, and quietly reassuring to auditors who peek behind the curtain.',
      bullets: [
        'Narratives that read like strategy—not scavenger hunts',
        'Signals that reassure before anyone has to ask',
        'Archives that feel curated—not abandoned filing cabinets',
      ],
      image: px(PEX.containerTerminal, 900, 1100),
      imageAlt: 'Container terminal and port logistics for compliant freight handling',
    },
    {
      kicker: 'Flow',
      title: 'Treasury that keeps pace',
      lead:
        'Mirror the ambition of your commercial team with experiences finance can embrace—transparent, tactile, and tuned for real-world logistics.',
      bullets: [
        'Rhythms that flex with programmes, seasons, and surprises',
        'Language both CFOs and field leaders actually enjoy reading',
        'Continuity between what was promised and what was collected',
      ],
      image: px(PEX.packagingLine, 900, 1100),
      imageAlt: 'Distribution packaging line supporting invoicing and settlement workflows',
    },
  ];

  readonly journeySteps: ReadonlyArray<{ n: number; title: string; detail: string }> = [
    {
      n: 1,
      title: 'Set the stage',
      detail: 'Organisation setup, branding, and inviting your first corridor partners into the right portals.',
    },
    {
      n: 2,
      title: 'Grow familiar faces',
      detail: 'Supplier and customer records, contacts, and relationship context—so every load has a face behind it.',
    },
    {
      n: 3,
      title: 'Shape the promise',
      detail: 'Programmes, pricing logic, and commercial terms your commercial and finance teams can defend together.',
    },
    {
      n: 4,
      title: 'Dress the details',
      detail: 'KYC packs, permits, and trade documents staged and reviewed before the first shipment moves.',
    },
    {
      n: 5,
      title: 'Choose your guides',
      detail: 'Clearing agents, inspectors, and finance partners mapped to lanes, commodities, and jurisdictions.',
    },
    {
      n: 6,
      title: 'Curate the convoy',
      detail: 'Trips, manifests, capacity, and driver assignments aligned to constraints everyone can see.',
    },
    {
      n: 7,
      title: 'Lift the curtain',
      detail: 'Go-live: notifications, operating rhythms, and “who does what” so day one feels rehearsed, not improvised.',
    },
    {
      n: 8,
      title: 'Keep the pulse',
      detail: 'Day-two support, SLAs, and escalations that stay tied to the order or trip—not lost threads.',
    },
    {
      n: 9,
      title: 'Own the thresholds',
      detail: 'Border posts, weighbridges, and checkpoints with statuses crews, partners, and HQ all trust.',
    },
    {
      n: 10,
      title: 'Fuel the mission',
      detail: 'Invoicing, settlements, proof of delivery, and audit trails with clean lineage for finance and regulators.',
    },
    {
      n: 11,
      title: 'Close with applause',
      detail: 'Customer confirmations, handover rituals, and close-out reporting that finish the story professionally.',
    },
    {
      n: 12,
      title: 'Encore visibility',
      detail: 'KPIs, exports, and leadership views that show the programme is under control—and where to act next.',
    },
  ];

  readonly intelHighlights: ReadonlyArray<{
    title: string;
    body: string;
    image: string;
    imageAlt: string;
  }> = [
    {
      title: 'Exception radar',
      body: 'Catch the whispers before they become headlines—delays, tension points, and hero opportunities.',
      image: px(PEX.routePlanning, 720, 480),
      imageAlt: 'Route map and corridor planning for supply chain exceptions',
    },
    {
      title: 'Network analytics',
      body: 'Blend movement, margin, and momentum into boardroom-ready stories.',
      image: px(PEX.distributionCenter, 720, 480),
      imageAlt: 'Distribution centre overview for network-wide logistics KPIs',
    },
    {
      title: 'Operational search',
      body: 'Jump from spark to resolution with breadcrumbs everyone recognises.',
      image: px(PEX.inventoryScan, 720, 480),
      imageAlt: 'Warehouse supervisor scanning inventory on the distribution floor',
    },
  ];

  readonly kpis = [
    {
      value: '12',
      unit: 'phases',
      label: 'Implementation arc',
      hint: 'From first setup through go-live, settlements, and steady-state KPIs—mapped below.',
    },
    { value: '7+', unit: 'roles', label: 'Cast of characters', hint: 'Each player sees the lines they need.' },
    { value: '2', unit: 'portals', label: 'Dual stages', hint: 'Operators shine while stewards keep balance.' },
    { value: '24/7', unit: 'pulse', label: 'Always-on signals', hint: 'Momentum you can feel around the clock.' },
  ] as const;

  readonly automationPoints = [
    'Nudges when exceptions need attention—without burying teams in noise',
    'Escalations that carry order, trip, and party context to the right owner',
    'Reminders that read like tasks with owners, not anonymous alarms',
    'Exports and handoffs formatted so partners and finance actually adopt them',
  ] as const;

  readonly moreFeatures: ReadonlyArray<{
    title: string;
    detail: string;
    pitch: string;
    image: string;
    imageAlt: string;
  }> = [
    {
      title: 'Roles & permissions',
      detail: 'Sculpt access like stage lighting—bright where needed, soft elsewhere.',
      pitch: 'Give each persona a spotlight without stealing the show from anyone else.',
      image: px(PEX.warehouseAisle, 640, 720),
      imageAlt: 'Modern warehouse aisles in a distribution facility',
    },
    {
      title: 'Beautiful history',
      detail: 'Chronicles that read like a highlight reel—not a forensic dump.',
      pitch: 'Let every decisive moment leave an elegant footprint you are proud to share.',
      image: px(PEX.warehouseScanning, 640, 720),
      imageAlt: 'Scanning and traceability on the warehouse floor',
    },
    {
      title: 'Geospatial canvas',
      detail: 'Maps that breathe with your operation—alive, layered, and legible.',
      pitch: 'Anchor intuition to geography so nobody argues about where reality lives.',
      image: px(PEX.highwayInterchange, 640, 720),
      imageAlt: 'Aerial highway interchange for freight routing and corridors',
    },
    {
      title: 'Conversation glue',
      detail: 'Threads that cling to the work—not the void of a generic inbox.',
      pitch: 'Keep context cuddled close to orders, trips, and promises.',
      image: px(PEX.yardForklifts, 640, 720),
      imageAlt: 'Forklifts coordinating inbound and outbound yard movements',
    },
    {
      title: 'Commercial poetry',
      detail: 'Credit stories that still feel crisp when spreadsheets come calling.',
      pitch: 'Celebrate flexibility without letting discipline drift out of frame.',
      image: px(PEX.shippingPort, 640, 720),
      imageAlt: 'Port logistics and cargo vessels in the supply chain',
    },
    {
      title: 'Partner constellations',
      detail: 'Surface the helpers crews crave—fuel, fix, or fortify—exactly when stars align.',
      pitch: 'Make roadside magic discoverable without breaking the narrative flow.',
      image: px(PEX.semiTruck, 640, 720),
      imageAlt: 'Semi-trailer truck on a long-distance distribution lane',
    },
    {
      title: 'Regulated vistas',
      detail: 'Views tuned for partners who need clarity without clutter.',
      pitch: 'Invite governance into the story as a respected guest—not a gatecrasher.',
      image: px(PEX.containersAerial, 640, 720),
      imageAlt: 'Aerial container terminal for regulated freight oversight',
    },
    {
      title: 'Story-ready exports',
      detail: 'Spreadsheets and decks that feel designed—not dumped.',
      pitch: 'Arm analysts with artefacts they actually want to present upstairs.',
      image: px(PEX.logisticsDashboard, 640, 720),
      imageAlt: 'Logistics dashboards prepared for executive supply chain reporting',
    },
  ];

  readonly onboardingSteps: ReadonlyArray<{ title: string; detail: string; image: string; imageAlt: string }> = [
    {
      title: 'Compose your debut',
      detail: 'Introduce your organisation with the same polish you show customers at the door.',
      image: px(PEX.warehouseInterior, 720, 480),
      imageAlt: 'Distribution warehouse interior ready for platform go-live',
    },
    {
      title: 'Fill the canvas',
      detail: 'Bring master data in like brush strokes—deliberate, vibrant, ready for motion.',
      image: px(PEX.inventoryScan, 720, 480),
      imageAlt: 'Inventory scanning and master data capture in the warehouse',
    },
    {
      title: 'Premiere a corridor',
      detail: 'Launch a pilot that feels exclusive, learn fast, then widen the aperture with swagger.',
      image: px(PEX.truckHighway, 720, 480),
      imageAlt: 'Freight trucks launching a pilot distribution corridor',
    },
  ];

  readonly notificationChannels = [
    'Moments that feel hand-written even when they scale',
    'Cadence that mirrors how trips breathe in the real world',
    'Harmony across suppliers, partners, and the people who steer trust',
  ] as const;

  readonly impactPoints = [
    { title: 'Time returned', detail: 'Fewer detours chasing ghosts in inboxes or voicemails.' },
    { title: 'Trust amplified', detail: 'Everyone reads from the same beautifully lit script.' },
    { title: 'Safety elevated', detail: 'Visibility that feels protective—not performative.' },
    { title: 'Foresight sharpened', detail: 'Signals arrive while you still have room to choreograph.' },
    { title: 'Margin protected', detail: 'Friction fades; the expensive surprises shrink.' },
  ] as const;

  readonly accessPortalCardImage = px(PEX.warehousePallets, 800, 520);
  readonly accessAdminCardImage = px(PEX.distributionCenter, 800, 520);

  readonly faqs = [
    {
      q: 'Where do our teams begin?',
      a: 'Most corridors start in the organisation portal—the daily stage for suppliers, partners, and operators. LX stewards use a dedicated console to keep the ecosystem balanced without slowing innovators.',
    },
    {
      q: 'Is LDMS only for one type of cargo?',
      a: 'LDMS is crafted for ambitious road programmes—bulk, packaged, or specialised—especially when geography adds drama. The story scales with you.',
    },
    {
      q: 'How do customers feel the magic at delivery?',
      a: 'Handovers become rituals: confident confirmations, graceful notes, and mutual respect baked into the experience.',
    },
    {
      q: 'Can our commercial imagination breathe?',
      a: 'Yes. LDMS embraces multiple commercial rhythms so finance and sales can dance together without stepping on toes.',
    },
    {
      q: 'What about the serious side of compliance?',
      a: 'Compliance is staged like premium theatre—visible, rehearsed, and worthy of the spotlight when regulators lean in.',
    },
  ] as const;

  /** Avoid nav class thrashing at the scroll threshold (reduces sticky-bar flicker). */
  private navScrolled = false;
  private navScrollRaf = 0;
  private scrollBarEl: HTMLElement | null = null;
  private readonly onWindowScroll = (): void => {
    if (this.navScrollRaf) {
      return;
    }
    this.navScrollRaf = requestAnimationFrame(() => {
      this.navScrollRaf = 0;
      this.updateScrollChrome();
    });
  };

  /** Passive window scroll: nav surface + top progress bar (single rAF path). */
  private updateScrollChrome(): void {
    const y = window.scrollY || document.documentElement.scrollTop || 0;
    const shell = this.el.nativeElement.querySelector('.ldms-landing__nav-shell');
    const nextNavScrolled = this.navScrolled ? y > 48 : y > 96;
    if (shell && nextNavScrolled !== this.navScrolled) {
      this.navScrolled = nextNavScrolled;
      shell.classList.toggle('ldms-landing__nav-shell--scrolled', nextNavScrolled);
    }

    if (this.scrollBarEl) {
      const max = Math.max(document.documentElement.scrollHeight - window.innerHeight, 1);
      this.scrollBarEl.style.transform = `scaleX(${Math.min(Math.max(y / max, 0), 1)})`;
    }
  }

  constructor(
    private readonly router: Router,
    readonly theme: ThemeService,
    private readonly el: ElementRef<HTMLElement>,
    private readonly ngZone: NgZone,
  ) {}

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      this.initScrollBar();
      this.initHeroReveal();
      this.initSectionReveals();
      this.initCounters();
      this.initNavScrollState();
      this.initCardTilt();
    });
  }

  ngOnDestroy(): void {
    window.removeEventListener('scroll', this.onWindowScroll);
    if (this.navScrollRaf) {
      cancelAnimationFrame(this.navScrollRaf);
    }
  }

  private initScrollBar(): void {
    this.scrollBarEl = this.el.nativeElement.querySelector('.ldms-scroll-bar');
  }

  private initHeroReveal(): void {
    const words = this.el.nativeElement.querySelectorAll('.ldms-word');
    if (words.length) {
      gsap.fromTo(
        words,
        { y: 52, opacity: 0 },
        { y: 0, opacity: 1, duration: 0.85, stagger: 0.07, ease: 'power3.out', delay: 0.25 },
      );
    }
    const heroSubs = this.el.nativeElement.querySelectorAll(
      '.ldms-landing__hero-topline, .ldms-landing__lead, .ldms-landing__hero-cta, .ldms-landing__chips, .ldms-landing__stats',
    );
    if (heroSubs.length) {
      gsap.fromTo(
        heroSubs,
        { y: 24, opacity: 0 },
        { y: 0, opacity: 1, duration: 0.7, stagger: 0.09, ease: 'power2.out', delay: 0.8 },
      );
    }
  }

  private initSectionReveals(): void {
    const reveals = this.el.nativeElement.querySelectorAll('.ldms-reveal');
    const revealIn = (target: Element) => {
      target.classList.add('ldms-reveal--in');
    };
    const io = new IntersectionObserver(
      (entries) => {
        requestAnimationFrame(() => {
          entries.forEach((e) => {
            if (e.isIntersecting) {
              revealIn(e.target);
              io.unobserve(e.target);
            }
          });
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -8% 0px' },
    );

    reveals.forEach((element: Element) => {
      const rect = element.getBoundingClientRect();
      const vh = window.innerHeight || document.documentElement.clientHeight || 0;
      const bleed = vh * 0.06;
      if (rect.top < vh - bleed && rect.bottom > bleed) {
        revealIn(element);
      } else {
        io.observe(element);
      }
    });
  }

  private initCounters(): void {
    const counters = this.el.nativeElement.querySelectorAll('.ldms-landing__kpi-num[data-count]');
    if (!counters.length) return;
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
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
          }
        });
      },
      { threshold: 0.5 },
    );
    counters.forEach((element: Element) => io.observe(element));
  }

  private initNavScrollState(): void {
    this.updateScrollChrome();
    window.addEventListener('scroll', this.onWindowScroll, { passive: true });
  }

  private initCardTilt(): void {
    const cards = this.el.nativeElement.querySelectorAll('.ldms-landing__intel-card--tilt');
    cards.forEach((card) => {
      const el = card as HTMLElement;
      el.addEventListener('mousemove', (ev: MouseEvent) => {
        const rect = el.getBoundingClientRect();
        const x = (ev.clientX - rect.left) / rect.width - 0.5;
        const y = (ev.clientY - rect.top) / rect.height - 0.5;
        gsap.to(el, { rotateX: -y * 10, rotateY: x * 10, duration: 0.4, ease: 'power2.out' });
      });
      el.addEventListener('mouseleave', () => {
        gsap.to(el, { rotateX: 0, rotateY: 0, duration: 0.6, ease: 'power2.out' });
      });
    });
  }

  purposeFlipped(i: number): boolean {
    return this.flippedPurpose().has(i);
  }

  togglePurposeFlip(i: number): void {
    this.flippedPurpose.update((prev) => {
      const next = new Set(prev);
      if (next.has(i)) {
        next.delete(i);
      } else {
        next.add(i);
      }
      return next;
    });
  }

  moreFlipped(i: number): boolean {
    return this.flippedMore().has(i);
  }

  toggleMoreFlip(i: number): void {
    this.flippedMore.update((prev) => {
      const next = new Set(prev);
      if (next.has(i)) {
        next.delete(i);
      } else {
        next.add(i);
      }
      return next;
    });
  }

  toggleTheme(): void {
    this.theme.toggle();
  }

  toggleMobileNav(): void {
    this.mobileNavOpen.update((open) => !open);
  }

  closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }

  goSignup(): void {
    void this.router.navigate(['/signup']);
  }

  goPlatformLogin(): void {
    void this.router.navigate(['/auth/login']);
  }

  goBookDemo(): void {
    void this.router.navigate(['/contact']);
  }
}
