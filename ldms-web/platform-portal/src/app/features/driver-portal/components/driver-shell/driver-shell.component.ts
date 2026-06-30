import { ChangeDetectionStrategy, Component } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { DriverThemeService } from '../../services/driver-theme.service';

@Component({
  selector: 'app-driver-shell',
  templateUrl: './driver-shell.component.html',
  styleUrls: ['./driver-shell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DriverShellComponent {
  menuOpen = false;

  constructor(
    readonly router: Router,
    readonly theme: DriverThemeService,
    private readonly authService: AuthService,
  ) {
    // Close the drawer whenever navigation completes.
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => (this.menuOpen = false));
  }

  get isWorkspace(): boolean {
    return this.router.url.includes('/driver/workspace') || this.router.url === '/driver';
  }

  get isTripDetail(): boolean {
    return this.router.url.includes('/driver/trip/');
  }

  get isLive(): boolean {
    return this.router.url.includes('/driver/live');
  }

  get pageTitle(): string {
    const url = this.router.url;
    if (url.includes('/driver/profile')) return 'My profile';
    if (url.includes('/driver/chat')) return 'Messages';
    if (url.includes('/driver/live')) return 'Live tracking';
    if (url.includes('/driver/trip/')) return 'Trip detail';
    return 'My trips';
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
    void this.router.navigate(['/auth/login'], { queryParams: { portal: 'driver' } });
  }
}
