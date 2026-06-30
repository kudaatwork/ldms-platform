import { ChangeDetectionStrategy, Component } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { ThemeService } from '../../../../core/services/theme.service';

@Component({
  selector: 'app-clerk-shell',
  templateUrl: './clerk-shell.component.html',
  styleUrls: ['./clerk-shell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ClerkShellComponent {
  menuOpen = false;

  constructor(
    readonly router: Router,
    readonly theme: ThemeService,
    private readonly authService: AuthService,
  ) {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => (this.menuOpen = false));
  }

  get isWorkspace(): boolean {
    return this.router.url.includes('/clerk/workspace') || this.router.url === '/clerk';
  }

  get pageTitle(): string {
    const url = this.router.url;
    if (url.includes('/clerk/stock-receive/')) return 'Receive stock';
    return 'Incoming deliveries';
  }

  openMenu(): void {
    this.menuOpen = true;
  }

  closeMenu(): void {
    this.menuOpen = false;
  }

  toggleTheme(): void {
    this.theme.toggle();
  }

  logout(): void {
    this.menuOpen = false;
    this.authService.logout();
    void this.router.navigate(['/auth/login'], { queryParams: { portal: 'clerk' } });
  }
}
