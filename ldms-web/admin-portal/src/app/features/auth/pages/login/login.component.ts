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

  readonly mockCreds = [...MOCK_DEMO_CREDENTIALS];
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
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });
    this.title.setTitle('Sign in | LX Admin');
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
    this.auth.loginWithGoogleIdToken(idToken).subscribe({
      next: () => {
        this.loading = false;
        this.cdr.markForCheck();
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/dashboard';
        void this.router.navigateByUrl(returnUrl);
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

  fillMock(cred: MockCredential): void {
    this.form.patchValue({ email: cred.email, password: cred.pass });
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
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    const { email, password } = this.form.value as { email: string; password: string };
    this.auth.login(email, password).subscribe({
      next: () => {
        this.loading = false;
        this.cdr.markForCheck();
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/dashboard';
        void this.router.navigateByUrl(returnUrl);
      },
      error: (e: Error) => {
        this.error = e.message || 'Invalid credentials';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  get emailCtrl() {
    return this.form.get('email');
  }

  get passCtrl() {
    return this.form.get('password');
  }
}
