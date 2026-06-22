import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { DemoRequisitionService } from '../../services/demo-requisition.service';
import { LandingMotionService } from '../../../landing/services/landing-motion.service';

@Component({
  selector: 'app-contact-demo',
  templateUrl: './contact-demo.component.html',
  styleUrls: ['./contact-demo.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ContactDemoComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly demoRequisition = inject(DemoRequisitionService);
  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly motion = inject(LandingMotionService);

  readonly submitting = signal(false);
  readonly submittedOk = signal(false);
  readonly submitAttempted = signal(false);
  readonly submitError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    phone: ['', [Validators.required, Validators.maxLength(40)]],
    address: ['', [Validators.required, Validators.maxLength(500)]],
    demoRequest: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(4000)]],
  });

  ngAfterViewInit(): void {
    this.motion.initHeroReveal(
      this.el.nativeElement,
      '.ldms-contact__eyebrow, .ldms-contact__title, .ldms-contact__intro, .ldms-contact__perks, .ldms-contact__panel',
    );
  }

  demoRequestLength(): number {
    return (this.form.get('demoRequest')?.value ?? '').length;
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
    this.submitError.set(null);
    const value = this.form.getRawValue();
    try {
      await firstValueFrom(
        this.demoRequisition.submit({
          fullName: value.fullName.trim(),
          email: value.email.trim(),
          phone: value.phone.trim(),
          address: value.address.trim(),
          demoRequest: value.demoRequest.trim(),
        }),
      );
      this.submittedOk.set(true);
      this.form.reset();
      this.submitAttempted.set(false);
    } catch (err: unknown) {
      this.submitError.set(err instanceof Error ? err.message : 'Could not submit your demo request.');
    } finally {
      this.submitting.set(false);
    }
  }

  prepareAnotherRequest(): void {
    this.submittedOk.set(false);
    this.submitError.set(null);
  }
}
