import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ThemeService } from '../../../../core/services/theme.service';
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
export class LoginComponent {
  form: FormGroup;
  loading = false;
  showPass = false;
  error = '';

  readonly mockCreds: MockCredRow[] = MOCK_USERS.map((row) => ({
    label: row.user.orgClassification.replace(/_/g, ' '),
    email: row.email,
    pass: row.password,
  }));

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    readonly theme: ThemeService,
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });
    this.title.setTitle('Sign in | LX Platform');
  }

  toggleTheme(): void {
    this.theme.toggle();
    this.cdr.markForCheck();
  }

  fillMock(cred: MockCredRow): void {
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
    const { email, password } = this.form.value as { email: string; password: string };
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.authService.login(email, password).subscribe({
      next: () => {
        this.loading = false;
        this.cdr.markForCheck();
        void this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.loading = false;
        this.error = 'Invalid credentials';
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
