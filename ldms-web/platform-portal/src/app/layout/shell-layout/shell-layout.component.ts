import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  HostListener,
  OnInit,
} from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { CurrentUser } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';
import { AuthStateService } from '../../core/services/auth-state.service';
import { ThemeService } from '../../core/services/theme.service';
import { NAV_CONFIG, NavItem } from '../sidebar/sidebar.config';

interface Breadcrumb {
  label: string;
  url: string;
}

interface ShellNotification {
  id: string;
  title: string;
  body: string;
  time: string;
}

@Component({
  selector: 'app-shell-layout',
  templateUrl: './shell-layout.component.html',
  styleUrls: ['./shell-layout.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ShellLayoutComponent implements OnInit {
  currentUser: CurrentUser | null = null;
  navItems: NavItem[] = [];

  collapsed = false;
  mobileSidebarOpen = false;

  pageTitle = 'Dashboard';
  breadcrumbs: Breadcrumb[] = [];

  notificationsOpen = false;
  profileOpen = false;
  topNotifications: ShellNotification[] = [
    {
      id: 'p1',
      title: 'Shipment update',
      body: 'LX-2017287528 departed Harare DC — ETA Bulawayo Hub tomorrow.',
      time: '5m ago',
    },
    {
      id: 'p2',
      title: 'Purchase order',
      body: 'New PO #4821 assigned to your organisation for review.',
      time: '32m ago',
    },
    {
      id: 'p3',
      title: 'Document shared',
      body: 'A compliance pack was uploaded to your workspace.',
      time: '2h ago',
    },
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly authState: AuthStateService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    readonly theme: ThemeService,
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authState.currentUser;
    if (!this.currentUser) {
      this.authService.bootstrapFromStorage();
      this.currentUser = this.authState.currentUser;
    }
    this.navItems = this.currentUser ? NAV_CONFIG[this.currentUser.orgClassification] : [];

    this.syncChromeFromUrl();
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => {
        this.syncChromeFromUrl();
        this.closeMobileSidebar();
        this.profileOpen = false;
        this.notificationsOpen = false;
        this.cdr.markForCheck();
      });
  }

  get userInitials(): string {
    const u = this.currentUser;
    if (!u) {
      return 'LX';
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
    const u = this.currentUser;
    if (!u?.roles?.length) {
      return u?.orgClassification.replace(/_/g, ' ') ?? '';
    }
    return u.roles.join(', ');
  }

  get userEmail(): string {
    return this.currentUser?.email ?? '';
  }

  get notificationCount(): number {
    return this.topNotifications.length;
  }

  trackByRoute(_index: number, item: NavItem): string {
    return item.route;
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

  dismissNotification(id: string): void {
    this.topNotifications = this.topNotifications.filter((n) => n.id !== id);
    this.cdr.markForCheck();
  }

  clearAllNotifications(): void {
    this.topNotifications = [];
    this.cdr.markForCheck();
  }

  logout(): void {
    this.profileOpen = false;
    this.notificationsOpen = false;
    this.authService.logout();
    void this.router.navigate(['/welcome']);
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

  private isNarrowViewport(): boolean {
    return typeof window !== 'undefined' && window.innerWidth <= 768;
  }

  private syncChromeFromUrl(): void {
    const url = this.router.url;
    this.pageTitle = this.resolvePageTitle(url);
    this.rebuildBreadcrumbs();
    this.cdr.markForCheck();
  }

  private resolvePageTitle(url: string): string {
    if (url.startsWith('/account')) {
      return 'My account';
    }
    if (url.startsWith('/settings')) {
      return 'Settings';
    }
    if (url.startsWith('/help')) {
      return 'Help & Support';
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
}
