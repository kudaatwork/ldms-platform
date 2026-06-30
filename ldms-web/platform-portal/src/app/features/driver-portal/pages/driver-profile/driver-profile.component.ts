import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from '@angular/core';
import { DriverPortalService } from '../../services/driver-portal.service';
import {
  DriverProfileDto,
  DriverProfileEditRequest,
} from '../../models/driver-portal.model';

@Component({
  selector: 'app-driver-profile',
  templateUrl: './driver-profile.component.html',
  styleUrls: ['./driver-profile.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DriverProfileComponent implements OnInit {
  loading = true;
  saving = false;
  editing = false;
  error = '';
  success = '';

  profile: DriverProfileDto | null = null;
  form: DriverProfileEditRequest = this.emptyForm();

  constructor(
    private readonly driverService: DriverPortalService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  get initials(): string {
    const f = this.profile?.firstName?.[0] ?? '';
    const l = this.profile?.lastName?.[0] ?? '';
    return (f + l).toUpperCase() || 'D';
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.driverService.getMyDriverProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (e: Error) => {
        this.error = e.message;
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  startEdit(): void {
    if (!this.profile) {
      return;
    }
    this.form = {
      firstName: this.profile.firstName ?? '',
      lastName: this.profile.lastName ?? '',
      phoneNumber: this.profile.phoneNumber ?? '',
      licenseNumber: this.profile.licenseNumber ?? '',
      licenseClass: this.profile.licenseClass ?? '',
      nationalIdNumber: this.profile.nationalIdNumber ?? '',
      addressLine1: this.profile.addressLine1 ?? '',
      addressLine2: this.profile.addressLine2 ?? '',
      addressCity: this.profile.addressCity ?? '',
      addressProvince: this.profile.addressProvince ?? '',
      addressPostalCode: this.profile.addressPostalCode ?? '',
      addressCountry: this.profile.addressCountry ?? '',
    };
    this.editing = true;
    this.success = '';
    this.error = '';
    this.cdr.markForCheck();
  }

  cancelEdit(): void {
    this.editing = false;
    this.error = '';
    this.cdr.markForCheck();
  }

  save(): void {
    if (!this.form.firstName?.trim() || !this.form.lastName?.trim()) {
      this.error = 'First and last name are required.';
      this.cdr.markForCheck();
      return;
    }
    if (!this.form.licenseNumber?.trim()) {
      this.error = 'License number is required.';
      this.cdr.markForCheck();
      return;
    }
    this.saving = true;
    this.error = '';
    this.cdr.markForCheck();
    this.driverService.updateMyDriverProfile(this.form).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.saving = false;
        this.editing = false;
        this.success = 'Profile updated.';
        this.cdr.markForCheck();
      },
      error: (e: Error) => {
        this.error = e.message;
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  private emptyForm(): DriverProfileEditRequest {
    return {
      firstName: '',
      lastName: '',
      phoneNumber: '',
      licenseNumber: '',
      licenseClass: '',
      nationalIdNumber: '',
      addressLine1: '',
      addressLine2: '',
      addressCity: '',
      addressProvince: '',
      addressPostalCode: '',
      addressCountry: '',
    };
  }
}
