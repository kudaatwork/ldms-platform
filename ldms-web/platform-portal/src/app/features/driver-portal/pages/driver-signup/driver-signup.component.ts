import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError, finalize, of } from 'rxjs';
import { ldmsServiceUrl } from '../../../../core/utils/api-url.util';
import { ThemeService } from '../../../../core/services/theme.service';

type DriverMode = 'company' | 'freelance';

type SignupDocSlotKey =
  | 'nationalIdFront'
  | 'nationalIdBack'
  | 'licenseFront'
  | 'licenseBack';

interface SignupDocSlot {
  key: SignupDocSlotKey;
  label: string;
  documentSlot: string;
  file: File | null;
  fileName: string;
  uploadId: number | null;
  uploading: boolean;
  error: string;
}

@Component({
  selector: 'app-driver-signup',
  templateUrl: './driver-signup.component.html',
  styleUrls: ['./driver-signup.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DriverSignupComponent implements OnInit {
  form: FormGroup;
  step: 'form' | 'success' = 'form';
  submitting = false;
  error = '';

  /** Whether the driver is joining freelance (no company code required). */
  driverMode: DriverMode = 'company';

  readonly licenseClasses = ['Code 7', 'Code 8', 'Code 10', 'Code 14', 'Code EC', 'Other'];

  /** Groups staging document uploads before submit. */
  readonly stagingSessionId = Date.now();

  docSlots: SignupDocSlot[] = [
    {
      key: 'nationalIdFront',
      label: 'National ID — front',
      documentSlot: 'NATIONAL_ID_FRONT',
      file: null,
      fileName: '',
      uploadId: null,
      uploading: false,
      error: '',
    },
    {
      key: 'nationalIdBack',
      label: 'National ID — back',
      documentSlot: 'NATIONAL_ID_BACK',
      file: null,
      fileName: '',
      uploadId: null,
      uploading: false,
      error: '',
    },
    {
      key: 'licenseFront',
      label: 'Driving licence — front',
      documentSlot: 'LICENSE_FRONT',
      file: null,
      fileName: '',
      uploadId: null,
      uploading: false,
      error: '',
    },
    {
      key: 'licenseBack',
      label: 'Driving licence — back',
      documentSlot: 'LICENSE_BACK',
      file: null,
      fileName: '',
      uploadId: null,
      uploading: false,
      error: '',
    },
  ];

  private readonly fleetBase = ldmsServiceUrl('fleet-management', 'fleet', undefined, 'frontend');

  constructor(
    private readonly fb: FormBuilder,
    private readonly http: HttpClient,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    readonly theme: ThemeService,
  ) {
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[\d\s\-()]{7,15}$/)]],
      nationalIdNumber: ['', [Validators.required, Validators.minLength(4)]],
      licenseNumber: ['', [Validators.required, Validators.minLength(4)]],
      licenseClass: ['', Validators.required],
      companyCode: ['', [Validators.required, Validators.minLength(4)]],
    });
  }

  ngOnInit(): void {}

  get isFreelance(): boolean {
    return this.driverMode === 'freelance';
  }

  selectMode(mode: DriverMode): void {
    this.driverMode = mode;
    const companyCodeCtrl = this.form.get('companyCode');
    if (!companyCodeCtrl) return;
    if (mode === 'freelance') {
      companyCodeCtrl.clearValidators();
      companyCodeCtrl.setValue('');
    } else {
      companyCodeCtrl.setValidators([Validators.required, Validators.minLength(4)]);
    }
    companyCodeCtrl.updateValueAndValidity();
    this.cdr.markForCheck();
  }

  onDocSelected(slot: SignupDocSlot, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) return;

    slot.file = file;
    slot.fileName = file.name;
    slot.uploadId = null;
    slot.error = '';
    slot.uploading = true;
    this.cdr.markForCheck();

    const form = new FormData();
    form.append('file', file, file.name);

    this.http
      .post<Record<string, unknown>>(
        `${this.fleetBase}/drivers/signup-request/document-upload?stagingSessionId=${this.stagingSessionId}&documentSlot=${slot.documentSlot}`,
        form,
      )
      .pipe(
        catchError((err) => {
          slot.error = err?.error?.message ?? 'Upload failed. Try again.';
          slot.file = null;
          slot.fileName = '';
          return of(null);
        }),
        finalize(() => {
          slot.uploading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe((res) => {
        if (!res) return;
        const envelope = (res['fileUploadDto'] ?? res) as Record<string, unknown>;
        const id = Number(envelope['id'] ?? res['id'] ?? 0);
        if (id > 0) {
          slot.uploadId = id;
        } else {
          slot.error = 'Upload did not return a file id.';
          slot.file = null;
          slot.fileName = '';
        }
        this.cdr.markForCheck();
      });
  }

  clearDoc(slot: SignupDocSlot): void {
    slot.file = null;
    slot.fileName = '';
    slot.uploadId = null;
    slot.error = '';
    this.cdr.markForCheck();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const docError = this.validateDocuments();
    if (docError) {
      this.error = docError;
      this.cdr.markForCheck();
      return;
    }

    this.submitting = true;
    this.error = '';
    this.cdr.markForCheck();

    const v = this.form.value;
    const payload: Record<string, unknown> = {
      firstName: v.firstName,
      lastName: v.lastName,
      email: v.email,
      phoneNumber: v.phoneNumber,
      nationalIdNumber: v.nationalIdNumber,
      licenseNumber: v.licenseNumber,
      licenseClass: v.licenseClass,
      stagingSessionId: this.stagingSessionId,
      nationalIdFrontUploadId: this.docSlots.find((s) => s.key === 'nationalIdFront')?.uploadId,
      nationalIdBackUploadId: this.docSlots.find((s) => s.key === 'nationalIdBack')?.uploadId,
      licenseFrontUploadId: this.docSlots.find((s) => s.key === 'licenseFront')?.uploadId,
      licenseBackUploadId: this.docSlots.find((s) => s.key === 'licenseBack')?.uploadId,
    };
    if (!this.isFreelance) {
      payload['companyCode'] = v.companyCode;
    }

    this.http
      .post(`${this.fleetBase}/drivers/signup-request`, payload)
      .pipe(
        catchError((err) => {
          this.error = err?.error?.message ?? 'Signup request failed. Please try again.';
          this.submitting = false;
          this.cdr.markForCheck();
          return of(null);
        }),
      )
      .subscribe((res) => {
        if (res !== null) {
          this.step = 'success';
        }
        this.submitting = false;
        this.cdr.markForCheck();
      });
  }

  private validateDocuments(): string | null {
    for (const slot of this.docSlots) {
      if (slot.uploading) {
        return 'Please wait for all document uploads to finish.';
      }
      if (!slot.uploadId) {
        return `Upload ${slot.label.toLowerCase()} before submitting.`;
      }
    }
    return null;
  }

  goToLogin(): void {
    void this.router.navigate(['/auth/login']);
  }

  get f() {
    return this.form.controls;
  }
}
