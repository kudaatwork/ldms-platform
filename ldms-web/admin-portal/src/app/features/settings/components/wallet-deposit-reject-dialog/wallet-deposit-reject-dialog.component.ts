import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import type { WalletDepositRow } from '../../services/platform-wallet-admin.service';

export interface WalletDepositRejectDialogData {
  deposit: WalletDepositRow;
}

@Component({
  selector: 'app-wallet-deposit-reject-dialog',
  templateUrl: './wallet-deposit-reject-dialog.component.html',
  styleUrl: './wallet-deposit-reject-dialog.component.scss',
  standalone: false,
})
export class WalletDepositRejectDialogComponent {
  readonly form: FormGroup;
  reasonError = '';
  readonly minReasonLength = 10;

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<WalletDepositRejectDialogComponent, string | null>,
    @Inject(MAT_DIALOG_DATA) readonly data: WalletDepositRejectDialogData,
  ) {
    this.form = this.fb.group({
      reason: ['', [Validators.required, Validators.minLength(this.minReasonLength)]],
    });
  }

  get deposit(): WalletDepositRow {
    return this.data.deposit;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  submit(): void {
    this.reasonError = '';
    const reason = String(this.form.value.reason ?? '').trim();
    if (reason.length < this.minReasonLength) {
      this.reasonError = `Enter at least ${this.minReasonLength} characters explaining why this deposit is being rejected.`;
      this.form.get('reason')?.markAsTouched();
      return;
    }
    this.dialogRef.close(reason);
  }
}
