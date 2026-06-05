import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ldmsApiUrl } from '../../../../core/utils/api-url.util';

@Component({
  selector: 'app-verify-organization-email',
  templateUrl: './verify-organization-email.component.html',
  styleUrl: './verify-organization-email.component.scss',
  standalone: false,
})
export class VerifyOrganizationEmailComponent implements OnInit {
  loading = true;
  outcome: 'verified' | 'already' | 'error' = 'error';
  message = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly http: HttpClient,
    private readonly title: Title,
  ) {
    this.title.setTitle('Verify organisation email | LX Platform');
  }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    const email = this.route.snapshot.queryParamMap.get('email') ?? '';
    if (!token || !email) {
      this.loading = false;
      this.message = 'This verification link is incomplete. Open the link from your email again.';
      return;
    }
    const url = ldmsApiUrl('/ldms-organization-management/v1/system/organization/verify-email');
    this.http
      .post<unknown>(url, null, { params: { token, email } })
      .subscribe({
        next: (body) => {
          const root = body as Record<string, unknown>;
          const data = (root['data'] as Record<string, unknown>) ?? root;
          const success = Boolean(data['success'] ?? root['success']);
          if (!success) {
            this.message =
              String(data['message'] ?? root['message'] ?? 'We could not verify this link. It may have expired.');
            this.loading = false;
            return;
          }
          const responseMessage = String(data['message'] ?? root['message'] ?? '');
          this.outcome = responseMessage.toLowerCase().includes('already') ? 'already' : 'verified';
          this.message =
            this.outcome === 'already'
              ? 'This organisation email was already verified. The contact person can sign in to the platform.'
              : 'Organisation email verified. Your organisation is now active on Project LX.';
          this.loading = false;
        },
        error: (err) => {
          const errBody = err?.error as Record<string, unknown> | undefined;
          const nested = (errBody?.['data'] as Record<string, unknown>) ?? errBody;
          this.message = String(
            nested?.['message'] ?? errBody?.['message'] ?? 'We could not verify this link. It may have expired.',
          );
          this.loading = false;
        },
      });
  }

  goLogin(): void {
    void this.router.navigate(['/auth/login']);
  }
}
