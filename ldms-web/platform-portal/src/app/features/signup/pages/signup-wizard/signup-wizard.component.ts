import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { OrganizationClassification } from '../../../../core/models/auth.model';
import { ThemeService } from '../../../../core/services/theme.service';
import { environment } from '../../../../../environments/environment';
import { delay, of } from 'rxjs';

export interface ClassificationOption {
  id: OrganizationClassification;
  title: string;
  description: string;
  icon: string;
  features: string[];
}

function passwordMatch(group: AbstractControl): ValidationErrors | null {
  const p = group.get('password')?.value as string | undefined;
  const c = group.get('confirmPassword')?.value as string | undefined;
  if (!p || !c) {
    return null;
  }
  return p === c ? null : { mismatch: true };
}

@Component({
  selector: 'app-signup-wizard',
  templateUrl: './signup-wizard.component.html',
  styleUrls: ['./signup-wizard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SignupWizardComponent {
  readonly steps = ['Organisation type', 'Organisation', 'Contact', 'Complete'];
  currentStep = 0;

  readonly classifications: ClassificationOption[] = [
    {
      id: 'SUPPLIER',
      title: 'Supplier',
      description: 'Supply products or services to other businesses.',
      icon: 'inventory_2',
      features: ['Inventory & orders', 'Shipments', 'Invoices'],
    },
    {
      id: 'CUSTOMER',
      title: 'Customer',
      description: 'Purchase from suppliers and track deliveries.',
      icon: 'storefront',
      features: ['Orders', 'Deliveries', 'Billing'],
    },
    {
      id: 'TRANSPORT_COMPANY',
      title: 'Transport company',
      description: 'Move goods with your fleet and drivers.',
      icon: 'local_shipping',
      features: ['Trips', 'Fleet', 'Documents'],
    },
    {
      id: 'CLEARING_AGENT',
      title: 'Clearing agent',
      description: 'Customs and regulatory clearance.',
      icon: 'fact_check',
      features: ['Clearances', 'Compliance', 'Tracking'],
    },
    {
      id: 'SERVICE_STATION',
      title: 'Service station',
      description: 'Fueling and vehicle services.',
      icon: 'local_gas_station',
      features: ['Visits', 'Fuel', 'Customers'],
    },
    {
      id: 'ROADSIDE_SUPPORT_SERVICE',
      title: 'Roadside support',
      description: 'Breakdown and emergency assistance.',
      icon: 'support_agent',
      features: ['Incidents', 'Dispatch', 'Jobs'],
    },
    {
      id: 'GOVERNMENT_AGENCY',
      title: 'Government agency',
      description: 'Oversight, border, and regulatory workflows.',
      icon: 'account_balance',
      features: ['Monitoring', 'Reviews', 'Reports'],
    },
  ];

  classification: OrganizationClassification | null = null;
  submitting = false;
  complete = false;

  orgForm: FormGroup;
  contactForm: FormGroup;

  showPassword = false;
  showConfirmPassword = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    readonly theme: ThemeService,
  ) {
    this.title.setTitle('Create account | LX Platform');
    this.orgForm = this.fb.group({
      orgName: ['', [Validators.required, Validators.minLength(2)]],
      orgEmail: ['', [Validators.required, Validators.email]],
      phone: ['', Validators.required],
      registrationRef: ['', Validators.required],
    });
    this.contactForm = this.fb.group(
      {
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required],
        acceptTerms: [false, Validators.requiredTrue],
      },
      { validators: passwordMatch },
    );
  }

  get progress(): number {
    return ((this.currentStep + 1) / this.steps.length) * 100;
  }

  selectClassification(c: OrganizationClassification): void {
    this.classification = c;
    this.cdr.markForCheck();
  }

  toggleTheme(): void {
    this.theme.toggle();
    this.cdr.markForCheck();
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
    this.cdr.markForCheck();
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
    this.cdr.markForCheck();
  }

  canAdvanceFromStep0(): boolean {
    return this.classification != null;
  }

  next(): void {
    if (this.currentStep >= 2) {
      return;
    }
    if (this.currentStep === 0 && !this.canAdvanceFromStep0()) {
      return;
    }
    if (this.currentStep === 1 && this.orgForm.invalid) {
      this.orgForm.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }
    if (this.currentStep === 2 && this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }
    if (this.currentStep < this.steps.length - 1) {
      this.currentStep++;
      this.cdr.markForCheck();
    }
  }

  back(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
      this.cdr.markForCheck();
    }
  }

  submit(): void {
    if (this.contactForm.invalid || !this.classification) {
      this.contactForm.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }
    this.submitting = true;
    this.cdr.markForCheck();
    const finish$ = environment.useMocks
      ? of(true).pipe(delay(650))
      : of(true).pipe(delay(650)); // replace with HttpClient when signup API is wired
    finish$.subscribe({
      next: () => {
        this.submitting = false;
        this.complete = true;
        this.currentStep = 3;
        this.cdr.markForCheck();
      },
      error: () => {
        this.submitting = false;
        this.cdr.markForCheck();
      },
    });
  }

  goLogin(): void {
    void this.router.navigate(['/auth/login']);
  }

  goWelcome(): void {
    void this.router.navigate(['/welcome']);
  }
}
