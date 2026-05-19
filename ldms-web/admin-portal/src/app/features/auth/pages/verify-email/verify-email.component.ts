import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { EmailVerificationService } from '@core/services/email-verification.service';
import { ThemeService } from '@core/services/theme.service';

@Component({
  selector: 'app-verify-email',
  templateUrl: './verify-email.component.html',
  styleUrls: ['../login/login.component.scss', './verify-email.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class VerifyEmailComponent implements OnInit {
  verifying = true;
  /** Fresh verification completed on this visit. */
  success = false;
  /** Account was already verified before this link was opened. */
  alreadyVerified = false;
  message = '';
  error = '';

  constructor(
    private readonly verification: EmailVerificationService,
    private readonly route: ActivatedRoute,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    readonly theme: ThemeService,
  ) {
    this.title.setTitle('Verify email | LX Admin');
  }

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const token = params.get('token') ?? '';
    const email = params.get('email') ?? '';
    if (!token || !email) {
      this.verifying = false;
      this.error =
        'This page needs a verification token and email from your link. Open the link from your registration email.';
      this.cdr.markForCheck();
      return;
    }
    this.verification.verifyEmail(token, email).subscribe({
      next: (resp) => {
        this.verifying = false;
        const outcome = resp.emailVerificationOutcome?.toUpperCase();
        const msg = resp.message ?? '';
        this.alreadyVerified =
          outcome === 'ALREADY_VERIFIED' ||
          (resp.isSuccess !== false && /already verified/i.test(msg));
        this.success = resp.isSuccess !== false && !this.alreadyVerified;
        this.message = msg || (this.alreadyVerified
          ? 'This email address is already verified.'
          : this.success
            ? 'Your email has been verified.'
            : 'Verification failed.');
        this.error = this.success || this.alreadyVerified ? '' : this.message;
        this.cdr.markForCheck();
      },
      error: (e: Error) => {
        this.verifying = false;
        this.success = false;
        this.alreadyVerified = false;
        this.error = e.message ?? 'We could not verify your email.';
        this.cdr.markForCheck();
      },
    });
  }

  toggleTheme(): void {
    this.theme.toggle();
    this.cdr.markForCheck();
  }
}
