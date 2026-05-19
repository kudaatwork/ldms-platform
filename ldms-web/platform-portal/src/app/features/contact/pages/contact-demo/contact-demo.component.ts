import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ThemeService } from '../../../../core/services/theme.service';

@Component({
  selector: 'app-contact-demo',
  templateUrl: './contact-demo.component.html',
  styleUrls: ['./contact-demo.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ContactDemoComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly theme = inject(ThemeService);

  readonly submitting = signal(false);
  readonly submittedOk = signal(false);
  readonly submitAttempted = signal(false);

  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    phone: ['', [Validators.required, Validators.maxLength(40)]],
    address: ['', [Validators.required, Validators.maxLength(500)]],
    demoRequest: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(4000)]],
  });

  toggleTheme(): void {
    this.theme.toggle();
  }

  fieldError(field: keyof typeof this.form.controls): string | null {
    const c = this.form.get(field);
    if (!c || !c.errors || (!c.touched && !this.submitAttempted())) {
      return null;
    }
    if (c.errors['required']) {
      return 'This field is required.';
    }
    if (c.errors['email']) {
      return 'Enter a valid email address.';
    }
    if (c.errors['minlength']) {
      const min = c.errors['minlength'].requiredLength;
      return `Enter at least ${min} characters.`;
    }
    if (c.errors['maxlength']) {
      return 'This value is too long.';
    }
    return null;
  }

  async onSubmit(): Promise<void> {
    this.submitAttempted.set(true);
    this.form.markAllAsTouched();
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.submittedOk.set(false);
    try {
      await new Promise((r) => setTimeout(r, 650));
      this.submittedOk.set(true);
      this.form.reset();
      this.submitAttempted.set(false);
    } finally {
      this.submitting.set(false);
    }
  }

  goHome(): void {
    void this.router.navigate(['/welcome']);
  }

  prepareAnotherRequest(): void {
    this.submittedOk.set(false);
  }
}
