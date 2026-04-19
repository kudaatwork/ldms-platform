import { ChangeDetectionStrategy, ChangeDetectorRef, Component, HostListener, OnInit } from '@angular/core';
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

  trackByRoute(_index: number, item: NavItem): string {
    return item.route;
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

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/auth/login']);
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
