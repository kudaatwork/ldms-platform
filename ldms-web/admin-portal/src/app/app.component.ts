import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  HostListener,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter, finalize, takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { CurrentUserService, ShellUserView } from './core/services/current-user.service';
import { NavAccessService } from './core/services/nav-access.service';
import { KycNotificationDismissService } from './core/services/kyc-notification-dismiss.service';
import { KycQueueStatsService } from './core/services/kyc-queue-stats.service';
import { PendingDepositsStatsService } from './core/services/pending-deposits-stats.service';
import { LiveChatStatsService } from './core/services/live-chat-stats.service';
import type { AdminSupportTicket } from './features/help/services/help-support-admin.service';
import {
  PlatformWalletAdminService,
  type WalletDepositRow,
} from './features/settings/services/platform-wallet-admin.service';
import { StorageService } from './core/services/storage.service';
import type { KycQueueSummary } from './features/organizations/services/organizations-admin.service';
import { ThemeService } from './core/services/theme.service';
import { ORG_CLASSIFICATIONS } from './shared/models/org-classifications';
import { isStoredSessionToken } from './core/utils/jwt.util';
import { PhoneVerificationPromptService } from './core/services/phone-verification-prompt.service';
import { NetworkConnectivityService } from './core/services/network-connectivity.service';
import { OfflinePageComponent } from './shared/components/offline-page/offline-page.component';

/** Leaf link or non-clickable section label inside a nav subsection. */
type NavSubEntry =
  | { type?: 'link'; label: string; icon: string; route: string }
  | { type: 'heading'; label: string };

interface NavChild {
  label: string;
  icon: string;
  route: string;
  /** Nested items under this subsection (e.g. All organisations → classifications only). */
  children?: NavSubEntry[];
}

/** Top-level item under an expandable nav group (link subsection or section title). */
type NavGroupEntry = NavChild | { type: 'heading'; label: string };

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number | null;
  children?: NavGroupEntry[];
}

interface Breadcrumb {
  label: string;
  url: string;
}

interface TopNotification {
  id: string;
  title: string;
  body: string;
  time: string;
  /** When set, clicking the notification navigates here. */
  route?: string[];
  queryParams?: Record<string, string | number>;
}

function classificationNavIcon(slug: string): string {
  switch (slug) {
    case 'SUPPLIER':
      return 'inventory_2';
    case 'CUSTOMER':
      return 'storefront';
    case 'TRANSPORT_COMPANY':
      return 'local_shipping';
    case 'CLEARING_AGENT':
      return 'fact_check';
    case 'SERVICE_STATION':
      return 'local_gas_station';
    case 'ROADSIDE_SUPPORT_SERVICE':
      return 'support_agent';
    case 'GOVERNMENT_AGENCY':
      return 'account_balance';
    default:
      return 'layers';
  }
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly idleTimeoutMs = 5 * 60 * 1000;
  private readonly idleWarningMs = 2 * 60 * 1000;
  private readonly activityEvents: Array<keyof DocumentEventMap> = [
    'mousemove',
    'mousedown',
    'keydown',
    'touchstart',
    'scroll',
  ];
  private idleWarningTimerId: ReturnType<typeof setTimeout> | null = null;
  private idleLogoutTimerId: ReturnType<typeof setTimeout> | null = null;
  private idleCountdownTimerId: ReturnType<typeof setInterval> | null = null;
  private warningDeadlineMs = 0;
  private lastActivityResetMs = 0;
  private static readonly ACTIVITY_RESET_COOLDOWN_MS = 1_000;
  private readonly onActivityBound = () => this.onUserActivity();

  collapsed = false;
  pageTitle = 'Dashboard';
  showShell = true;
  private sessionProfileHydrated = false;
  breadcrumbs: Breadcrumb[] = [];

  /** Expanded state per sidebar group route (e.g. /locations, /activity). */
  readonly expandedGroups = signal<Record<string, boolean>>({});
  /** Groups the user explicitly collapsed — sync must not re-expand these. */
  private userCollapsedNavGroups = new Set<string>();
  /** Ensures sidebar groups start collapsed once per authenticated shell session. */
  private shellNavExpandedInitialized = false;

  /** Snapshot for templates (e.g. sidebar active states). */
  currentUrl = '';

  visibleNavItems: NavItem[] = [];
  showNavRoleHint = false;
  private kycStatsLoaded = false;
  private pendingDepositsLoaded = false;
  private kycSummary: KycQueueSummary | null = null;
  private pendingDeposits: WalletDepositRow[] = [];
  private liveChatTickets: AdminSupportTicket[] = [];
  private liveChatLoaded = false;

  /** Minimal menu when RBAC roles are not loaded yet or the user group has none assigned. */
  private readonly bootstrapNavItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    {
      label: 'Users',
      icon: 'people_outline',
      route: '/users',
      children: [
        { label: 'All users', icon: 'person_search', route: '/users' },
        { label: 'User groups', icon: 'groups', route: '/users/groups' },
        { label: 'User roles', icon: 'verified_user', route: '/users/roles' },
      ],
    },
  ];

  private readonly allNavItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'KYC Queue', icon: 'verified_user', route: '/kyc/applications' },
    {
      label: 'Organizations',
      icon: 'corporate_fare',
      route: '/organizations',
      children: [
        {
          label: 'All organisations',
          icon: 'corporate_fare',
          route: '/organizations',
          children: ORG_CLASSIFICATIONS.map((c) => ({
            type: 'link' as const,
            label: c.label,
            icon: classificationNavIcon(c.slug),
            route: `/organizations/classification/${c.slug}`,
          })),
        },
        { type: 'heading', label: 'Branch sectors' },
        { label: 'Branches', icon: 'account_tree', route: '/organizations/branches' },
        { label: 'Agents', icon: 'badge', route: '/organizations/agents' },
        { type: 'heading', label: 'Industry sectors' },
        { label: 'Industries', icon: 'factory', route: '/organizations/industries' },
      ],
    },
    { label: 'Documents', icon: 'folder_open', route: '/kyc/documents' },
    {
      label: 'Users',
      icon: 'people_outline',
      route: '/users',
      children: [
        { label: 'All users', icon: 'person_search', route: '/users' },
        { label: 'User groups', icon: 'groups', route: '/users/groups' },
        { label: 'User roles', icon: 'verified_user', route: '/users/roles' },
        { label: 'User types', icon: 'category', route: '/users/types' },
      ],
    },
    {
      label: 'Locations',
      icon: 'location_on',
      route: '/locations',
      children: [
        { label: 'Hierarchy explorer', icon: 'schema', route: '/locations/explorer' },
        { label: 'Countries', icon: 'public', route: '/locations/countries' },
        {
          label: 'Administrative levels',
          icon: 'account_tree',
          route: '/locations/admin-levels',
        },
        { label: 'Provinces', icon: 'map', route: '/locations/provinces' },
        { label: 'Districts', icon: 'holiday_village', route: '/locations/districts' },
        { label: 'Cities', icon: 'location_city', route: '/locations/cities' },
        { label: 'Suburbs', icon: 'villa', route: '/locations/suburbs' },
        { label: 'Villages', icon: 'cabin', route: '/locations/villages' },
        { label: 'Addresses', icon: 'pin_drop', route: '/locations/addresses' },
        { label: 'Languages', icon: 'translate', route: '/locations/languages' },
        { label: 'Localized names', icon: 'g_translate', route: '/locations/localized-names' },
      ],
    },
    { label: 'Notifications', icon: 'notifications', route: '/notifications' },
    {
      label: 'Analytics',
      icon: 'analytics',
      route: '/analytics',
      children: [
        { label: 'Shipment analytics', icon: 'local_shipping', route: '/analytics' },
        { label: 'Platform revenue', icon: 'payments', route: '/analytics/revenue' },
      ],
    },
    {
      label: 'Help & Support',
      icon: 'help_outline',
      route: '/help',
      children: [
        { label: 'Live chat', icon: 'forum', route: '/help/live-chat' },
        { label: 'Demo requisitions', icon: 'event_available', route: '/help/requisitions' },
        { label: 'Bot service', icon: 'smart_toy', route: '/help/bot-service' },
        { label: 'Bot analytics', icon: 'insights', route: '/help/bot-analytics' },
        { label: 'Bot knowledge', icon: 'menu_book', route: '/help/bot-knowledge' },
      ],
    },
    {
      label: 'Audit Log',
      icon: 'receipt_long',
      route: '/activity',
      children: [
        { label: 'Request logs', icon: 'receipt', route: '/activity/request-logs' },
        { label: 'Login & activity', icon: 'history', route: '/activity/activity-logs' },
        { label: 'Churnout history', icon: 'restore', route: '/activity/churnout-history' },
      ],
    },
    { label: 'System Health', icon: 'monitor_heart', route: '/system/health' },
  ];

  shellUser: ShellUserView | null = null;

  notificationsOpen = false;
  profileOpen = false;
  topNotifications: TopNotification[] = [];
  sessionWarningVisible = false;
  sessionSecondsRemaining = 0;

  constructor(
    readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly storage: StorageService,
    private readonly currentUser: CurrentUserService,
    readonly navAccess: NavAccessService,
    private readonly kycStats: KycQueueStatsService,
    private readonly pendingDepositsStats: PendingDepositsStatsService,
    private readonly liveChatStats: LiveChatStatsService,
    private readonly walletAdmin: PlatformWalletAdminService,
    private readonly kycNotificationDismiss: KycNotificationDismissService,
    readonly theme: ThemeService,
    private readonly phoneVerificationPrompt: PhoneVerificationPromptService,
    readonly connectivity: NetworkConnectivityService,
  ) {
    this.showShell = !router.url.startsWith('/auth');
  }

  ngOnInit(): void {
    this.currentUser.syncFromStorage();
    this.registerActivityListeners();
    this.currentUser.user$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.shellUser = user;
      this.rebuildVisibleNav();
      this.cdr.markForCheck();
    });
    this.kycStats.summary$.pipe(takeUntil(this.destroy$)).subscribe((summary) => {
      this.kycSummary = summary;
      this.applyKycQueueSummary(summary);
      this.cdr.markForCheck();
    });
    this.pendingDepositsStats.deposits$.pipe(takeUntil(this.destroy$)).subscribe((deposits) => {
      this.pendingDeposits = deposits;
      this.rebuildTopNotifications();
      this.cdr.markForCheck();
    });
    this.liveChatStats.tickets$.pipe(takeUntil(this.destroy$)).subscribe((tickets) => {
      this.liveChatTickets = tickets;
      this.rebuildTopNotifications();
      this.cdr.markForCheck();
    });
    this.rebuildVisibleNav();
    this.syncChromeFromUrl();
    this.rebuildBreadcrumbs();
    this.hydrateSessionProfile();
    if (this.showShell && isStoredSessionToken(this.storage.getToken())) {
      setTimeout(() => {
        if (this.destroy$.closed) {
          return;
        }
        this.phoneVerificationPrompt.maybePrompt().pipe(takeUntil(this.destroy$)).subscribe();
      }, 800);
    }
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        tap(() => {
          this.syncChromeFromUrl();
          this.rebuildBreadcrumbs();
          this.profileOpen = false;
          this.notificationsOpen = false;
          this.scheduleKycStatsRefresh();
          this.schedulePendingDepositsRefresh();
          this.scheduleLiveChatRefresh();
          this.hydrateSessionProfile();
          this.cdr.markForCheck();
        }),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.clearSessionTimers();
    this.unregisterActivityListeners();
    this.destroy$.next();
    this.destroy$.complete();
  }

  trackByRoute(_index: number, item: NavItem): string {
    return item.route;
  }

  trackByGroupEntry(_index: number, entry: NavGroupEntry): string {
    return this.isGroupHeading(entry) ? `heading-${entry.label}` : entry.route;
  }

  trackBySubEntry(_index: number, entry: NavSubEntry): string {
    return entry.type === 'heading' ? `heading-${entry.label}` : entry.route;
  }

  isGroupHeading(entry: NavGroupEntry): entry is { type: 'heading'; label: string } {
    return 'type' in entry && entry.type === 'heading';
  }

  isNavChild(entry: NavGroupEntry): entry is NavChild {
    return !this.isGroupHeading(entry);
  }

  isNavLink(entry: NavSubEntry): entry is { type?: 'link'; label: string; icon: string; route: string } {
    return entry.type !== 'heading';
  }

  hasSubsection(child: NavChild): boolean {
    return (child.children?.length ?? 0) > 0;
  }

  /** Drives sidebar child rendering (heading | nested subsection | flat link). */
  navEntryKind(entry: NavGroupEntry): 'heading' | 'subsection' | 'link' {
    if (this.isGroupHeading(entry)) {
      return 'heading';
    }
    if (this.isNavChild(entry) && this.hasSubsection(entry)) {
      return 'subsection';
    }
    return 'link';
  }

  navChild(entry: NavGroupEntry): NavChild {
    return entry as NavChild;
  }

  /** First navigable child route when sidebar is collapsed (fallback: group route). */
  defaultGroupRoute(item: NavItem): string {
    if (!item.children?.length) {
      return item.route;
    }
    const firstLink = item.children.find((e): e is NavChild => this.isNavChild(e));
    return firstLink?.route ?? item.route;
  }

  /** True when the URL is the all-orgs list or a classification view (not branches/agents/industries). */
  private isAllOrganisationsSubsectionUrl(url: string): boolean {
    const path = url.split('?')[0];
    return path === '/organizations' || path.startsWith('/organizations/classification/');
  }

  trackByNotifId(_index: number, n: TopNotification): string {
    return n.id;
  }

  /** Expand-state key for a top-level sidebar group (e.g. `/organizations`). */
  groupExpandKey(item: NavItem): string {
    return item.route;
  }

  /** Expand-state key for a nested subsection — must not reuse the parent route. */
  subsectionExpandKey(parent: NavItem, child: NavChild): string {
    return `${parent.route}::${child.label}`;
  }

  toggleGroup(key: string, event?: Event): void {
    event?.stopPropagation();
    event?.preventDefault();
    const expanding = !this.isGroupExpanded(key);
    this.expandedGroups.update((groups) => ({ ...groups, [key]: expanding }));
    if (expanding) {
      this.userCollapsedNavGroups.delete(key);
      this.scrollNavGroupIntoView(key);
    } else {
      this.userCollapsedNavGroups.add(key);
    }
    this.cdr.detectChanges();
  }

  private scrollNavGroupIntoView(groupKey: string): void {
    requestAnimationFrame(() => {
      const children = document.querySelector<HTMLElement>(
        `.sb-children[data-nav-group="${CSS.escape(groupKey)}"]`,
      );
      children?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    });
  }

  isSectionActive(route: string, url: string): boolean {
    return url.startsWith(route);
  }

  isGroupExpanded(key: string): boolean {
    return Boolean(this.expandedGroups()[key]);
  }

  private syncChromeFromUrl(): void {
    const url = this.router.url;
    this.currentUrl = url;
    const wantsShell = !url.startsWith('/auth');
    this.showShell = wantsShell;

    if (!wantsShell) {
      this.shellNavExpandedInitialized = false;
      this.userCollapsedNavGroups.clear();
      this.expandedGroups.set({});
      this.pageTitle = this.resolvePageTitle(url);
      this.showNavRoleHint = false;
      this.syncSessionWatchState();
      this.cdr.markForCheck();
      return;
    }

    const firstShellEntry = !this.shellNavExpandedInitialized;
    if (firstShellEntry) {
      this.shellNavExpandedInitialized = true;
      this.userCollapsedNavGroups.clear();
      this.expandedGroups.set({});
    }

    this.applyNavExpansionFromUrl(url, !firstShellEntry);

    this.pageTitle = this.resolvePageTitle(url);
    this.updateNavRoleHint();
    this.syncSessionWatchState();
    this.cdr.markForCheck();
  }

  /**
   * Keeps only the active section open when navigating. On first shell entry after login,
   * leaves every group collapsed regardless of the landing route.
   */
  private applyNavExpansionFromUrl(url: string, expandActiveSection: boolean): void {
    const nextExpanded = { ...this.expandedGroups() };
    const activeGroupKeys = new Set<string>();

    for (const item of this.visibleNavItems) {
      if (!item.children?.length) {
        continue;
      }
      const groupKey = this.groupExpandKey(item);
      if (this.isRouteInNavGroup(url, item.route)) {
        activeGroupKeys.add(groupKey);
        if (expandActiveSection && !this.userCollapsedNavGroups.has(groupKey)) {
          nextExpanded[groupKey] = true;
        }
      }
      for (const child of item.children) {
        if (this.isNavChild(child) && child.children?.length) {
          const subKey = this.subsectionExpandKey(item, child);
          if (expandActiveSection && this.isAllOrganisationsSubsectionUrl(url)) {
            if (!this.userCollapsedNavGroups.has(subKey)) {
              nextExpanded[subKey] = true;
            }
          } else if (activeGroupKeys.has(groupKey)) {
            nextExpanded[subKey] = false;
          }
        }
      }
    }

    if (activeGroupKeys.size > 0) {
      for (const item of this.visibleNavItems) {
        if (!item.children?.length) {
          continue;
        }
        const groupKey = this.groupExpandKey(item);
        if (!activeGroupKeys.has(groupKey)) {
          nextExpanded[groupKey] = false;
        }
      }
    }

    this.expandedGroups.set(nextExpanded);
  }

  private hydrateSessionProfile(): void {
    if (!this.isAuthenticated() || this.router.url.startsWith('/auth')) {
      return;
    }
    const roles = this.navAccess.currentRoles();
    if (this.sessionProfileHydrated && roles.length > 0) {
      return;
    }
    if (roles.length > 0 && this.currentUser.snapshot) {
      this.sessionProfileHydrated = true;
      this.rebuildVisibleNav();
      this.updateNavRoleHint();
      return;
    }
    this.currentUser
      .refreshFromApi()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.sessionProfileHydrated = true;
          this.rebuildVisibleNav();
          this.updateNavRoleHint();
          this.cdr.markForCheck();
        }),
      )
      .subscribe();
  }

  private updateNavRoleHint(): void {
    this.showNavRoleHint =
      this.showShell &&
      this.sessionProfileHydrated &&
      this.navAccess.currentRoles().length === 0;
  }

  private syncSessionWatchState(): void {
    if (!this.showShell || !this.isAuthenticated()) {
      this.sessionWarningVisible = false;
      this.clearSessionTimers();
      return;
    }
    this.armSessionTimers();
  }

  private registerActivityListeners(): void {
    for (const evt of this.activityEvents) {
      document.addEventListener(evt, this.onActivityBound, { passive: true });
    }
  }

  private unregisterActivityListeners(): void {
    for (const evt of this.activityEvents) {
      document.removeEventListener(evt, this.onActivityBound);
    }
  }

  private onUserActivity(): void {
    if (!this.showShell || !this.isAuthenticated()) {
      return;
    }
    const now = Date.now();
    if (now - this.lastActivityResetMs < AppComponent.ACTIVITY_RESET_COOLDOWN_MS) {
      return;
    }
    this.lastActivityResetMs = now;
    this.armSessionTimers();
  }

  private armSessionTimers(): void {
    this.clearSessionTimers();
    this.sessionWarningVisible = false;
    this.sessionSecondsRemaining = 0;
    const warningDelayMs = this.idleTimeoutMs - this.idleWarningMs;
    this.idleWarningTimerId = setTimeout(() => this.showSessionWarning(), warningDelayMs);
    this.idleLogoutTimerId = setTimeout(() => this.handleInactivityTimeout(), this.idleTimeoutMs);
  }

  private showSessionWarning(): void {
    this.sessionWarningVisible = true;
    this.warningDeadlineMs = Date.now() + this.idleWarningMs;
    if (this.idleLogoutTimerId) {
      clearTimeout(this.idleLogoutTimerId);
    }
    this.idleLogoutTimerId = setTimeout(() => this.handleInactivityTimeout(), this.idleWarningMs);
    this.updateSessionCountdown();
    this.idleCountdownTimerId = setInterval(() => this.updateSessionCountdown(), 1000);
    this.cdr.markForCheck();
  }

  private updateSessionCountdown(): void {
    const remaining = Math.max(0, Math.ceil((this.warningDeadlineMs - Date.now()) / 1000));
    this.sessionSecondsRemaining = remaining;
    if (remaining <= 0) {
      if (this.idleCountdownTimerId) {
        clearInterval(this.idleCountdownTimerId);
        this.idleCountdownTimerId = null;
      }
    }
    this.cdr.markForCheck();
  }

  stayLoggedIn(event?: Event): void {
    event?.stopPropagation();
    event?.preventDefault();
    if (!this.isAuthenticated()) {
      return;
    }
    this.lastActivityResetMs = 0;
    this.armSessionTimers();
    this.cdr.markForCheck();
  }

  private handleInactivityTimeout(): void {
    this.logout(true);
  }

  get sessionCountdownLabel(): string {
    const mins = Math.floor(this.sessionSecondsRemaining / 60);
    const secs = this.sessionSecondsRemaining % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  private clearSessionTimers(): void {
    if (this.idleWarningTimerId) {
      clearTimeout(this.idleWarningTimerId);
      this.idleWarningTimerId = null;
    }
    if (this.idleLogoutTimerId) {
      clearTimeout(this.idleLogoutTimerId);
      this.idleLogoutTimerId = null;
    }
    if (this.idleCountdownTimerId) {
      clearInterval(this.idleCountdownTimerId);
      this.idleCountdownTimerId = null;
    }
  }

  private isAuthenticated(): boolean {
    return isStoredSessionToken(this.storage.getToken());
  }

  /** True when the current URL is this nav group or one of its child routes. */
  private isRouteInNavGroup(url: string, groupRoute: string): boolean {
    const path = url.split('?')[0].split('#')[0];
    return path === groupRoute || path.startsWith(groupRoute + '/');
  }

  private resolvePageTitle(url: string): string {
    const path = url.split('?')[0];
    if (path === '/dashboard' || path.startsWith('/dashboard/')) {
      return this.shellUser?.welcomeMessage ?? 'Dashboard';
    }
    if (url.startsWith('/account')) {
      return 'My account';
    }
    if (url.startsWith('/settings')) {
      return 'Settings';
    }
    if (url.startsWith('/help/requisitions')) {
      return 'Demo requisitions';
    }
    if (url.startsWith('/help/bot-analytics')) {
      return 'Bot analytics';
    }
    if (url.startsWith('/help/bot-knowledge')) {
      return 'Bot knowledge';
    }
    if (url.startsWith('/help/bot-service')) {
      return 'Bot service';
    }
    if (url.startsWith('/help/live-chat') || url.startsWith('/help/tickets')) {
      return 'Live chat';
    }
    if (url.startsWith('/help')) {
      return 'Help & Support';
    }
    if (url.startsWith('/analytics/revenue')) {
      return 'Platform revenue';
    }
    if (url.startsWith('/analytics/companies')) {
      return 'Company analytics';
    }
    if (url.startsWith('/analytics')) {
      return 'Analytics';
    }
    for (const item of this.visibleNavItems.length ? this.visibleNavItems : this.allNavItems) {
      if (item.children) {
        for (const child of item.children) {
          if (!this.isNavChild(child)) {
            continue;
          }
          for (const entry of child.children ?? []) {
            if (this.isNavLink(entry) && url.startsWith(entry.route)) {
              return entry.label;
            }
          }
          if (!child.children?.length && url.startsWith(child.route)) {
            return child.label;
          }
        }
        if (url === '/organizations' || url.startsWith('/organizations?')) {
          return 'All organisations';
        }
        if (url.startsWith(item.route)) {
          return item.label;
        }
      } else if (url.startsWith(item.route)) {
        return item.label;
      }
    }
    return 'Dashboard';
  }

  private rebuildBreadcrumbs(): void {
    if (!this.showShell) {
      this.breadcrumbs = [];
      return;
    }

    const pathParts: string[] = [];
    const crumbs: Breadcrumb[] = [];
    let node: ActivatedRouteSnapshot = this.router.routerState.snapshot.root;

    while (node.firstChild) {
      node = node.firstChild;
      for (const seg of node.url) {
        if (seg.path) {
          pathParts.push(seg.path);
        }
      }
      const raw = node.data['breadcrumb'];
      if (typeof raw === 'string' && raw.trim().length > 0) {
        crumbs.push({
          label: raw.trim(),
          url: '/' + pathParts.join('/'),
        });
      }
    }

    this.breadcrumbs = [{ label: 'Home', url: '/dashboard' }, ...crumbs];
    this.cdr.markForCheck();
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
    this.cdr.detectChanges();
  }

  get notificationCount(): number {
    return this.topNotifications.length;
  }

  navBadge(item: NavItem): number | null {
    if (item.route === '/kyc/applications') {
      const count = this.kycStats.snapshot?.totalInQueue ?? 0;
      return count > 0 ? count : null;
    }
    return item.badge ?? null;
  }

  private applyKycQueueSummary(summary: KycQueueSummary | null): void {
    const kycNav = this.allNavItems.find((item) => item.route === '/kyc/applications');
    if (kycNav) {
      const count = summary?.totalInQueue ?? 0;
      kycNav.badge = count > 0 ? count : undefined;
    }
    this.rebuildTopNotifications();
  }

  private rebuildTopNotifications(): void {
    const kycNotifications: TopNotification[] = (this.kycSummary?.recentApplications ?? []).map((row) => ({
      id: `kyc-${row.id}`,
      title: 'KYC application in queue',
      body: `${row.applicant} — ${row.statusLabel}`,
      time: row.submitted?.trim() || 'Pending review',
      route: ['/kyc/applications'],
      queryParams: { applicationId: row.id },
    }));

    const depositNotifications: TopNotification[] = this.pendingDeposits.map((row) => ({
      id: `deposit-${row.id}`,
      title: 'Pending wallet deposit',
      body: this.formatDepositNotificationBody(row),
      time: this.walletAdmin.formatWhen(row.createdAt),
      route: ['/settings'],
      queryParams: {
        section: 'platform-billing',
        tab: 'deposits',
        depositId: row.id,
      },
    }));

    const liveChatNotifications: TopNotification[] = this.liveChatTickets.map((ticket) => ({
      id: `livechat-${ticket.id}`,
      title: 'New live chat message',
      body: this.formatLiveChatNotificationBody(ticket),
      time: this.walletAdmin.formatWhen(ticket.modifiedAt ?? ticket.createdAt),
      route: ['/help/live-chat'],
      queryParams: { ticketId: ticket.id },
    }));

    this.topNotifications = this.kycNotificationDismiss.filterById([
      ...kycNotifications,
      ...depositNotifications,
      ...liveChatNotifications,
    ]);
  }

  private formatLiveChatNotificationBody(ticket: AdminSupportTicket): string {
    const who = ticket.requesterUsername?.trim() || ticket.organizationName?.trim() || 'Requester';
    const subject = ticket.subject?.trim();
    return subject ? `${who} — ${subject}` : who;
  }

  private formatDepositNotificationBody(row: WalletDepositRow): string {
    const amount = this.walletAdmin.formatCents(row.amountCents, row.currencyCode);
    const org = row.organizationId ? `Organisation #${row.organizationId}` : 'Organisation';
    const ref = row.referenceNumber?.trim();
    return ref ? `${amount} — ${org} · ${ref}` : `${amount} — ${org}`;
  }

  toggleNotifications(): void {
    this.notificationsOpen = !this.notificationsOpen;
    if (this.notificationsOpen) {
      this.profileOpen = false;
      if (this.navAccess.canAccessRoute('/settings')) {
        this.pendingDepositsStats.refresh().pipe(takeUntil(this.destroy$)).subscribe();
      }
      if (this.isOperationsHandler()) {
        this.liveChatStats.refresh().pipe(takeUntil(this.destroy$)).subscribe();
      }
    }
    this.cdr.markForCheck();
  }

  toggleProfile(): void {
    this.profileOpen = !this.profileOpen;
    if (this.profileOpen) {
      this.notificationsOpen = false;
    }
    this.cdr.markForCheck();
  }

  openNotification(notification: TopNotification): void {
    if (!notification.route?.length) {
      return;
    }
    this.notificationsOpen = false;
    void this.router.navigate(notification.route, {
      queryParams: notification.queryParams,
    });
    this.dismissNotification(notification.id);
    this.cdr.markForCheck();
  }

  dismissNotification(id: string, event?: Event): void {
    event?.stopPropagation();
    // Live-chat items dedupe via the stats service (re-notify on new messages), so they must NOT be
    // added to the permanent dismiss set — only marked seen up to the current activity.
    if (id.startsWith('livechat-')) {
      const ticketId = Number(id.slice('livechat-'.length));
      if (Number.isFinite(ticketId)) {
        this.liveChatStats.markSeen(ticketId);
      }
    } else {
      this.kycNotificationDismiss.dismiss(id);
    }
    this.topNotifications = this.topNotifications.filter((n) => n.id !== id);
    this.cdr.markForCheck();
  }

  clearAllNotifications(): void {
    for (const n of this.topNotifications) {
      if (n.id.startsWith('livechat-')) {
        const ticketId = Number(n.id.slice('livechat-'.length));
        if (Number.isFinite(ticketId)) {
          this.liveChatStats.markSeen(ticketId);
        }
      } else {
        this.kycNotificationDismiss.dismiss(n.id);
      }
    }
    this.topNotifications = [];
    this.cdr.markForCheck();
  }

  goMyAccount(): void {
    void this.router.navigate(['/account']);
    queueMicrotask(() => {
      this.profileOpen = false;
      this.cdr.markForCheck();
    });
  }

  get myAccountRoute(): string[] {
    return ['/account'];
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(ev: MouseEvent): void {
    const el = ev.target as HTMLElement;
    if (el.closest('.tb-notify-wrap') || el.closest('.tb-profile-wrap')) {
      return;
    }
    if (this.notificationsOpen || this.profileOpen) {
      this.notificationsOpen = false;
      this.profileOpen = false;
      this.cdr.markForCheck();
    }
  }

  rebuildVisibleNav(): void {
    if (!this.showShell) {
      return;
    }
    const filtered = this.navAccess.filterNavItems(this.allNavItems);
    if (filtered.length > 0) {
      this.visibleNavItems = filtered;
    } else {
      const stored = this.storage.getUser();
      const fallback = [...this.bootstrapNavItems];
      if (stored?.organizationKycApprover) {
        fallback.push({ label: 'KYC Queue', icon: 'verified_user', route: '/kyc/applications' });
      }
      if (stored?.operationalIssueHandler) {
        fallback.push({
          label: 'Help & Support',
          icon: 'help_outline',
          route: '/help',
          children: [
            { label: 'Live chat', icon: 'forum', route: '/help/live-chat' },
            { label: 'Demo requisitions', icon: 'event_available', route: '/help/requisitions' },
            { label: 'Bot service', icon: 'smart_toy', route: '/help/bot-service' },
        { label: 'Bot analytics', icon: 'insights', route: '/help/bot-analytics' },
        { label: 'Bot knowledge', icon: 'menu_book', route: '/help/bot-knowledge' },
          ],
        });
      }
      this.visibleNavItems = fallback;
    }
    this.updateNavRoleHint();
  }

  private scheduleKycStatsRefresh(): void {
    if (this.kycStatsLoaded || !this.showShell || !this.isAuthenticated()) {
      return;
    }
    this.kycStatsLoaded = true;
    setTimeout(() => {
      this.kycStats.refresh().pipe(takeUntil(this.destroy$)).subscribe();
    }, 350);
  }

  private schedulePendingDepositsRefresh(): void {
    if (this.pendingDepositsLoaded || !this.showShell || !this.isAuthenticated()) {
      return;
    }
    if (!this.navAccess.canAccessRoute('/settings')) {
      return;
    }
    this.pendingDepositsLoaded = true;
    setTimeout(() => {
      this.pendingDepositsStats.refresh().pipe(takeUntil(this.destroy$)).subscribe();
    }, 450);
  }

  /** Live-chat bell notifications are only for operations handlers (operationalIssueHandler flag). */
  private isOperationsHandler(): boolean {
    return this.storage.getUser()?.operationalIssueHandler === true;
  }

  private scheduleLiveChatRefresh(): void {
    if (this.liveChatLoaded || !this.showShell || !this.isAuthenticated()) {
      return;
    }
    if (!this.isOperationsHandler()) {
      return;
    }
    this.liveChatLoaded = true;
    setTimeout(() => {
      this.liveChatStats.refresh().pipe(takeUntil(this.destroy$)).subscribe();
    }, 550);
  }

  logout(fromTimeout = false): void {
    this.clearSessionTimers();
    this.sessionWarningVisible = false;
    this.profileOpen = false;
    this.notificationsOpen = false;
    this.storage.clearSession();
    this.currentUser.clear();
    this.sessionProfileHydrated = false;
    this.kycStatsLoaded = false;
    this.pendingDepositsLoaded = false;
    this.liveChatLoaded = false;
    this.liveChatStats.clear();
    this.shellNavExpandedInitialized = false;
    this.userCollapsedNavGroups.clear();
    this.expandedGroups.set({});
    void this.router.navigate(['/auth/login'], {
      replaceUrl: true,
      queryParams: { reason: fromTimeout ? 'inactivity' : 'logout' },
    });
    if (fromTimeout) {
      this.topNotifications = [
        {
          id: `session-timeout-${Date.now()}`,
          title: 'Session expired',
          body: 'You were signed out after inactivity. Please sign in again.',
          time: 'Just now',
        },
      ];
    }
    this.cdr.markForCheck();
  }
}
