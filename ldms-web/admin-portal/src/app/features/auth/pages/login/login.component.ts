import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, ViewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  AuthService,
  MOCK_DEMO_CREDENTIALS,
  MockCredential,
} from '@core/services/auth.service';
import { GoogleGsiService } from '@core/services/google-gsi.service';
import { ThemeService } from '@core/services/theme.service';
import { environment } from '../../../../../environments/environment';
import { resolveAdminPostLoginUrl } from '@core/utils/post-login-url.util';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LoginComponent implements AfterViewInit {
  form: FormGroup;
  loading = false;
  showPass = false;
  error = '';
  infoMessage = '';

  readonly mockCreds = [...MOCK_DEMO_CREDENTIALS];
  readonly showMockCredentials = environment.authUseMocks || environment.useMocks;
  readonly googleClientId = (environment.googleOAuthClientId ?? '').trim();

  @ViewChild('googleSignInHost', { static: false }) googleSignInHost?: ElementRef<HTMLElement>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    readonly theme: ThemeService,
    private readonly googleGsi: GoogleGsiService,
  ) {
    this.form = this.fb.group({
      usernameOrEmail: ['', Validators.required],
      password: ['', Validators.required],
    });
    this.title.setTitle('Sign in | LX Admin');
    const reason = (this.route.snapshot.queryParamMap.get('reason') ?? '').trim().toLowerCase();
    if (reason === 'inactivity') {
      this.infoMessage = 'You were signed out after inactivity. Please sign in again.';
    } else if (reason === 'logout') {
      this.infoMessage = 'You were signed out. Please sign in again.';
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
    this.beginSignIn();
    this.auth.loginWithGoogleIdToken(idToken).subscribe({
      next: () => {
        void this.navigateAfterLogin('/dashboard');
      },
      error: (e: Error) => this.failSignIn(e.message ?? 'Google sign-in failed'),
    });
  }

  toggleTheme(): void {
    this.theme.toggle();
    this.cdr.markForCheck();
  }

  fillMock(cred: MockCredential): void {
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
    this.beginSignIn();
    const { usernameOrEmail, password } = this.form.value as {
      usernameOrEmail: string;
      password: string;
    };
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    const target = resolveAdminPostLoginUrl(returnUrl);
    this.auth.login(usernameOrEmail, password).subscribe({
      next: () => {
        void this.navigateAfterLogin(target);
      },
      error: (e: Error) => this.failSignIn(e.message || 'Invalid credentials'),
    });
  }

  private beginSignIn(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
  }

  private failSignIn(message: string): void {
    this.loading = false;
    this.error = message;
    this.cdr.markForCheck();
  }

  get usernameOrEmailCtrl() {
    return this.form.get('usernameOrEmail');
  }

  get passCtrl() {
    return this.form.get('password');
  }

  private async navigateAfterLogin(target: string): Promise<void> {
    const path = (target.startsWith('/') ? target : `/${target}`).split('?')[0];
    const commands =
      !path || path === '/' ? ['dashboard'] : path.split('/').filter((segment) => segment.length > 0);

    try {
      let ok = await this.router.navigate(commands, { replaceUrl: true });
      if (!ok) {
        ok = await this.router.navigateByUrl(path.startsWith('/') ? path : `/${path}`, { replaceUrl: true });
      }
      if (!ok) {
        this.failSignIn('Could not open the app after sign-in. Refresh the page or open /dashboard.');
        return;
      }
    } catch {
      this.failSignIn('Navigation failed. Please try again.');
      return;
    }
    this.loading = false;
    this.cdr.markForCheck();
  }
}
