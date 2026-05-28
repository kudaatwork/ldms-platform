import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ldmsApiUrl } from '../../../../core/utils/api-url.util';

@Component({
  selector: 'app-verify-email',
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss',
  standalone: false,
})
export class VerifyEmailComponent implements OnInit {
  loading = true;
  outcome: 'verified' | 'already' | 'error' = 'error';
  message = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly http: HttpClient,
    private readonly title: Title,
  ) {
    this.title.setTitle('Verify email | LX Platform');
  }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    const email = this.route.snapshot.queryParamMap.get('email') ?? '';
    if (!token || !email) {
      this.loading = false;
      this.message = 'This verification link is incomplete. Open the link from your email again.';
      return;
    }
    const url = ldmsApiUrl('/ldms-user-management/v1/system/user/verify-email');
    this.http
      .get<unknown>(url, { params: { token, email } })
      .subscribe({
        next: (body) => {
          const root = body as Record<string, unknown>;
          const data = (root['data'] as Record<string, unknown>) ?? root;
          const outcome = String(data['emailVerificationOutcome'] ?? 'VERIFIED');
          this.outcome = outcome === 'ALREADY_VERIFIED' ? 'already' : 'verified';
          this.message =
            this.outcome === 'already'
              ? 'Your email was already verified. You can sign in to the platform.'
              : 'Your email is verified. Your organisation can now use the platform portal.';
          this.loading = false;
        },
        error: () => {
          this.message = 'We could not verify this link. It may have expired — request a new link after signing in.';
          this.loading = false;
        },
      });
  }

  goLogin(): void {
    void this.router.navigate(['/auth/login']);
  }
}
