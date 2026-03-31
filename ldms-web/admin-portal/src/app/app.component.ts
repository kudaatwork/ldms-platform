import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter, tap } from 'rxjs/operators';
import { StorageService } from './core/services/storage.service';
import { ThemeService } from './core/services/theme.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number | null;
}

interface Breadcrumb {
  label: string;
  url: string;
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

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'KYC Queue', icon: 'verified_user', route: '/kyc/applications', badge: 4 },
    { label: 'Organizations', icon: 'corporate_fare', route: '/organizations' },
    { label: 'Documents', icon: 'folder_open', route: '/kyc/documents' },
    { label: 'Users', icon: 'people_outline', route: '/users' },
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

  constructor(
    private readonly router: Router,
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
        }),
      )
      .subscribe();
  }

  private syncChromeFromUrl(): void {
    const url = this.router.url;
    this.showShell = !url.startsWith('/auth');
    const match = this.navItems.find((n) => url.startsWith(n.route));
    this.pageTitle = match?.label ?? 'Dashboard';
    this.cdr.markForCheck();
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

  trackByRoute(_index: number, item: NavItem): string {
    return item.route;
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
    this.cdr.markForCheck();
  }

  logout(): void {
    this.storage.clearSession();
    void this.router.navigate(['/auth/login']);
  }
}
