import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PasswordRecoveryService } from '@core/services/password-recovery.service';
import { ThemeService } from '@core/services/theme.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['../login/login.component.scss', './forgot-password.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ForgotPasswordComponent {
  form: FormGroup;
  loading = false;
  error = '';
  sent = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly recovery: PasswordRecoveryService,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    readonly theme: ThemeService,
  ) {
    this.form = this.fb.group({
      usernameOrEmail: ['', Validators.required],
    });
    this.title.setTitle('Forgot password | LX Admin');
  }

  toggleTheme(): void {
    this.theme.toggle();
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
    const { usernameOrEmail } = this.form.value as { usernameOrEmail: string };
    this.recovery.forgotPassword(usernameOrEmail).subscribe({
      next: () => {
        this.sent = true;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (e: Error) => {
        this.error = e.message ?? 'Request failed';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  get idCtrl() {
    return this.form.get('usernameOrEmail');
  }
}
