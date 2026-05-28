import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { OrganizationsAdminService } from '../../services/organizations-admin.service';
import type { AgentPayload } from '../../models/agent.model';
import type { AgentListRow } from '../../models/organization-directory.model';

export type AgentFormDialogAction = 'create' | 'edit' | 'view';

export interface AgentFormDialogData {
  action: AgentFormDialogAction;
  row?: AgentListRow | null;
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
export class AgentFormDialogComponent implements OnInit {
  form: FormGroup;
  submitting = false;
  saveError: string | null = null;
  organizations: Array<{ id: number; name: string }> = [];

  readonly agentKindOptions = [
    { value: 'INDIVIDUAL', label: 'Individual' },
    { value: 'ORGANIZATION', label: 'Organisation' },
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly orgService: OrganizationsAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AgentFormDialogComponent, AgentFormDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: AgentFormDialogData,
  ) {
    const row = data.row;
    this.form = this.fb.group({
      organizationId: [row?.organizationId ?? null, [Validators.required]],
      agentKind: [row?.agentKind ?? 'INDIVIDUAL', [Validators.required]],
      firstName: [row?.firstName ?? '', [Validators.required, Validators.maxLength(100)]],
      lastName: [row?.lastName ?? '', [Validators.required, Validators.maxLength(100)]],
      role: [row?.role ?? '', Validators.maxLength(200)],
      agentType: [row?.agentType ?? '', Validators.maxLength(100)],
      email: [row?.email ?? '', [Validators.email, Validators.maxLength(200)]],
      phoneNumber: [row?.phoneNumber ?? '', Validators.maxLength(50)],
      active: [row?.active ?? true],
    });

    if (data.action === 'view') {
      this.form.disable();
    }
  }

  ngOnInit(): void {
    if (this.data.action !== 'view') {
      this.orgService.fetchOrganizationsForSelect().subscribe({
        next: (orgs) => (this.organizations = orgs),
        error: () => (this.organizations = []),
      });
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
    return this.isEdit
      ? 'Update the agent details below.'
      : 'Register a new agent delegate under an existing organisation.';
  }

  get primaryActionLabel(): string {
    return this.isEdit ? 'Update' : 'Save';
  }

  get primaryActionLoadingLabel(): string {
    return this.isEdit ? 'Updating…' : 'Saving…';
  }

  hasError(control: string, error: string): boolean {
    const c = this.form.get(control);
    return !!(c && (c.touched || c.dirty) && c.hasError(error));
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
        ? this.orgService.updateAgent(this.data.row.id, payload)
        : this.orgService.createAgent(payload);

    request$.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: (response) => {
        if (!response.ok) {
          this.saveError =
            response.message ?? 'The server rejected this save. Check required fields and try again.';
          this.snackBar.open(this.saveError, 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
          return;
        }
        this.snackBar.open(
          response.message ?? (this.isEdit ? 'Agent updated.' : 'Agent created.'),
          'Close',
          { duration: 3000, panelClass: ['app-snackbar-success'] },
        );
        this.dialogRef.close({ saved: true });
      },
      error: (err: { message?: string; status?: number }) => {
        this.saveError =
          (err?.status === 0
            ? 'Request failed before the server response reached the browser. Please retry.'
            : undefined) ??
          err?.message ??
          'Failed to save this agent. Please check inputs and try again.';
        this.snackBar.open(this.saveError, 'Close', {
          duration: 5000,
          panelClass: ['app-snackbar-error'],
        });
      },
    });
  }

  private toPayload(): AgentPayload {
    const v = this.form.getRawValue();
    return {
      organizationId: Number(v.organizationId),
      agentKind: String(v.agentKind ?? '').trim(),
      firstName: String(v.firstName ?? '').trim(),
      lastName: String(v.lastName ?? '').trim(),
      role: this.optionalString(v.role),
      agentType: this.optionalString(v.agentType),
      email: this.optionalString(v.email),
      phoneNumber: this.optionalString(v.phoneNumber),
      active: Boolean(v.active),
    };
  }

  private optionalString(value: unknown): string | undefined {
    const s = String(value ?? '').trim();
    return s.length > 0 ? s : undefined;
  }
}
