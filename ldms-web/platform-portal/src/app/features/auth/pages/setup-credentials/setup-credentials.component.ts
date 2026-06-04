import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { CredentialsSetupService } from '../../../../core/services/credentials-setup.service';
import { ThemeService } from '../../../../core/services/theme.service';
import {
  LDMS_USERNAME_INVALID_MESSAGE,
  isLdmsUsernameValid,
  ldmsUsernameFormatValidator,
} from '../../../../core/utils/ldms-username.util';
import {
  LDMS_PASSWORD_INVALID_MESSAGE,
  ldmsPasswordFormatValidator,
} from '../../../../core/utils/ldms-password.util';
import { Subject, of } from 'rxjs';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  filter,
  map,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs/operators';

function passwordsMatch(c: AbstractControl): ValidationErrors | null {
  const np = c.get('newPassword')?.value as string | undefined;
  const cp = c.get('confirmPassword')?.value as string | undefined;
  if (!np || !cp) {
    return null;
  }
  return np === cp ? null : { mismatch: true };
}

export type UsernameAvailabilityUi = 'idle' | 'checking' | 'available' | 'taken';

@Component({
  selector: 'app-setup-credentials',
  templateUrl: './setup-credentials.component.html',
  styleUrls: ['../login/login.component.scss', './setup-credentials.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SetupCredentialsComponent implements OnInit, OnDestroy {
  readonly usernameFormatHint = LDMS_USERNAME_INVALID_MESSAGE;
  readonly passwordFormatHint = LDMS_PASSWORD_INVALID_MESSAGE;
  readonly usernameUniquenessInfo =
    'Your username must be unique across the platform. If it is already taken, you will be asked to choose another.';

  form: FormGroup;
  loading = false;
  error = '';
  showPass = false;
  showPass2 = false;
  usernameAvailability: UsernameAvailabilityUi = 'idle';

  private readonly destroy$ = new Subject<void>();

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
        newUsername: ['', [Validators.required, ldmsUsernameFormatValidator()]],
        newPassword: ['', [Validators.required, ldmsPasswordFormatValidator()]],
        confirmPassword: ['', Validators.required],
      },
      { validators: passwordsMatch },
    );
    this.title.setTitle('Set up your account | LX Platform');
  }

  ngOnInit(): void {
    const usernameControl = this.form.get('newUsername');
    if (!usernameControl) {
      return;
    }
    usernameControl.valueChanges
      .pipe(
        debounceTime(400),
        map((value) => String(value ?? '').trim()),
        distinctUntilChanged(),
        tap((value) => {
          this.clearUsernameTakenError();
          if (!value || !isLdmsUsernameValid(value)) {
            this.usernameAvailability = 'idle';
            this.cdr.markForCheck();
          }
        }),
        filter((value) => value.length > 0 && isLdmsUsernameValid(value)),
        tap(() => {
          this.usernameAvailability = 'checking';
          this.cdr.markForCheck();
        }),
        switchMap((value) =>
          this.credentialsSetup.checkUsernameAvailability(value).pipe(
            map((available) => ({ value, available })),
            catchError(() => of({ value, available: false })),
          ),
        ),
        takeUntil(this.destroy$),
      )
      .subscribe(({ value, available }) => {
        const current = String(usernameControl.value ?? '').trim();
        if (current !== value) {
          return;
        }
        this.usernameAvailability = available ? 'available' : 'taken';
        if (!available) {
          this.setUsernameTakenError();
        } else {
          this.clearUsernameTakenError();
        }
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
    if (this.usernameAvailability === 'taken' || this.usernameAvailability === 'checking') {
      this.usernameCtrl?.markAsTouched();
      this.cdr.markForCheck();
      return;
    }
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

  get submitDisabled(): boolean {
    return (
      this.loading ||
      this.usernameAvailability === 'checking' ||
      this.usernameAvailability === 'taken'
    );
  }

  private setUsernameTakenError(): void {
    const control = this.usernameCtrl;
    if (!control) {
      return;
    }
    const errors = { ...(control.errors ?? {}), usernameTaken: true };
    control.setErrors(errors);
  }

  private clearUsernameTakenError(): void {
    const control = this.usernameCtrl;
    if (!control?.hasError('usernameTaken')) {
      return;
    }
    const errors = { ...control.errors } as ValidationErrors;
    delete errors['usernameTaken'];
    control.setErrors(Object.keys(errors).length ? errors : null);
  }
}
