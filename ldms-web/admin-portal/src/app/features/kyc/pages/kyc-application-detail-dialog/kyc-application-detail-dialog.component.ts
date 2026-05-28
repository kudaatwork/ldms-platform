import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { StorageService } from '../../../../core/services/storage.service';
import { OrganizationDocumentsPanelComponent } from '../../../../shared/components/organization-documents-panel/organization-documents-panel.component';
import type {
  KycApplicationDecisionResult,
  KycApplicationDetail,
  KycApproverAssignment,
} from '../../../organizations/models/organization.model';

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
    MatIconModule,
    OrganizationDocumentsPanelComponent,
  ],
})
export class KycApplicationDetailDialogComponent {
  readonly form: FormGroup;

  reasonError = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly storage: StorageService,
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

  get canStage1(): boolean {
    return this.detail.kycStage === 'stage1' && this.isAssignedForStage(1);
  }

  get canStage2(): boolean {
    return this.detail.kycStage === 'stage2' && this.isAssignedForStage(2);
  }

  get acceptButtonLabel(): string {
    if (this.canStage2) {
      return 'Accept & verify';
    }
    return 'Accept';
  }

  get showApproverAssignments(): boolean {
    return this.detail.requiresKycApproval;
  }

  get stage1ApproverLabel(): string {
    return this.formatApprover(this.detail.stage1Approver);
  }

  get stage2ApproverLabel(): string {
    return this.formatApprover(this.detail.stage2Approver);
  }

  get assignmentHint(): string {
    if (!this.showApproverAssignments) {
      return '';
    }
    if (this.canStage1 || this.canStage2) {
      return 'You are the assigned reviewer for the current stage.';
    }
    const stage = this.detail.kycStage;
    if (stage === 'stage1') {
      return `Waiting for ${this.stage1ApproverLabel} (stage 1).`;
    }
    if (stage === 'stage2') {
      return `Waiting for ${this.stage2ApproverLabel} (stage 2).`;
    }
    return '';
  }

  get canAllowResubmission(): boolean {
    return (
      this.detail.kycStatus === 'REJECTED' && this.storage.getUser()?.organizationKycApprover === true
    );
  }

  get canDecide(): boolean {
    return this.canStage1 || this.canStage2 || this.canAllowResubmission;
  }

  get stageBanner(): string {
    if (this.canStage1 && this.detail.kycStatus === 'DRAFT') {
      return 'Draft signup — stage 1 review (accept advances to stage 2; reject requires a reason)';
    }
    if (this.canStage1) {
      return 'Stage 1 — compliance & identity review';
    }
    if (this.canStage2) {
      return 'Stage 2 — final approval (verification email on accept)';
    }
    if (this.canAllowResubmission) {
      return 'Rejected — allow applicant to resubmit KYC';
    }
    return '';
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  accept(): void {
    const notes = this.optionalNotes();
    if (this.canStage1) {
      this.dialogRef.close({ action: 'stage1-approve', reason: notes });
      return;
    }
    if (this.canStage2) {
      this.dialogRef.close({ action: 'stage2-approve', reason: notes });
    }
  }

  reject(): void {
    const reason = this.validateRejectReason();
    if (reason === null) {
      return;
    }
    if (this.canStage1) {
      this.dialogRef.close({ action: 'stage1-reject', reason });
      return;
    }
    if (this.canStage2) {
      this.dialogRef.close({ action: 'stage2-reject', reason });
    }
  }

  allowResubmission(): void {
    const reason = this.validateRejectReason();
    if (reason === null) {
      return;
    }
    this.dialogRef.close({ action: 'allow-resubmission', reason });
  }

  private formatApprover(approver?: KycApproverAssignment): string {
    if (!approver) {
      return 'Not assigned yet';
    }
    const name = (approver.displayName ?? approver.username ?? '').trim();
    return name || 'Not assigned yet';
  }

  private isAssignedForStage(stage: 1 | 2): boolean {
    if (!this.detail.requiresKycApproval) {
      return false;
    }
    if (this.storage.getUser()?.organizationKycApprover !== true) {
      return false;
    }
    const approver = stage === 1 ? this.detail.stage1Approver : this.detail.stage2Approver;
    if (!approver?.username) {
      return false;
    }
    const principal = this.reviewerPrincipal();
    if (!principal) {
      return false;
    }
    const assigned = approver.username.trim().toLowerCase();
    return (
      principal === assigned ||
      principal === assigned.split('@')[0] ||
      (approver.userId != null && this.storage.getUser()?.id === approver.userId)
    );
  }

  private reviewerPrincipal(): string {
    const user = this.storage.getUser();
    const email = (user?.email ?? '').trim().toLowerCase();
    const username = (user?.username ?? '').trim().toLowerCase();
    return email || username;
  }

  private optionalNotes(): string {
    this.reasonError = '';
    return (this.form.value.reason ?? '').trim();
  }

  private validateRejectReason(): string | null {
    this.reasonError = '';
    const reason = (this.form.value.reason ?? '').trim();
    if (reason.length < 8) {
      this.reasonError = 'Enter a rejection reason (at least 8 characters) so the applicant knows what to fix.';
      this.form.get('reason')?.markAsTouched();
      return null;
    }
    return reason;
  }
}
