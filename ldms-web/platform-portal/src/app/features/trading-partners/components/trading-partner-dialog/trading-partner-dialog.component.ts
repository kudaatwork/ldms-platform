import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { OrganizationService, TradingPartner, TradingPartnerPayload } from '../../../../core/services/organization.service';

export interface TradingPartnerDialogData {
  partner?: TradingPartner;
}

@Component({
  selector: 'app-trading-partner-dialog',
  templateUrl: './trading-partner-dialog.component.html',
  styleUrl: './trading-partner-dialog.component.scss',
  standalone: false,
})
export class TradingPartnerDialogComponent implements OnInit {
  form: FormGroup;
  submitting = false;
  submitError = '';

  get isEdit(): boolean {
    return !!this.data.partner;
  }

  readonly roles: Array<{ value: TradingPartner['role']; label: string }> = [
    { value: 'CUSTOMER', label: 'Customer' },
    { value: 'SUPPLIER', label: 'Supplier' },
    { value: 'TRANSPORTER', label: 'Transporter' },
    { value: 'OTHER', label: 'Other' },
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly orgService: OrganizationService,
    private readonly dialogRef: MatDialogRef<TradingPartnerDialogComponent, TradingPartner>,
    @Inject(MAT_DIALOG_DATA) public readonly data: TradingPartnerDialogData,
  ) {
    this.dialogRef.disableClose = true;
    const p = data.partner;
    this.form = this.fb.group({
      name: [p?.name ?? '', [Validators.required, Validators.maxLength(120)]],
      email: [p?.email ?? '', [Validators.email, Validators.maxLength(120)]],
      phoneNumber: [p?.phoneNumber ?? '', Validators.maxLength(30)],
      role: [p?.role ?? 'CUSTOMER', Validators.required],
      notes: [p?.notes ?? '', Validators.maxLength(500)],
    });
  }

  ngOnInit(): void {}

  hasError(controlName: string, errorName: string): boolean {
    const ctrl = this.form.get(controlName);
    return !!ctrl && ctrl.hasError(errorName) && (ctrl.touched || ctrl.dirty);
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.submitError = '';

    const raw = this.form.value;
    const payload: TradingPartnerPayload = {
      name: String(raw['name']).trim(),
      email: String(raw['email'] ?? '').trim() || undefined,
      phoneNumber: String(raw['phoneNumber'] ?? '').trim() || undefined,
      role: raw['role'] as TradingPartner['role'],
      notes: String(raw['notes'] ?? '').trim() || undefined,
    };

    const request$ = this.isEdit
      ? this.orgService.updateTradingPartner(this.data.partner!.id, payload)
      : this.orgService.createTradingPartner(payload);

    request$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (partner) => this.dialogRef.close(partner),
        error: (err: Error) => (this.submitError = err.message ?? 'Could not save trading partner.'),
      });
  }
}
