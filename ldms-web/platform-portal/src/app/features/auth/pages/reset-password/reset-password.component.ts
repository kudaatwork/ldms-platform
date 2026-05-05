import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { PasswordRecoveryService } from '../../../../core/services/password-recovery.service';
import { ThemeService } from '../../../../core/services/theme.service';

function passwordsMatch(c: AbstractControl): ValidationErrors | null {
  const np = c.get('newPassword')?.value as string | undefined;
  const cp = c.get('confirmPassword')?.value as string | undefined;
  if (!np || !cp) {
    return null;
  }
  return np === cp ? null : { mismatch: true };
}

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['../login/login.component.scss', './reset-password.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ResetPasswordComponent implements OnInit {
  form: FormGroup;
  loading = false;
  validating = true;
  tokenValid = false;
  tokenError = '';
  error = '';
  done = false;
  showPass = false;
  showPass2 = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly recovery: PasswordRecoveryService,
    private readonly route: ActivatedRoute,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    readonly theme: ThemeService,
  ) {
    this.form = this.fb.group(
      {
        token: ['', Validators.required],
        email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required],
      },
      { validators: passwordsMatch },
    );
    this.title.setTitle('Reset password | LX Platform');
  }

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const token = params.get('token') ?? '';
    const email = params.get('email') ?? '';
    this.form.patchValue({ token, email });
    if (!token || !email) {
      this.validating = false;
      this.tokenValid = false;
      this.tokenError =
        'This page needs a reset token and email from your link. Open the reset link from your email, or request a new link below.';
      this.cdr.markForCheck();
      return;
    }
    this.recovery.validateResetToken(token, email).subscribe({
      next: () => {
        this.validating = false;
        this.tokenValid = true;
        this.tokenError = '';
        this.cdr.markForCheck();
      },
      error: (e: Error) => {
        this.validating = false;
        this.tokenValid = false;
        this.tokenError = e.message ?? 'This reset link is invalid or has expired.';
        this.cdr.markForCheck();
      },
    });
  }

  toggleTheme(): void {
    this.theme.toggle();
    this.cdr.markForCheck();
  }

  togglePass(): void {
    this.showPass = !this.showPass;
    this.cdr.markForCheck();
  }

  togglePass2(): void {
    this.showPass2 = !this.showPass2;
    this.cdr.markForCheck();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }
    const raw = this.form.getRawValue() as {
      token: string;
      email: string;
      newPassword: string;
      confirmPassword: string;
    };
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.recovery
      .resetPassword({
        token: raw.token,
        email: raw.email,
        newPassword: raw.newPassword,
        confirmPassword: raw.confirmPassword,
      })
      .subscribe({
        next: () => {
          this.done = true;
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (e: Error) => {
          this.error = e.message ?? 'Reset failed';
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
  }

  get newPass() {
    return this.form.get('newPassword');
  }

  get confirm() {
    return this.form.get('confirmPassword');
  }
}
