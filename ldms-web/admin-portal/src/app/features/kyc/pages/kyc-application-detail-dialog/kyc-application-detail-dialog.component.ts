import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import type {
  KycApplicationDecisionResult,
  KycApplicationDetail,
} from '../../models/kyc-application.model';

export interface KycApplicationDetailDialogData {
  detail: KycApplicationDetail;
}

@Component({
  selector: 'app-kyc-application-detail-dialog',
  templateUrl: './kyc-application-detail-dialog.component.html',
  styleUrl: './kyc-application-detail-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
  ],
})
export class KycApplicationDetailDialogComponent {
  readonly form: FormGroup;

  reasonError = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<
      KycApplicationDetailDialogComponent,
      KycApplicationDecisionResult | undefined
    >,
    @Inject(MAT_DIALOG_DATA) readonly data: KycApplicationDetailDialogData,
  ) {
    this.form = this.fb.group({
      reason: [''],
    });
  }

  get detail(): KycApplicationDetail {
    return this.data.detail;
  }

  get canDecide(): boolean {
    const s = this.detail.status.toLowerCase();
    return s !== 'approved' && s !== 'rejected';
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  private validateReason(): string | null {
    this.reasonError = '';
    const reason = (this.form.value.reason ?? '').trim();
    if (reason.length < 8) {
      this.reasonError =
        'Please enter a reason or notes for this decision (at least 8 characters).';
      this.form.get('reason')?.markAsTouched();
      return null;
    }
    return reason;
  }

  approve(): void {
    const reason = this.validateReason();
    if (reason === null) {
      return;
    }
    this.dialogRef.close({ decision: 'approve', reason });
  }

  reject(): void {
    const reason = this.validateReason();
    if (reason === null) {
      return;
    }
    this.dialogRef.close({ decision: 'reject', reason });
  }

  viewDocument(docName: string): void {
    this.snackBar.open(`Preview for “${docName}” would open here (wire storage URL).`, 'OK', {
      duration: 4500,
    });
  }
}
