import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { AgentRow, CreateAgentPayload } from '../../models/org-management.model';
import { OrgManagementPortalService } from '../../services/org-management-portal.service';

export type AgentFormDialogAction = 'create' | 'edit' | 'view';

export interface AgentFormDialogData {
  action: AgentFormDialogAction;
  row?: AgentRow | null;
}

export interface AgentFormDialogResult {
  saved: boolean;
}

@Component({
  selector: 'app-agent-form-dialog',
  templateUrl: './agent-form-dialog.component.html',
  styleUrl: './agent-form-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
  ],
})
export class AgentFormDialogComponent {
  form: FormGroup;
  submitting = false;
  saveError: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly orgMgmt: OrgManagementPortalService,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AgentFormDialogComponent, AgentFormDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: AgentFormDialogData,
  ) {
    const row = data.row;
    this.form = this.fb.group({
      agentKind: [row?.agentKind ?? 'INDIVIDUAL', Validators.required],
      firstName: [row?.firstName ?? '', [Validators.required, Validators.maxLength(100)]],
      lastName: [row?.lastName ?? '', [Validators.required, Validators.maxLength(100)]],
      email: [row?.email ?? '', Validators.maxLength(255)],
      phoneNumber: [row?.phoneNumber ?? '', Validators.maxLength(50)],
      role: [row?.role ?? '', Validators.maxLength(100)],
      assignedRegion: [row?.assignedRegion ?? '', Validators.maxLength(200)],
      active: [row?.active ?? true],
    });
    if (data.action === 'view') {
      this.form.disable();
    }
  }

  get isEdit(): boolean {
    return this.data.action === 'edit';
  }

  get isView(): boolean {
    return this.data.action === 'view';
  }

  get title(): string {
    if (this.isView) return 'View agent';
    return this.isEdit ? 'Edit agent' : 'Add agent';
  }

  get subtitle(): string {
    if (this.isView) return 'Agent details — read only.';
    return this.isEdit ? 'Update the agent details below.' : 'Register a new agent for your organisation.';
  }

  cancel(): void {
    this.dialogRef.close({ saved: false });
  }

  save(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = this.toPayload();
    this.submitting = true;
    this.saveError = null;

    const request$ =
      this.isEdit && this.data.row?.id
        ? this.orgMgmt.updateAgent(this.data.row.id, payload)
        : this.orgMgmt.createAgent(payload);

    request$.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: () => {
        this.snackBar.open(this.isEdit ? 'Agent updated.' : 'Agent created.', 'Close', { duration: 3000 });
        this.dialogRef.close({ saved: true });
      },
      error: (err: Error) => {
        this.saveError = err.message ?? 'Failed to save this agent.';
        this.snackBar.open(this.saveError, 'Close', { duration: 5000 });
      },
    });
  }

  hasError(control: string, error: string): boolean {
    const c = this.form.get(control);
    return !!(c && (c.touched || c.dirty) && c.hasError(error));
  }

  private toPayload(): CreateAgentPayload {
    const v = this.form.getRawValue();
    return {
      agentKind: v.agentKind,
      firstName: String(v.firstName ?? '').trim(),
      lastName: String(v.lastName ?? '').trim(),
      email: String(v.email ?? '').trim() || undefined,
      phoneNumber: String(v.phoneNumber ?? '').trim() || undefined,
      role: String(v.role ?? '').trim() || undefined,
      assignedRegion: String(v.assignedRegion ?? '').trim() || undefined,
      active: Boolean(v.active),
    };
  }
}
