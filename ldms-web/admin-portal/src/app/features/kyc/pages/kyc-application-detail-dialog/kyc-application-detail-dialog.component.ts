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
  KycDecisionAction,
  KycStageKey,
} from '../../../organizations/models/organization.model';
import { kycStageNumber } from '../../../organizations/models/organization.model';

export interface KycApplicationDetailDialogData {
  detail: KycApplicationDetail;
}

type DecisionStep = 'choose' | 'reject' | 'resubmit';

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
  readonly rejectionSuggestions = [
    'Incomplete or missing supporting documents.',
    'Registration or tax details could not be verified.',
    'Contact identification does not match submitted records.',
    'Organisation details require correction before approval.',
  ] as const;

  readonly minReasonLength = 8;

  decisionStep: DecisionStep = 'choose';
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

  get requiredApprovalStages(): number {
    return this.detail.effectiveKycRequiredApprovalStages ?? 2;
  }

  get usesSingleStageApproval(): boolean {
    return this.requiredApprovalStages <= 1;
  }

  get activeStageNumber(): number {
    if (this.detail.kycStage === 'none') {
      return 0;
    }
    return kycStageNumber(this.detail.kycStage);
  }

  get canCurrentStage(): boolean {
    const stage = this.activeStageNumber;
    return stage > 0 && this.detail.kycStage !== 'none' && this.isAssignedForStage(stage);
  }

  get isFinalStage(): boolean {
    return this.activeStageNumber > 0 && this.activeStageNumber >= this.requiredApprovalStages;
  }

  get acceptButtonLabel(): string {
    if (this.canCurrentStage && this.isFinalStage) {
      return 'Accept & verify';
    }
    return 'Accept application';
  }

  get acceptSummary(): string {
    if (this.isFinalStage) {
      return 'Approve this application and send the verification email to the primary contact.';
    }
    if (this.usesSingleStageApproval) {
      return 'Approve organisation and contact details to complete verification.';
    }
    return `Advance to stage ${this.activeStageNumber + 1} after your review.`;
  }

  get rejectSummary(): string {
    return 'Decline this application. The applicant will receive your reason and can fix issues before resubmitting.';
  }

  get showApproverAssignments(): boolean {
    return this.detail.requiresKycApproval;
  }

  get assignedReviewerRows(): { stage: number; label: string }[] {
    const rows: { stage: number; label: string }[] = [];
    for (let stage = 1; stage <= this.requiredApprovalStages; stage++) {
      rows.push({
        stage,
        label: this.approverLabelForStage(stage),
      });
    }
    return rows;
  }

  get assignmentHint(): string {
    if (!this.showApproverAssignments) {
      return '';
    }
    if (this.canCurrentStage) {
      return 'You are the assigned reviewer for the current stage.';
    }
    const stage = this.activeStageNumber;
    if (stage > 0) {
      return `Waiting for ${this.approverLabelForStage(stage)} (stage ${stage}).`;
    }
    return '';
  }

  get canAllowResubmission(): boolean {
    return (
      this.detail.kycStatus === 'REJECTED' && this.storage.getUser()?.organizationKycApprover === true
    );
  }

  get canDecide(): boolean {
    return this.canCurrentStage || this.canAllowResubmission;
  }

  get reasonLength(): number {
    return (this.form.value.reason ?? '').trim().length;
  }

  get reasonMeetsMinimum(): boolean {
    return this.reasonLength >= this.minReasonLength;
  }

  get stageBanner(): string {
    if (this.canCurrentStage && this.detail.kycStatus === 'DRAFT') {
      return this.usesSingleStageApproval
        ? 'Draft signup — single approver review (accept completes verification; reject requires a reason)'
        : `Draft signup — stage ${this.activeStageNumber} review (accept advances pipeline; reject requires a reason)`;
    }
    if (this.canCurrentStage) {
      if (this.isFinalStage) {
        return `Stage ${this.activeStageNumber} — final approval (verification email on accept)`;
      }
      return `Stage ${this.activeStageNumber} — review in progress`;
    }
    if (this.canAllowResubmission) {
      return 'Rejected — open a new application cycle (applicant returns to Draft)';
    }
    return '';
  }

  get decisionHint(): string {
    if (this.canAllowResubmission && this.decisionStep === 'resubmit') {
      return 'Opens a new application cycle in Draft. The applicant may reuse the same registration details on the platform.';
    }
    if (this.decisionStep === 'reject') {
      return 'Be specific so the applicant knows exactly what to fix.';
    }
    if (this.canAllowResubmission && !this.canCurrentStage) {
      return 'This application was rejected. You can allow the applicant to submit again.';
    }
    if (!this.canCurrentStage) {
      return '';
    }
    if (this.isFinalStage) {
      return `Stage ${this.activeStageNumber} — final decision on this application.`;
    }
    if (this.usesSingleStageApproval) {
      return 'Review organisation and contact details, then choose accept or reject.';
    }
    return `Stage ${this.activeStageNumber} — review details, then accept to advance or reject with feedback.`;
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  backToChoose(): void {
    this.reasonError = '';
    this.form.patchValue({ reason: '' });
    this.decisionStep = 'choose';
  }

  startReject(): void {
    this.reasonError = '';
    this.decisionStep = 'reject';
  }

  startResubmission(): void {
    this.reasonError = '';
    this.decisionStep = 'resubmit';
  }

  applySuggestion(text: string): void {
    this.form.patchValue({ reason: text });
    this.reasonError = '';
  }

  accept(): void {
    const notes = this.optionalNotes();
    const stage = this.activeStageNumber;
    if (stage > 0 && this.canCurrentStage) {
      this.dialogRef.close({ action: `stage${stage}-approve` as KycDecisionAction, reason: notes });
    }
  }

  reject(): void {
    const reason = this.validateRejectReason();
    if (reason === null) {
      return;
    }
    const stage = this.activeStageNumber;
    if (stage > 0 && this.canCurrentStage) {
      this.dialogRef.close({ action: `stage${stage}-reject` as KycDecisionAction, reason });
    }
  }

  allowResubmission(): void {
    const reason = this.validateRejectReason();
    if (reason === null) {
      return;
    }
    this.dialogRef.close({ action: 'allow-resubmission', reason });
  }

  isActiveReviewStage(stage: KycStageKey): boolean {
    return this.detail.kycStage !== 'none' && this.detail.kycStage === stage;
  }

  private approverLabelForStage(stage: number): string {
    const approver = this.approverForStage(stage);
    return this.formatApprover(approver);
  }

  private approverForStage(stage: number): KycApproverAssignment | undefined {
    switch (stage) {
      case 1:
        return this.detail.stage1Approver;
      case 2:
        return this.detail.stage2Approver;
      case 3:
        return this.detail.stage3Approver;
      case 4:
        return this.detail.stage4Approver;
      case 5:
        return this.detail.stage5Approver;
      default:
        return undefined;
    }
  }

  private formatApprover(approver?: KycApproverAssignment): string {
    if (!approver) {
      return 'Not assigned yet';
    }
    const name = (approver.displayName ?? approver.username ?? '').trim();
    return name || 'Not assigned yet';
  }

  private isAssignedForStage(stage: number): boolean {
    if (!this.detail.requiresKycApproval) {
      return false;
    }
    if (this.storage.getUser()?.organizationKycApprover !== true) {
      return false;
    }
    const approver = this.approverForStage(stage);
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
    const username = (user?.username ?? '').trim().toLowerCase();
    const email = (user?.email ?? '').trim().toLowerCase();
    return username || email;
  }

  private optionalNotes(): string {
    this.reasonError = '';
    return (this.form.value.reason ?? '').trim();
  }

  private validateRejectReason(): string | null {
    this.reasonError = '';
    const reason = (this.form.value.reason ?? '').trim();
    if (reason.length < this.minReasonLength) {
      this.reasonError = `Enter at least ${this.minReasonLength} characters so the applicant knows what to fix.`;
      this.form.get('reason')?.markAsTouched();
      return null;
    }
    return reason;
  }
}
