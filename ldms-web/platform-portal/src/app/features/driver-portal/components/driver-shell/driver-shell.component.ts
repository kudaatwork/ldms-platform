import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-driver-shell',
  templateUrl: './driver-shell.component.html',
  styleUrls: ['./driver-shell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DriverShellComponent {
  constructor(
    readonly router: Router,
    private readonly authService: AuthService,
  ) {}

  get isWorkspace(): boolean {
    return this.router.url.includes('/driver/workspace') || this.router.url === '/driver';
  }

  get isTripDetail(): boolean {
    return this.router.url.includes('/driver/trip/');
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/auth/login'], { queryParams: { portal: 'driver' } });
  }
}
