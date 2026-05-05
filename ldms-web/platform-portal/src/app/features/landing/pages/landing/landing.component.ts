import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router } from '@angular/router';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LandingComponent {
  readonly adminUrl = environment.adminPortalOrigin;
  readonly platformOrigin = environment.platformPortalOrigin;

  constructor(private readonly router: Router) {}

  goSignup(): void {
    void this.router.navigate(['/signup']);
  }

  goPlatformLogin(): void {
    void this.router.navigate(['/auth/login']);
  }
}
