import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { GoogleGsiService } from '../../../../core/services/google-gsi.service';
import { ThemeService } from '../../../../core/services/theme.service';
import { environment } from '../../../../../environments/environment';
import { MOCK_USERS } from '../../../dashboard/data/platform-mock-data';

interface MockCredRow {
  label: string;
  email: string;
  pass: string;
}

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LoginComponent implements OnInit, AfterViewInit {
  form: FormGroup;
  loading = false;
  showPass = false;
  error = '';
  infoMessage = '';
  twoFactorStep = false;
  mfaChallengeToken = '';
  twoFactorOtp = '';
  twoFactorMethod: 'SMS' | 'AUTHENTICATOR_APP' = 'SMS';

  readonly googleClientId = (environment.googleOAuthClientId ?? '').trim();
  @ViewChild('googleSignInHost', { static: false }) googleSignInHost?: ElementRef<HTMLElement>;

  readonly showMockCredentials = environment.useMocks;
  readonly mockCreds: MockCredRow[] = MOCK_USERS.map((row) => ({
    label: row.user.orgClassification.replace(/_/g, ' '),
    email: row.email,
    pass: row.password,
  }));

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    readonly theme: ThemeService,
    private readonly googleGsi: GoogleGsiService,
  ) {
    this.form = this.fb.group({
      usernameOrEmail: ['', Validators.required],
      password: ['', Validators.required],
    });
    this.title.setTitle('Sign in | LX Platform');
  }

  ngOnInit(): void {
    const email = (this.route.snapshot.queryParamMap.get('email') ?? '').trim();
    if (email) {
      this.form.patchValue({ usernameOrEmail: email });
    }
    if (this.route.snapshot.queryParamMap.get('reason') === 'inactivity') {
      this.infoMessage = 'You were signed out after inactivity. Please sign in again.';
    }
    if (this.route.snapshot.queryParamMap.get('verified') === '1') {
      this.infoMessage = 'Your email is verified. Sign in to open your organisation workspace.';
    }
    if (this.route.snapshot.queryParamMap.get('registered') === '1') {
      this.infoMessage =
        'Registration received. After KYC approval, sign in with the temporary username and password emailed to your organisation and contact addresses.';
    }
  }

  /** Replace login in browser history so Back does not return to the sign-in form after entry. */
  private async navigateAfterLogin(): Promise<void> {
    const commands = this.authService.postLoginRoute();
    try {
      let ok = await this.router.navigate(commands, { replaceUrl: true });
      if (!ok) {
        const path = `/${commands.filter(Boolean).join('/')}`;
        ok = await this.router.navigateByUrl(path, { replaceUrl: true });
      }
      if (!ok) {
        this.error = 'Could not open your workspace after sign-in. Refresh the page or try again.';
        this.cdr.markForCheck();
      }
    } catch {
      this.error = 'Navigation failed. Please try again.';
      this.cdr.markForCheck();
    }
  }

  ngAfterViewInit(): void {
    if (!this.googleClientId) {
      return;
    }
    queueMicrotask(() => {
      const el = this.googleSignInHost?.nativeElement;
      if (!el) {
        return;
      }
      void this.googleGsi
        .loadScript()
        .then(() => {
          this.googleGsi.renderSignInButton(el, (idToken) => this.onGoogleCredential(idToken));
          this.cdr.markForCheck();
        })
        .catch((e: Error) => {
          this.error = e.message ?? 'Google sign-in could not be loaded';
          this.cdr.markForCheck();
        });
    });
  }

  private onGoogleCredential(idToken: string): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.authService.loginWithGoogleIdToken(idToken).subscribe({
      next: () => {
        this.loading = false;
        this.cdr.markForCheck();
        void this.navigateAfterLogin();
      },
      error: (e: Error) => {
        this.error = e.message ?? 'Google sign-in failed';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  toggleTheme(): void {
    this.theme.toggle();
    this.cdr.markForCheck();
  }

  fillMock(cred: MockCredRow): void {
    this.form.patchValue({ usernameOrEmail: cred.email, password: cred.pass });
    this.cdr.markForCheck();
  }

  togglePasswordVisibility(): void {
    this.showPass = !this.showPass;
    this.cdr.markForCheck();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }
    const { usernameOrEmail, password } = this.form.value as {
      usernameOrEmail: string;
      password: string;
    };
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.authService.login(usernameOrEmail, password).subscribe({
      next: (outcome) => {
        if (outcome.kind === 'two_factor_required') {
          this.loading = false;
          this.twoFactorStep = true;
          this.mfaChallengeToken = outcome.mfaChallengeToken;
          this.twoFactorMethod =
            outcome.twoFactorMethod === 'AUTHENTICATOR_APP' ? 'AUTHENTICATOR_APP' : 'SMS';
          this.cdr.markForCheck();
          return;
        }
        this.loading = false;
        this.cdr.markForCheck();
        void this.navigateAfterLogin();
      },
      error: (e: Error) => {
        this.loading = false;
        this.error = e.message || 'Invalid credentials';
        this.cdr.markForCheck();
      },
    });
  }

  submitTwoFactor(): void {
    const otp = this.twoFactorOtp.trim();
    if (!/^\d{6}$/.test(otp)) {
      this.error =
        this.twoFactorMethod === 'AUTHENTICATOR_APP'
          ? 'Enter the 6-digit code from your authenticator app.'
          : 'Enter the 6-digit security code from your SMS.';
      this.cdr.markForCheck();
      return;
    }
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.authService.verifyTwoFactor(this.mfaChallengeToken, otp).subscribe({
      next: () => {
        this.loading = false;
        this.twoFactorStep = false;
        this.cdr.markForCheck();
        void this.navigateAfterLogin();
      },
      error: (e: Error) => {
        this.loading = false;
        this.error = e.message || 'Invalid security code';
        this.cdr.markForCheck();
      },
    });
  }

  backToPasswordStep(): void {
    this.twoFactorStep = false;
    this.twoFactorOtp = '';
    this.mfaChallengeToken = '';
    this.error = '';
    this.cdr.markForCheck();
  }

  twoFactorMethodLabel(): string {
    return this.twoFactorMethod === 'AUTHENTICATOR_APP' ? 'Authenticator app' : 'SMS text message';
  }

  twoFactorStepHeadline(): string {
    return this.twoFactorMethod === 'AUTHENTICATOR_APP'
      ? 'Use your authenticator app'
      : 'Check your phone for a text message';
  }

  twoFactorStepInstruction(): string {
    return this.twoFactorMethod === 'AUTHENTICATOR_APP'
      ? 'Open the authenticator app you used when you set up two-step verification (for example Google Authenticator, Authy, or Microsoft Authenticator). Enter the current 6-digit code shown for your account.'
      : 'We sent a 6-digit security code by SMS to the phone number on your account. Enter that code below to complete sign-in.';
  }

  twoFactorStepHint(): string {
    return this.twoFactorMethod === 'AUTHENTICATOR_APP'
      ? 'Forgot which app? Look for an entry named LDMS or Project LX. Codes refresh every 30 seconds — use the latest one.'
      : 'Did not receive the text? Wait a minute, check signal strength, then use Back to sign in and try again to get a new code.';
  }

  twoFactorInputLabel(): string {
    return this.twoFactorMethod === 'AUTHENTICATOR_APP' ? 'Authenticator code' : 'SMS security code';
  }

  twoFactorInputPlaceholder(): string {
    return this.twoFactorMethod === 'AUTHENTICATOR_APP'
      ? '6-digit code from your app'
      : '6-digit code from your text message';
  }

  get usernameOrEmailCtrl() {
    return this.form.get('usernameOrEmail');
  }

  get passCtrl() {
    return this.form.get('password');
  }
}
