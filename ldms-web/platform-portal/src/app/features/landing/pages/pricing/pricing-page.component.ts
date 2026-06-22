import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  NgZone,
  OnInit,
  ViewEncapsulation,
  computed,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  PlatformWalletService,
  type PlatformActionChargeRow,
  type SubscriptionPackageRow,
} from '../../../../core/services/platform-wallet.service';
import {
  PLATFORM_BILLING_MODULES,
  moduleLabel,
} from '../../../settings/utils/platform-billing-modules.util';
import {
  PLATFORM_BILLING_TIERS,
  chargesForTier,
  type PlatformBillingTierCode,
} from '../../../../shared/utils/platform-billing-tiers.util';
import {
  DEFAULT_SUBSCRIPTION_PACKAGE_FEATURES,
  packageFeaturePoints,
} from '../../../../shared/utils/subscription-package-description.util';
import { LandingMotionService } from '../../services/landing-motion.service';

@Component({
  selector: 'app-pricing-page',
  templateUrl: './pricing-page.component.html',
  styleUrls: ['./pricing-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  standalone: false,
})
export class PricingPageComponent implements OnInit, AfterViewInit {
  readonly pricingMode = signal<'prepaid' | 'subscription'>('prepaid');
  readonly pricingPackages = signal<SubscriptionPackageRow[]>([]);
  readonly pricingCharges = signal<PlatformActionChargeRow[]>([]);
  readonly pricingLoading = signal(true);
  readonly pricingLoadFailed = signal(false);
  readonly expandedModule = signal<string | null>(null);
  readonly pricingModules = PLATFORM_BILLING_MODULES;
  readonly moduleLabel = moduleLabel;
  readonly defaultPackageFeatures = DEFAULT_SUBSCRIPTION_PACKAGE_FEATURES;
  readonly billingTiers = PLATFORM_BILLING_TIERS;

  readonly prepaidDemoEvents = [
    { icon: 'account_balance_wallet', label: 'Wallet top-up', cents: 50000, credit: true },
    { icon: 'local_shipping', label: 'Completed delivery (Heavy)', cents: 45, credit: false },
    { icon: 'share_location', label: 'Live tracking day', cents: 20, credit: false },
    { icon: 'sms', label: 'SMS alert sent', cents: 7, credit: false },
    { icon: 'rule', label: 'Workflow approval (Light)', cents: 5, credit: false },
  ] as const;

  readonly pricingChargesByModule = computed(() => {
    const grouped = new Map<string, PlatformActionChargeRow[]>();
    for (const charge of this.pricingCharges()) {
      const category = charge.category ?? 'GENERAL';
      const list = grouped.get(category) ?? [];
      list.push(charge);
      grouped.set(category, list);
    }
    return this.pricingModules
      .map((mod) => ({ mod, charges: grouped.get(mod.category) ?? [] }))
      .filter((entry) => entry.charges.length > 0);
  });

  readonly totalChargeCount = computed(() => this.pricingCharges().length);

  tierCharges(tier: PlatformBillingTierCode): PlatformActionChargeRow[] {
    return chargesForTier(this.pricingCharges(), tier);
  }

  packageCreditSummary(pkg: SubscriptionPackageRow): string | null {
    const parts: string[] = [];
    if (pkg.includedHeavyCredits) {
      parts.push(`${pkg.includedHeavyCredits} Heavy`);
    }
    if (pkg.includedStandardCredits) {
      parts.push(`${pkg.includedStandardCredits} Standard`);
    }
    if (pkg.includedLightCredits) {
      parts.push(`${pkg.includedLightCredits} Light`);
    }
    if (pkg.includedTrackingDayCredits) {
      parts.push(`${pkg.includedTrackingDayCredits} tracking-day`);
    }
    return parts.length ? parts.join(' · ') : null;
  }

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly wallet: PlatformWalletService,
    private readonly cdr: ChangeDetectorRef,
    private readonly el: ElementRef<HTMLElement>,
    private readonly ngZone: NgZone,
    private readonly motion: LandingMotionService,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const tab = params.get('tab');
      if (tab === 'subscription') {
        this.pricingMode.set('subscription');
      } else if (tab === 'prepaid' || tab === 'wallet') {
        this.pricingMode.set('prepaid');
      }
      const module = params.get('module');
      if (module) {
        this.expandedModule.set(module.toUpperCase());
      }
    });

    this.wallet.getPublicPricingCatalog().subscribe({
      next: ({ packages, actionCharges }) => {
        this.pricingPackages.set(
          [...packages].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name)),
        );
        this.pricingCharges.set(actionCharges);
        this.pricingLoading.set(false);
        this.pricingLoadFailed.set(false);
        if (packages.length && !actionCharges.length) {
          this.pricingMode.set('subscription');
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.pricingLoading.set(false);
        this.pricingLoadFailed.set(true);
        this.cdr.markForCheck();
      },
    });
  }

  ngAfterViewInit(): void {
    this.motion.initHeroReveal(this.el.nativeElement);
    if (this.pricingMode() === 'prepaid') {
      this.motion.initPrepaidDemoMotion(this.el.nativeElement);
    }
  }

  formatPrice(cents: number, currency = 'USD'): string {
    const amount = (cents ?? 0) / 100;
    try {
      return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount);
    } catch {
      return `${currency} ${amount.toFixed(2)}`;
    }
  }

  packageFeatures(description?: string | null): readonly string[] {
    const points = packageFeaturePoints(description);
    return points.length ? points : this.defaultPackageFeatures;
  }

  setPricingMode(mode: 'prepaid' | 'subscription'): void {
    this.pricingMode.set(mode);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: mode === 'prepaid' ? 'wallet' : 'subscription', module: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
    if (mode === 'prepaid') {
      this.ngZone.runOutsideAngular(() => {
        requestAnimationFrame(() => this.motion.initPrepaidDemoMotion(this.el.nativeElement));
      });
    }
  }

  toggleModule(category: string): void {
    const next = this.expandedModule() === category ? null : category;
    this.expandedModule.set(next);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { module: next },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  isModuleExpanded(category: string): boolean {
    return this.expandedModule() === category;
  }

  goSignup(): void {
    void this.router.navigate(['/signup']);
  }

  goBookDemo(): void {
    void this.router.navigate(['/contact']);
  }
}
