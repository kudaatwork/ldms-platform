import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { CredentialsSetupService } from '../../../../core/services/credentials-setup.service';
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
  selector: 'app-setup-credentials',
  templateUrl: './setup-credentials.component.html',
  styleUrls: ['../login/login.component.scss', './setup-credentials.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SetupCredentialsComponent {
  form: FormGroup;
  loading = false;
  error = '';
  showPass = false;
  showPass2 = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly credentialsSetup: CredentialsSetupService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    readonly theme: ThemeService,
  ) {
    this.form = this.fb.group(
      {
        newUsername: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required],
      },
      { validators: passwordsMatch },
    );
    this.title.setTitle('Set up your account | LX Platform');
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
    const { newUsername, newPassword, confirmPassword } = this.form.value as {
      newUsername: string;
      newPassword: string;
      confirmPassword: string;
    };
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.credentialsSetup
      .completeSetup({
        newUsername: newUsername.trim(),
        newPassword,
        confirmPassword,
      })
      .subscribe({
        next: () => {
          this.authService.login(newUsername.trim(), newPassword).subscribe({
            next: () => {
              this.loading = false;
              this.cdr.markForCheck();
              void this.router.navigate(this.authService.postLoginRoute());
            },
            error: (e: Error) => {
              this.loading = false;
              this.error =
                e.message ??
                'Credentials were saved. Sign in again with your new username and password.';
              this.cdr.markForCheck();
            },
          });
        },
        error: (e: Error) => {
          this.loading = false;
          this.error = e.message ?? 'Could not save your credentials';
          this.cdr.markForCheck();
        },
      });
  }

  get usernameCtrl() {
    return this.form.get('newUsername');
  }

  get newPass() {
    return this.form.get('newPassword');
  }

  get confirm() {
    return this.form.get('confirmPassword');
  }
}
