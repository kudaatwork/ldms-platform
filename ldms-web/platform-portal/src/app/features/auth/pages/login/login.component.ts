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
    if (this.route.snapshot.queryParamMap.get('verified') === '1') {
      this.infoMessage = 'Your email is verified. Sign in to open your organisation workspace.';
    }
    if (this.route.snapshot.queryParamMap.get('registered') === '1') {
      this.infoMessage =
        'Registration received. After KYC approval, sign in with the temporary username and password emailed to your organisation and contact addresses.';
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
        void this.router.navigate(this.authService.postLoginRoute());
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
      next: () => {
        this.loading = false;
        this.cdr.markForCheck();
        void this.router.navigate(this.authService.postLoginRoute());
      },
      error: (e: Error) => {
        this.loading = false;
        this.error = e.message || 'Invalid credentials';
        this.cdr.markForCheck();
      },
    });
  }

  get usernameOrEmailCtrl() {
    return this.form.get('usernameOrEmail');
  }

  get passCtrl() {
    return this.form.get('password');
  }
}
