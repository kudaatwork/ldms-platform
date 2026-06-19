import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  HostListener,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { Subject, of } from 'rxjs';
import { filter, map, switchMap, take, takeUntil } from 'rxjs/operators';
import { CurrentUser } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';
import { AuthStateService } from '../../core/services/auth-state.service';
import { NavAccessService } from '../../core/services/nav-access.service';
import { ShellUserService } from '../../core/services/shell-user.service';
import { ThemeService } from '../../core/services/theme.service';
import { shellRoleSummary } from '../../core/utils/field-display.util';
import { currentUserFromJwt, decodeJwtPayload, isJwtExpired } from '../../core/utils/jwt.util';
import { StorageService } from '../../core/services/storage.service';
import { UserProfileService } from '../../core/services/user-profile.service';
import { SessionIdleService } from '../../core/services/session-idle.service';
import { SessionExpiryService } from '../../core/services/session-expiry.service';
import { AuthenticatedHistoryService } from '../../core/services/authenticated-history.service';
import { PhoneVerificationPromptService } from '../../core/services/phone-verification-prompt.service';
import { ShellNotification, ShellNotificationService } from '../../core/services/shell-notification.service';
import { FuelAlertMonitorService } from '../../core/services/fuel-alert-monitor.service';
import { CurrencyContextService } from '../../core/services/currency-context.service';
import { PlatformWalletService, type OrganizationBillingMode, type PlatformWalletSummary } from '../../core/services/platform-wallet.service';
import { DuplexTradingModeService } from '../../core/services/duplex-trading-mode.service';
import { portalHomeRoute } from '../../core/utils/portal-navigation.util';
import { isDuplexTradingOrg, type TradingWorkspaceMode } from '../../core/utils/org-classification.util';
import {
  AUDIT_LOG_NAV_ITEM,
  NAV_CONFIG,
  NavItem,
  withAnalyticsNav,
  withAuditLogNav,
  withFleetNav,
  withInventoryNav,
  withMyOrdersNav,
  withOperationalModeNav,
  withOrganizationManagementNav,
  withShipmentsNav,
  withUsersNavAfterDocuments,
} from '../sidebar/sidebar.config';

interface Breadcrumb {
  label: string;
  url: string;
}

@Component({
  selector: 'app-shell-layout',
  templateUrl: './shell-layout.component.html',
  styleUrls: ['./shell-layout.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ShellLayoutComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  currentUser: CurrentUser | null = null;
  navItems: NavItem[] = [];
  activeTradingMode: TradingWorkspaceMode | null = null;

  collapsed = false;
  mobileSidebarOpen = false;
  currentUrl = '';
  /** Expanded state per sidebar group route (e.g. /users, /activity). */
  private expandedGroups: Record<string, boolean> = {};

  pageTitle = 'Dashboard';
  breadcrumbs: Breadcrumb[] = [];

  notificationsOpen = false;
  profileOpen = false;
  sessionWarningVisible = false;
  sessionSecondsRemaining = 0;
  walletSummary: PlatformWalletSummary | null = null;
  topNotifications: ShellNotification[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly authState: AuthStateService,
    private readonly navAccess: NavAccessService,
    private readonly shellUserService: ShellUserService,
    private readonly storage: StorageService,
    private readonly userProfile: UserProfileService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    readonly theme: ThemeService,
    private readonly sessionIdle: SessionIdleService,
    private readonly sessionExpiry: SessionExpiryService,
    private readonly authenticatedHistory: AuthenticatedHistoryService,
    private readonly phoneVerificationPrompt: PhoneVerificationPromptService,
    private readonly shellNotifications: ShellNotificationService,
    private readonly fuelAlertMonitor: FuelAlertMonitorService,
    private readonly currencyContext: CurrencyContextService,
    private readonly platformWallet: PlatformWalletService,
    private readonly duplexTradingMode: DuplexTradingModeService,
  ) {}

  ngOnInit(): void {
    this.shellUserService.syncFromStorage();
    this.currentUser = this.authState.currentUser;
    if (this.currentUser) {
      this.shellUserService.syncFromAuthState(this.currentUser);
    } else {
      const token = this.storage.getToken();
      if (token && !token.startsWith('mock.') && !isJwtExpired(token)) {
        const jwtUser = currentUserFromJwt(token);
        if (jwtUser) {
          this.authState.setCurrentUser(jwtUser);
          this.currentUser = jwtUser;
          this.shellUserService.syncFromAuthState(jwtUser);
        }
      }
    }
    this.refreshWorkspaceNav();

    this.duplexTradingMode.activeMode$.pipe(takeUntil(this.destroy$)).subscribe((mode) => {
      this.activeTradingMode = mode;
      this.refreshWorkspaceNav();
      this.cdr.markForCheck();
    });

    this.authState.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.currentUser = user;
      this.duplexTradingMode.syncFromUser(user);
      if (user) {
        this.shellUserService.syncFromAuthState(user);
        this.currencyContext.load().subscribe();
        this.platformWallet.refreshSummary().pipe(takeUntil(this.destroy$)).subscribe({
          next: (summary) => {
            this.walletSummary = summary;
            this.cdr.markForCheck();
          },
          error: () => {
            this.walletSummary = {
              balanceCents: 0,
              currencyCode: 'USD',
              billingMode: 'PREPAID_WALLET',
            };
            this.cdr.markForCheck();
          },
        });
        this.platformWallet.summary$.pipe(takeUntil(this.destroy$)).subscribe((summary) => {
          if (summary) {
            this.walletSummary = summary;
            this.cdr.markForCheck();
          }
        });
        this.shellNotifications.refresh();
        this.fuelAlertMonitor.resumeWatching();
      }
      this.refreshWorkspaceNav();
      this.syncChromeFromUrl();
      this.cdr.markForCheck();
    });

    this.shellUserService.user$.pipe(takeUntil(this.destroy$)).subscribe(() => this.cdr.markForCheck());

    this.shellNotifications.notifications$.pipe(takeUntil(this.destroy$)).subscribe((items) => {
      this.topNotifications = items;
      this.cdr.markForCheck();
    });

    const stored = this.storage.getUser();
    const needsProfileRefresh = !String(stored?.firstName ?? '').trim();
    if (needsProfileRefresh) {
      setTimeout(() => {
        if (this.destroy$.closed) {
          return;
        }
        this.shellUserService.refreshFromApi().pipe(takeUntil(this.destroy$)).subscribe();
      }, 400);
    }

    this.syncChromeFromUrl();
    this.startAuthenticatedSessionGuards();
    setTimeout(() => {
      if (this.destroy$.closed) {
        return;
      }
      this.phoneVerificationPrompt.maybePrompt().pipe(takeUntil(this.destroy$)).subscribe();
    }, 800);

    this.sessionIdle.warningVisible$.pipe(takeUntil(this.destroy$)).subscribe((visible) => {
      this.sessionWarningVisible = visible;
      this.cdr.markForCheck();
    });
    this.sessionIdle.secondsRemaining$.pipe(takeUntil(this.destroy$)).subscribe((seconds) => {
      this.sessionSecondsRemaining = seconds;
      this.cdr.markForCheck();
    });

    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntil(this.destroy$),
      )
      .subscribe(() => {
        this.syncChromeFromUrl();
        this.closeMobileSidebar();
        this.profileOpen = false;
        this.notificationsOpen = false;
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.sessionIdle.deactivate();
    this.authenticatedHistory.disable();
    this.destroy$.next();
    this.destroy$.complete();
  }

  get sessionCountdownLabel(): string {
    return this.sessionIdle.countdownLabel(this.sessionSecondsRemaining);
  }

  staySignedIn(event?: Event): void {
    event?.stopPropagation();
    event?.preventDefault();
    this.sessionIdle.staySignedIn();
    this.sessionWarningVisible = false;
    this.sessionSecondsRemaining = 0;
    this.cdr.markForCheck();
  }

  private startAuthenticatedSessionGuards(): void {
    const homeCommands = portalHomeRoute(this.currentUser, this.activeTradingMode);
    const homePath = `/${homeCommands.filter((s) => s && s !== '/').join('/')}` || '/dashboard';
    const workspaceUrl = this.router.url.startsWith('/auth') ? homePath : this.router.url;
    this.authenticatedHistory.enable(workspaceUrl);
    this.sessionIdle.activate(() => this.sessionExpiry.handleSessionExpired('inactivity'));
  }

  get shellUser() {
    return this.shellUserService.snapshot;
  }

  get userInitials(): string {
    const shell = this.shellUser;
    if (shell?.initials) {
      return shell.initials;
    }
    const u = this.currentUser;
    if (!u) {
      return 'LX';
    }
    const first = (u.firstName ?? '').trim();
    const last = (u.lastName ?? '').trim();
    if (first && last) {
      return `${first.charAt(0)}${last.charAt(0)}`.toUpperCase();
    }
    if (first.length >= 2) {
      return first.slice(0, 2).toUpperCase();
    }
    const fromOrg = u.orgName
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0]?.toUpperCase() ?? '')
      .join('');
    if (fromOrg.length >= 2) {
      return fromOrg;
    }
    const mail = u.email ?? '';
    const local = mail.split('@')[0] ?? '';
    if (local.length >= 2) {
      return local.slice(0, 2).toUpperCase();
    }
    return 'LX';
  }

  get roleSummary(): string {
    const shellRole = this.shellUser?.role?.trim();
    if (shellRole && !shellRole.includes(',')) {
      return shellRole;
    }
    const u = this.currentUser;
    return shellRoleSummary(u?.roleLabel, u?.orgClassification);
  }

  get userEmail(): string {
    return this.currentUser?.email ?? '';
  }

  get notificationCount(): number {
    return this.topNotifications.length;
  }

  get isInventoryWorkspace(): boolean {
    return (
      this.currentUrl.startsWith('/products-inventory') ||
      this.currentUrl.startsWith('/purchase-orders') ||
      this.currentUrl.startsWith('/my-orders')
    );
  }

  trackByRoute(_index: number, item: NavItem): string {
    return item.route;
  }

  trackByChildRoute(_index: number, child: { route: string }): string {
    return child.route;
  }

  groupExpandKey(item: NavItem): string {
    return item.route;
  }

  toggleGroup(key: string, event?: Event): void {
    event?.stopPropagation();
    event?.preventDefault();
    const expanding = !this.isGroupExpanded(key);
    this.expandedGroups = { ...this.expandedGroups, [key]: expanding };
    if (expanding) {
      requestAnimationFrame(() => {
        const children = document.querySelector<HTMLElement>(
          `.sb-children[data-nav-group="${CSS.escape(key)}"]`,
        );
        children?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      });
    }
    this.cdr.markForCheck();
  }

  onNavCollapsedGroupClick(item: NavItem, event: Event): void {
    event.preventDefault();
    const target = item.children?.[0];
    if (target) {
      this.onNavChildClick(target, event);
      return;
    }
    this.onNavParentClick(item, event);
  }

  onNavParentClick(item: NavItem, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    const target = item.children?.[0];
    if (target) {
      this.onNavChildClick(target, event);
      return;
    }
    this.closeMobileSidebar();
    void this.router.navigate(this.navCommands(item.route), { queryParams: item.queryParams });
  }

  onNavChildClick(
    child: { route: string; queryParams?: Record<string, string> },
    event?: Event,
  ): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.closeMobileSidebar();
    const commands = this.navCommands(child.route);
    const tree = this.router.createUrlTree(commands, { queryParams: child.queryParams });
    const targetPath = this.router.serializeUrl(tree).split('?')[0].split('#')[0];
    const currentPath = this.router.url.split('?')[0].split('#')[0];
    if (targetPath === currentPath) {
      return;
    }
    void this.router.navigateByUrl(tree);
  }

  isNavChildActive(child: { route: string; queryParams?: Record<string, string> }): boolean {
    const target = this.router.serializeUrl(
      this.router.createUrlTree(this.navCommands(child.route), { queryParams: child.queryParams }),
    );
    const current = this.router.url.split('#')[0];
    return current === target || current.split('?')[0] === target.split('?')[0];
  }

  /** Absolute app paths such as `/products-inventory/warehouses` → `['/products-inventory', 'warehouses']`. */
  private navCommands(route: string): string[] {
    const parts = route.split('/').filter(Boolean);
    if (!parts.length) {
      return ['/'];
    }
    return ['/' + parts[0], ...parts.slice(1)];
  }

  isGroupExpanded(key: string): boolean {
    return this.expandedGroups[key] ?? false;
  }

  isSectionActive(route: string): boolean {
    return this.currentUrl.startsWith(route);
  }

  trackByNotifId(_index: number, n: ShellNotification): string {
    return n.id;
  }

  toggle(): void {
    if (this.isNarrowViewport()) {
      this.mobileSidebarOpen = !this.mobileSidebarOpen;
    } else {
      this.collapsed = !this.collapsed;
      this.mobileSidebarOpen = false;
    }
    this.cdr.markForCheck();
  }

  closeMobileSidebar(): void {
    this.mobileSidebarOpen = false;
    this.cdr.markForCheck();
  }

  onThemeToggle(): void {
    this.theme.toggle();
    this.cdr.markForCheck();
  }

  toggleNotifications(): void {
    this.notificationsOpen = !this.notificationsOpen;
    if (this.notificationsOpen) {
      this.profileOpen = false;
      this.shellNotifications.refresh();
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

  dismissNotification(id: string, event?: Event): void {
    event?.stopPropagation();
    event?.preventDefault();
    this.shellNotifications.dismiss(id);
    this.cdr.markForCheck();
  }

  clearAllNotifications(): void {
    this.shellNotifications.clearAll();
    this.cdr.markForCheck();
  }

  onNotificationClick(notification: ShellNotification, event?: Event): void {
    event?.stopPropagation();
    if (!notification.action) {
      return;
    }
    this.notificationsOpen = false;
    if (notification.action === 'fuel-alert') {
      if (notification.tripId) {
        void this.router.navigate(['/shipments/live', notification.tripId]);
      }
      this.cdr.markForCheck();
      return;
    }
    this.navigateToVerificationAction(notification.action);
    this.cdr.markForCheck();
  }

  private navigateToVerificationAction(action: 'verify-phone' | 'verify-email'): void {
    const syncId = this.resolveSignedInUserId();
    if (syncId > 0) {
      void this.router.navigate(['/users', String(syncId), 'profile'], {
        queryParams: { verify: action === 'verify-phone' ? 'phone' : 'email' },
      });
      return;
    }

    const username = String(decodeJwtPayload(this.storage.getToken() ?? '')?.sub ?? '').trim();
    this.userProfile
      .fetchCurrentUser()
      .pipe(
        take(1),
        switchMap((profile) => {
          const fromProfile = Number(profile?.id ?? 0);
          if (Number.isFinite(fromProfile) && fromProfile > 0) {
            return of(fromProfile);
          }
          if (!username) {
            return of(0);
          }
          return this.userProfile.fetchByUsername(username).pipe(
            map((fallback) => {
              const fromFallback = Number(fallback?.id ?? 0);
              return Number.isFinite(fromFallback) && fromFallback > 0 ? fromFallback : 0;
            }),
          );
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((resolvedId) => {
        if (resolvedId > 0) {
          void this.router.navigate(['/users', String(resolvedId), 'profile'], {
            queryParams: { verify: action === 'verify-phone' ? 'phone' : 'email' },
          });
          return;
        }
        if (action === 'verify-phone') {
          this.phoneVerificationPrompt.openDialog().pipe(takeUntil(this.destroy$)).subscribe();
          return;
        }
        void this.router.navigate(['/account']);
      });
  }

  logout(fromInactivity = false): void {
    this.profileOpen = false;
    this.notificationsOpen = false;
    if (fromInactivity) {
      this.sessionExpiry.handleSessionExpired('inactivity');
      this.cdr.markForCheck();
      return;
    }
    this.sessionIdle.deactivate();
    this.authenticatedHistory.disable();
    this.authService.logout();
    void this.router.navigate(['/auth/login'], { replaceUrl: true });
    this.cdr.markForCheck();
  }

  /** Programmatic navigation so the flyout is not removed before the router handles the click. */
  goMyAccount(): void {
    void this.router.navigate(['/account']);
    queueMicrotask(() => {
      this.profileOpen = false;
      this.cdr.markForCheck();
    });
  }

  /** Full user profile workspace (sections, documents, security) — same route as users list “View profile”. */
  goViewProfile(): void {
    const syncId = this.resolveSignedInUserId();
    if (syncId > 0) {
      this.navigateToUserProfile(syncId);
      return;
    }

    const username = String(decodeJwtPayload(this.storage.getToken() ?? '')?.sub ?? '').trim();
    this.userProfile
      .fetchCurrentUser()
      .pipe(
        take(1),
        switchMap((profile) => {
          const fromProfile = Number(profile?.id ?? 0);
          if (Number.isFinite(fromProfile) && fromProfile > 0) {
            return of(fromProfile);
          }
          if (!username) {
            return of(0);
          }
          return this.userProfile.fetchByUsername(username).pipe(
            map((fallback) => {
              const fromFallback = Number(fallback?.id ?? 0);
              return Number.isFinite(fromFallback) && fromFallback > 0 ? fromFallback : 0;
            }),
          );
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((resolvedId) => {
        if (resolvedId > 0) {
          this.navigateToUserProfile(resolvedId);
          return;
        }
        this.navigateToMyAccountFallback();
      });
  }

  private navigateToUserProfile(userId: number): void {
    void this.router.navigate(['/users', String(userId), 'profile']);
    queueMicrotask(() => {
      this.profileOpen = false;
      this.cdr.markForCheck();
    });
  }

  private navigateToMyAccountFallback(): void {
    void this.router.navigate(['/account']);
    queueMicrotask(() => {
      this.profileOpen = false;
      this.cdr.markForCheck();
    });
  }

  goSettings(): void {
    void this.router.navigate(['/settings']);
    queueMicrotask(() => {
      this.profileOpen = false;
      this.cdr.markForCheck();
    });
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

  @HostListener('window:resize')
  onResize(): void {
    if (!this.isNarrowViewport()) {
      this.mobileSidebarOpen = false;
      this.cdr.markForCheck();
    }
  }

  get showDuplexSwitcher(): boolean {
    return isDuplexTradingOrg(this.currentUser);
  }

  switchTradingMode(mode: TradingWorkspaceMode): void {
    if (!this.currentUser || mode === this.activeTradingMode) {
      return;
    }
    this.duplexTradingMode.setMode(mode, this.currentUser);
    const home = portalHomeRoute(this.currentUser, mode);
    void this.router.navigate(home);
  }

  private refreshWorkspaceNav(): void {
    const classification = this.duplexTradingMode.effectiveClassification(this.currentUser);
    this.navItems = this.currentUser ? this.workspaceNav(classification) : [];
  }

  private workspaceNav(classification: CurrentUser['orgClassification']): NavItem[] {
    const fallback: NavItem[] = [{ label: 'Dashboard', route: '/dashboard', icon: 'dashboard' }];
    const crossDocking = this.currentUser?.crossDockingEnabled ?? false;
    const inventoryMgmt = this.currentUser?.inventoryManagementEnabled ?? true;
    const standalone = this.currentUser?.standaloneMode ?? false;
    const counterpartyEngagement = this.currentUser?.counterpartyEngagementMode ?? 'PLATFORM_ORG';
    const inventoryDataSource = this.currentUser?.inventoryDataSource ?? 'INTERNAL';

    const base = withOperationalModeNav(
      withAnalyticsNav(
        withAuditLogNav(
          withMyOrdersNav(
            withShipmentsNav(
              withFleetNav(
                withInventoryNav(
                  withUsersNavAfterDocuments(
                    withOrganizationManagementNav(
                      classification ? (NAV_CONFIG[classification] ?? fallback) : fallback,
                      classification ?? undefined,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
      crossDocking,
      inventoryMgmt,
      standalone,
      counterpartyEngagement,
      inventoryDataSource,
    );
    return this.navAccess.filterNavItems(base);
  }

  private isNarrowViewport(): boolean {
    return typeof window !== 'undefined' && window.innerWidth <= 768;
  }

  private syncChromeFromUrl(): void {
    const url = this.router.url;
    this.currentUrl = url.split('?')[0];
    const nextExpanded = { ...this.expandedGroups };
    const activeGroupKeys = new Set<string>();

    for (const item of this.navItems) {
      if (!item.children?.length) {
        continue;
      }
      const groupKey = this.groupExpandKey(item);
      if (this.isRouteInNavGroup(url, item.route)) {
        activeGroupKeys.add(groupKey);
        nextExpanded[groupKey] = true;
      }
    }

    // When viewing a section, close other top-level groups; on neutral pages keep manual toggles.
    if (activeGroupKeys.size > 0) {
      for (const item of this.navItems) {
        if (!item.children?.length) {
          continue;
        }
        const groupKey = this.groupExpandKey(item);
        if (!activeGroupKeys.has(groupKey)) {
          nextExpanded[groupKey] = false;
        }
      }
    }

    this.expandedGroups = nextExpanded;
    this.pageTitle = this.resolvePageTitle(this.currentUrl);
    this.rebuildBreadcrumbs();
    this.cdr.markForCheck();
  }

  /** True when the current URL is this nav group or one of its child routes. */
  private isRouteInNavGroup(url: string, groupRoute: string): boolean {
    const path = url.split('?')[0].split('#')[0];
    return path === groupRoute || path.startsWith(`${groupRoute}/`);
  }

  private resolveSignedInUserId(): number {
    const fromSession = Number(this.currentUser?.userId ?? 0);
    if (Number.isFinite(fromSession) && fromSession > 0) {
      return fromSession;
    }
    const jwt = decodeJwtPayload(this.storage.getToken() ?? '');
    const fromJwt = Number(jwt?.userId ?? 0);
    return Number.isFinite(fromJwt) && fromJwt > 0 ? fromJwt : 0;
  }

  private resolvePageTitle(url: string): string {
    const path = url.split('?')[0];
    if (path === '/dashboard' || path.startsWith('/dashboard/')) {
      return this.currentUser?.welcomeMessage ?? 'Dashboard';
    }
    if (url.startsWith('/account')) {
      return 'My account';
    }
    if (url.startsWith('/settings')) {
      return 'Settings';
    }
    if (url.startsWith('/help')) {
      return 'Help & Support';
    }
    const userWorkspace = this.resolveUserWorkspaceTitle(path);
    if (userWorkspace) {
      return userWorkspace;
    }
    for (const item of this.navItems) {
      for (const child of item.children ?? []) {
        if (
          url === child.route ||
          (child.route !== '/users' &&
            !child.route.startsWith('/products-inventory/') &&
            !child.route.startsWith('/fleet/') &&
            !child.route.startsWith('/my-orders/') &&
            !child.route.startsWith('/shipments/') &&
            !child.route.startsWith('/analytics/') &&
            !child.route.startsWith('/organization/') &&
            url.startsWith(child.route + '/'))
        ) {
          return child.label;
        }
      }
    }
    if (url.startsWith('/users')) {
      return 'User management';
    }
    if (url.startsWith('/activity')) {
      return AUDIT_LOG_NAV_ITEM.label;
    }
    const sorted = [...this.navItems].sort((a, b) => b.route.length - a.route.length);
    for (const item of sorted) {
      if (url === item.route || url.startsWith(item.route + '/')) {
        return item.label;
      }
    }
    if (url.includes('/dashboard')) {
      return 'Dashboard';
    }
    return 'Workspace';
  }

  private resolveUserWorkspaceTitle(path: string): string | null {
    const match = path.match(
      /^\/users\/(\d+)\/(profile|account|preferences|security-policies|addresses|password)$/,
    );
    if (!match) {
      return null;
    }
    switch (match[2]) {
      case 'profile':
        return 'User profile';
      case 'account':
        return 'User account';
      case 'preferences':
        return 'User preferences';
      case 'security-policies':
        return 'Security details';
      case 'addresses':
        return 'User addresses';
      case 'password':
        return 'Change password';
      default:
        return 'User profile';
    }
  }

  private rebuildBreadcrumbs(): void {
    const crumbs: Breadcrumb[] = [];
    const pathParts: string[] = [];
    let node: ActivatedRouteSnapshot = this.router.routerState.snapshot.root;

    while (node.firstChild) {
      node = node.firstChild;
      for (const seg of node.url) {
        if (seg.path) {
          pathParts.push(seg.path);
        }
      }
      const raw =
        (node.data['breadcrumb'] as string | undefined) ??
        (node.data['title'] as string | undefined);
      if (typeof raw === 'string' && raw.trim().length > 0) {
        crumbs.push({
          label: raw.trim(),
          url: '/' + pathParts.join('/'),
        });
      }
    }

    this.breadcrumbs = [{ label: 'Home', url: '/dashboard' }, ...crumbs];
  }

  get walletLabel(): string {
    const summary = this.walletSummary ?? {
      balanceCents: 0,
      currencyCode: 'USD',
      billingMode: 'PREPAID_WALLET' as const,
    };
    if (summary.billingMode === 'PREMIUM_SUBSCRIPTION') {
      return summary.subscriptionPackageName ?? 'Premium';
    }
    return this.platformWallet.formatCents(
      summary.balanceCents ?? 0,
      summary.currencyCode ?? 'USD',
    );
  }

  get walletLowBalance(): boolean {
    const summary = this.walletSummary;
    return summary?.billingMode === 'PREPAID_WALLET' && summary.lowBalance === true;
  }

  get walletBillingMode(): OrganizationBillingMode {
    return this.walletSummary?.billingMode ?? 'PREPAID_WALLET';
  }

  goWalletSettings(): void {
    void this.router.navigate(['/settings'], { queryParams: { section: 'billing' } });
  }
}
