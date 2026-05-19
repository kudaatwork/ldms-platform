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
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { environment } from '../../../../../environments/environment';
import { ThemeService } from '../../../../core/services/theme.service';

gsap.registerPlugin(ScrollTrigger);

/** Pexels CDN — logistics & transport photography (pexels.com licence). */
const px = (id: number, w = 800, h = 1000) =>
  `https://images.pexels.com/photos/${id}/pexels-photo-${id}.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=${w}&h=${h}&fit=crop`;

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

  readonly showcaseVideoSrc =
    'https://videos.pexels.com/video-files/857195/857195-hd_1920_1080_30fps.mp4';
  readonly showcaseVideoPoster = px(1562324, 1200, 675);

  /** Hero + section art — explicit Pexels logistics imagery (not bundled assets). */
  readonly heroImage = px(1562324, 1600, 1067);

  /** Logistics photography (Pexels) — intro & automation side art. */
  readonly introImage = px(3635874, 960, 1200);
  readonly automationImage = px(974314, 960, 960);

  /** Split “Notifications / Impact” panels. */
  readonly splitNotifyImage = px(5025514, 960, 640);
  readonly splitImpactImage = px(4484079, 960, 640);

  /** Journey, KPI band, FAQ, CTA — corridor visuals. */
  readonly journeyImage = px(267583, 900, 1100);
  readonly faqAsideImage = px(4483616, 900, 1100);
  readonly ctaBandImage = px(3184338, 1200, 800);

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
      image: px(3184292, 800, 1000),
      imageAlt: 'Commercial partners collaborating over logistics plans',
    },
    {
      title: 'Confidence',
      accent: 'on paper',
      tagline: 'Compliance becomes a quiet superpower—not a Sunday scramble.',
      story:
        'Turn filings, permits, and approvals into a polished narrative partners trust. LDMS keeps the backstage tidy so your front stage shines.',
      image: px(4483616, 800, 1000),
      imageAlt: 'Stacked shipping containers representing compliant cargo documentation',
    },
    {
      title: 'Fleet',
      accent: 'in flow',
      tagline: 'Capacity that feels orchestrated, not improvised.',
      story:
        'Blend crews, assets, and timing into one graceful motion. The platform celebrates your operators while protecting the standards you cannot bend.',
      image: px(5025514, 800, 1000),
      imageAlt: 'Warehouse logistics and cargo handling',
    },
    {
      title: 'Live',
      accent: 'perspective',
      tagline: 'Everyone watches the same horizon—not their own guesswork.',
      story:
        'Bring near-real awareness to every stakeholder without drowning them in noise. Calm maps, crisp moments, and human-readable progress.',
      image: px(3861969, 800, 1000),
      imageAlt: 'GPS and route planning on a digital map for fleet tracking',
    },
    {
      title: 'Together',
      accent: 'on mission',
      tagline: 'Agents, yards, and HQ finally speak the same language.',
      story:
        'Break silos with shared rituals: quick huddles, transparent handoffs, and celebrations when milestones land—because logistics is a team sport.',
      image: px(6169862, 800, 1000),
      imageAlt: 'Dispatch team monitoring freight operations on screens',
    },
    {
      title: 'Capital',
      accent: 'with clarity',
      tagline: 'Money moves with the story—not against it.',
      story:
        'Match commercial creativity with guardrails finance loves. Flexible rhythms for how you collect, release, and reconcile—without losing the plot.',
      image: px(3635874, 800, 1000),
      imageAlt: 'Border and customs documentation alongside freight paperwork',
    },
  ];

  readonly personas: ReadonlyArray<{ role: string; detail: string; image: string; imageAlt: string }> = [
    {
      role: 'Suppliers',
      detail: 'Shape demand, orchestrate promises, and look heroic to buyers.',
      image: px(5025514, 640, 480),
      imageAlt: 'Warehouse pallets staged for outbound supplier programmes',
    },
    {
      role: 'Customers',
      detail: 'Know what is coming, when it lands, and who stands behind it.',
      image: px(4483616, 640, 480),
      imageAlt: 'Intermodal containers ready for customer delivery windows',
    },
    {
      role: 'Drivers',
      detail: 'Stay focused on the road with guidance that respects their day.',
      image: px(4484079, 640, 480),
      imageAlt: 'Commercial truck on the open road',
    },
    {
      role: 'Transport partners',
      detail: 'Show capacity as craft—not just commodity tonnage.',
      image: px(267583, 640, 480),
      imageAlt: 'Aerial view of highway freight corridors',
    },
    {
      role: 'Clearing allies',
      detail: 'Turn complexity into choreography everyone can follow.',
      image: px(3635874, 640, 480),
      imageAlt: 'Documentation and cargo checks at a logistics checkpoint',
    },
    {
      role: 'Roadside partners',
      detail: 'Be discoverable exactly when crews need you most.',
      image: px(974314, 640, 480),
      imageAlt: 'Service yard supporting trucks and roadside operations',
    },
    {
      role: 'Platform stewards',
      detail: 'Keep the ecosystem trustworthy without slowing innovators.',
      image: px(6169862, 640, 480),
      imageAlt: 'Control room monitoring network-wide logistics performance',
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
      image: px(6169862, 900, 1100),
      imageAlt: 'Operations team reviewing logistics data',
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
      image: px(5025514, 900, 1100),
      imageAlt: 'Warehouse floor in motion with pallets and forklifts',
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
      image: px(4483616, 900, 1100),
      imageAlt: 'Shipping containers and logistics yard',
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
      image: px(6169862, 900, 1100),
      imageAlt: 'Operations desk reviewing freight and settlement workflows',
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
      image: px(3861969, 720, 480),
      imageAlt: 'Fleet map and exception alerts on a logistics dashboard',
    },
    {
      title: 'Network analytics',
      body: 'Blend movement, margin, and momentum into boardroom-ready stories.',
      image: px(6169862, 720, 480),
      imageAlt: 'Operations analysts reviewing corridor KPIs',
    },
    {
      title: 'Operational search',
      body: 'Jump from spark to resolution with breadcrumbs everyone recognises.',
      image: px(5025514, 720, 480),
      imageAlt: 'Supervisor searching shipment records on a warehouse floor',
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
      image: px(5025514, 640, 720),
      imageAlt: 'Forklift and pallets inside a distribution warehouse',
    },
    {
      title: 'Beautiful history',
      detail: 'Chronicles that read like a highlight reel—not a forensic dump.',
      pitch: 'Let every decisive moment leave an elegant footprint you are proud to share.',
      image: px(6169862, 640, 720),
      imageAlt: 'Logistics control room with shipment dashboards',
    },
    {
      title: 'Geospatial canvas',
      detail: 'Maps that breathe with your operation—alive, layered, and legible.',
      pitch: 'Anchor intuition to geography so nobody argues about where reality lives.',
      image: px(267583, 640, 720),
      imageAlt: 'Aerial view of highway interchange',
    },
    {
      title: 'Conversation glue',
      detail: 'Threads that cling to the work—not the void of a generic inbox.',
      pitch: 'Keep context cuddled close to orders, trips, and promises.',
      image: px(3184292, 640, 720),
      imageAlt: 'Operations team collaborating around shipment context',
    },
    {
      title: 'Commercial poetry',
      detail: 'Credit stories that still feel crisp when spreadsheets come calling.',
      pitch: 'Celebrate flexibility without letting discipline drift out of frame.',
      image: px(4483616, 640, 720),
      imageAlt: 'Container terminal and cargo handling equipment',
    },
    {
      title: 'Partner constellations',
      detail: 'Surface the helpers crews crave—fuel, fix, or fortify—exactly when stars align.',
      pitch: 'Make roadside magic discoverable without breaking the narrative flow.',
      image: px(4484079, 640, 720),
      imageAlt: 'Commercial truck on the road for long-haul delivery',
    },
    {
      title: 'Regulated vistas',
      detail: 'Views tuned for partners who need clarity without clutter.',
      pitch: 'Invite governance into the story as a respected guest—not a gatecrasher.',
      image: px(3184338, 640, 720),
      imageAlt: 'Compliance and operations review with logistics documentation',
    },
    {
      title: 'Story-ready exports',
      detail: 'Spreadsheets and decks that feel designed—not dumped.',
      pitch: 'Arm analysts with artefacts they actually want to present upstairs.',
      image: px(6169862, 640, 720),
      imageAlt: 'Operations analyst reviewing logistics KPI charts',
    },
  ];

  readonly onboardingSteps: ReadonlyArray<{ title: string; detail: string; image: string; imageAlt: string }> = [
    {
      title: 'Compose your debut',
      detail: 'Introduce your organisation with the same polish you show customers at the door.',
      image: px(5025514, 720, 480),
      imageAlt: 'Distribution centre ready for go-live configuration',
    },
    {
      title: 'Fill the canvas',
      detail: 'Bring master data in like brush strokes—deliberate, vibrant, ready for motion.',
      image: px(6169862, 720, 480),
      imageAlt: 'Team loading master data and routes into the platform',
    },
    {
      title: 'Premiere a corridor',
      detail: 'Launch a pilot that feels exclusive, learn fast, then widen the aperture with swagger.',
      image: px(4484079, 720, 480),
      imageAlt: 'Convoy of trucks launching a pilot corridor',
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

  readonly accessPortalCardImage = px(5025514, 800, 520);
  readonly accessAdminCardImage = px(6169862, 800, 520);

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

  /** Passive window scroll: toggles sticky nav surface. Removed in ngOnDestroy. */
  private readonly onWindowScrollForNav = (): void => {
    const nav = this.el.nativeElement.querySelector('.ldms-landing__nav');
    if (!nav) return;
    const y = window.scrollY || document.documentElement.scrollTop || 0;
    nav.classList.toggle('ldms-landing__nav--scrolled', y > 72);
  };

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
      this.initParallax();
      this.initCounters();
      this.initNavScrollState();
      this.initCardTilt();
    });
  }

  ngOnDestroy(): void {
    ScrollTrigger.getAll().forEach((t) => t.kill());
    window.removeEventListener('scroll', this.onWindowScrollForNav);
  }

  private initScrollBar(): void {
    const bar = this.el.nativeElement.querySelector('.ldms-scroll-bar');
    if (!bar) return;
    gsap.to(bar, {
      scaleX: 1,
      ease: 'none',
      scrollTrigger: { start: 'top top', end: 'bottom bottom', scrub: 0 },
    });
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
      '.ldms-landing__eyebrow, .ldms-landing__lead, .ldms-landing__hero-cta, .ldms-landing__chips, .ldms-landing__stats, .ldms-landing__meta',
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
        entries.forEach((e) => {
          if (e.isIntersecting) {
            revealIn(e.target);
            io.unobserve(e.target);
          }
        });
      },
      { threshold: 0.08, rootMargin: '0px 0px -36px 0px' },
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

  private initParallax(): void {
    const hero = this.el.nativeElement.querySelector('.ldms-landing__hero');
    const heroImg = this.el.nativeElement.querySelector('.ldms-landing__hero-img');
    if (heroImg && hero) {
      gsap.to(heroImg, {
        yPercent: -12,
        ease: 'none',
        scrollTrigger: {
          trigger: hero,
          start: 'top top',
          end: 'bottom top',
          scrub: true,
        },
      });
    }
    const divePhotos = this.el.nativeElement.querySelectorAll('.ldms-landing__dive-photo');
    divePhotos.forEach((img: Element) => {
      const section = img.closest('.ldms-landing__dive');
      if (!section) return;
      gsap.to(img, {
        yPercent: -8,
        ease: 'none',
        scrollTrigger: {
          trigger: section,
          start: 'top bottom',
          end: 'bottom top',
          scrub: true,
        },
      });
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
    this.onWindowScrollForNav();
    window.addEventListener('scroll', this.onWindowScrollForNav, { passive: true });
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
