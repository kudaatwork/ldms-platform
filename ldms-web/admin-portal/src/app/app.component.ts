import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  HostListener,
  OnInit,
} from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter, tap } from 'rxjs/operators';
import { StorageService } from './core/services/storage.service';
import { ThemeService } from './core/services/theme.service';

interface NavChild {
  label: string;
  icon: string;
  route: string;
}

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number | null;
  children?: NavChild[];
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
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class AppComponent implements OnInit {
  collapsed = false;
  pageTitle = 'Dashboard';
  showShell = true;
  breadcrumbs: Breadcrumb[] = [];

  /** Expanded when URL is under /locations (accordion + auto-expand on navigation). */
  locationsExpanded = false;

  /** Snapshot for templates (e.g. sidebar active states). */
  currentUrl = '';

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'KYC Queue', icon: 'verified_user', route: '/kyc/applications', badge: 4 },
    { label: 'Organizations', icon: 'corporate_fare', route: '/organizations' },
    { label: 'Documents', icon: 'folder_open', route: '/kyc/documents' },
    { label: 'Users', icon: 'people_outline', route: '/users' },
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
    { label: 'Audit Log', icon: 'receipt_long', route: '/activity' },
    { label: 'System Health', icon: 'monitor_heart', route: '/system/health' },
  ];

  currentUser = {
    firstName: 'Platform',
    lastName: 'Admin',
    email: 'admin@projectlx.co.zw',
    role: 'Platform Admin',
    initials: 'PA',
  };

  notificationsOpen = false;
  profileOpen = false;
  topNotifications: TopNotification[] = [
    {
      id: 'n1',
      title: 'KYC application queued',
      body: 'Org “Demo Logistics” submitted documents for review.',
      time: '2m ago',
    },
    {
      id: 'n2',
      title: 'System health',
      body: 'All services reported healthy in the last poll.',
      time: '1h ago',
    },
    {
      id: 'n3',
      title: 'Locations import',
      body: 'Bulk province update finished with 12 warnings.',
      time: 'Yesterday',
    },
  ];

  constructor(
    readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly storage: StorageService,
    readonly theme: ThemeService,
  ) {}

  ngOnInit(): void {
    this.syncChromeFromUrl();
    this.rebuildBreadcrumbs();
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        tap(() => {
          this.syncChromeFromUrl();
          this.rebuildBreadcrumbs();
          this.profileOpen = false;
          this.notificationsOpen = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe();
  }

  trackByRoute(_index: number, item: NavItem): string {
    return item.route;
  }

  trackByChildRoute(_index: number, item: NavChild): string {
    return item.route;
  }

  trackByNotifId(_index: number, n: TopNotification): string {
    return n.id;
  }

  toggleLocations(): void {
    this.locationsExpanded = !this.locationsExpanded;
    this.cdr.markForCheck();
  }

  isLocationsSectionActive(url: string): boolean {
    return url.startsWith('/locations');
  }

  private syncChromeFromUrl(): void {
    const url = this.router.url;
    this.currentUrl = url;
    this.showShell = !url.startsWith('/auth');
    this.locationsExpanded = url.startsWith('/locations');
    this.pageTitle = this.resolvePageTitle(url);
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
    for (const item of this.navItems) {
      if (item.children) {
        const child = item.children.find((c) => url.startsWith(c.route));
        if (child) {
          return child.label;
        }
        if (url.startsWith(item.route)) {
          return item.children[0]?.label ?? item.label;
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
    this.cdr.markForCheck();
  }

  get notificationCount(): number {
    return this.topNotifications.length;
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

  logout(): void {
    this.profileOpen = false;
    this.notificationsOpen = false;
    this.storage.clearSession();
    void this.router.navigate(['/auth/login']);
    this.cdr.markForCheck();
  }
}
