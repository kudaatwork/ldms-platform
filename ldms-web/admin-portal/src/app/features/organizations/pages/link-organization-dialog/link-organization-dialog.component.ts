import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { OrganizationsAdminService } from '../../services/organizations-admin.service';
import type { OrganizationClassification } from '../../models/organization.model';

export type LinkOrganizationKind = 'CUSTOMER' | 'TRANSPORT_COMPANY' | 'CLEARING_AGENT';

export interface LinkOrganizationCandidate {
  id: number;
  name: string;
  email?: string;
}

export interface LinkOrganizationDialogData {
  supplierId: number;
  supplierName: string;
  linkKind: LinkOrganizationKind;
  /** Organisation ids already linked — excluded from the picker. */
  excludeIds?: number[];
}

export interface LinkOrganizationDialogResult {
  linked: boolean;
}

@Component({
  selector: 'app-link-organization-dialog',
  templateUrl: './link-organization-dialog.component.html',
  styleUrl: './link-organization-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
})
export class LinkOrganizationDialogComponent implements OnInit {
  form: FormGroup;
  submitting = false;
  saveError: string | null = null;
  organizations: LinkOrganizationCandidate[] = [];
  filteredOrganizations: LinkOrganizationCandidate[] = [];
  loadingOrgs = false;
  searchQuery = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly orgService: OrganizationsAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<LinkOrganizationDialogComponent, LinkOrganizationDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: LinkOrganizationDialogData,
  ) {
    this.form = this.fb.group({
      targetOrganizationId: [null as number | null, Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadCandidates();
  }

  get title(): string {
    switch (this.data.linkKind) {
      case 'CUSTOMER':
        return 'Link customer';
      case 'TRANSPORT_COMPANY':
        return 'Link transporter';
      case 'CLEARING_AGENT':
        return 'Link clearing agent';
      default:
        return 'Link organisation';
    }
  }

  get subtitle(): string {
    switch (this.data.linkKind) {
      case 'CUSTOMER':
        return 'Search and select a customer organisation to connect with this supplier.';
      case 'TRANSPORT_COMPANY':
        return 'Search and select a transport company to contract with this supplier.';
      case 'CLEARING_AGENT':
        return 'Search and select a clearing agent to partner with this supplier.';
      default:
        return 'Search and select an organisation to link.';
    }
  }

  get targetLabel(): string {
    switch (this.data.linkKind) {
      case 'CUSTOMER':
        return 'Customer organisation';
      case 'TRANSPORT_COMPANY':
        return 'Transport company';
      case 'CLEARING_AGENT':
        return 'Clearing agent';
      default:
        return 'Organisation';
    }
  }

  get kindIcon(): string {
    switch (this.data.linkKind) {
      case 'CUSTOMER':
        return 'storefront';
      case 'TRANSPORT_COMPANY':
        return 'local_shipping';
      case 'CLEARING_AGENT':
        return 'fact_check';
      default:
        return 'corporate_fare';
    }
  }

  get selectedId(): number | null {
    const raw = this.form.get('targetOrganizationId')?.value;
    const id = Number(raw);
    return Number.isFinite(id) && id > 0 ? id : null;
  }

  get selectedOrganization(): LinkOrganizationCandidate | null {
    const id = this.selectedId;
    if (!id) {
      return null;
    }
    return this.organizations.find((o) => o.id === id) ?? null;
  }

  supplierDisplayName(): string {
    return this.data.supplierName?.trim() || `Supplier #${this.data.supplierId}`;
  }

  avatarLetter(name: string): string {
    const trimmed = String(name ?? '').trim();
    return trimmed ? trimmed.charAt(0).toUpperCase() : '?';
  }

  isSelected(id: number): boolean {
    return this.selectedId === id;
  }

  selectOrganization(org: LinkOrganizationCandidate): void {
    this.form.patchValue({ targetOrganizationId: org.id });
    this.form.get('targetOrganizationId')?.markAsTouched();
  }

  onSearchQueryChange(): void {
    this.applySearchFilter();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.applySearchFilter();
  }

  cancel(): void {
    this.dialogRef.close({ linked: false });
  }

  save(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const targetId = this.selectedId;
    if (!targetId) {
      return;
    }
    this.submitting = true;
    this.saveError = null;
    const supplierId = this.data.supplierId;
    const link$ =
      this.data.linkKind === 'CUSTOMER'
        ? this.orgService.linkCustomer(supplierId, targetId)
        : this.data.linkKind === 'TRANSPORT_COMPANY'
          ? this.orgService.linkTransporter(supplierId, targetId)
          : this.orgService.linkClearingAgent(supplierId, targetId);

    link$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.snackBar.open(`${this.targetLabel} linked successfully.`, 'Close', {
            duration: 4500,
            panelClass: ['app-snackbar-success'],
          });
          this.dialogRef.close({ linked: true });
        },
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not link organisation.';
        },
      });
  }

  private loadCandidates(): void {
    this.loadingOrgs = true;
    const classification = this.data.linkKind as OrganizationClassification;
    const exclude = new Set([this.data.supplierId, ...(this.data.excludeIds ?? [])]);
    this.orgService.fetchOrganizationsByClassification(classification).subscribe({
      next: (rows: LinkOrganizationCandidate[]) => {
        this.organizations = rows.filter((r) => !exclude.has(r.id));
        this.applySearchFilter();
        this.loadingOrgs = false;
      },
      error: () => {
        this.organizations = [];
        this.filteredOrganizations = [];
        this.loadingOrgs = false;
      },
    });
  }

  private applySearchFilter(): void {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) {
      this.filteredOrganizations = [...this.organizations];
      return;
    }
    this.filteredOrganizations = this.organizations.filter((o) => {
      const name = String(o.name ?? '').toLowerCase();
      const email = String(o.email ?? '').toLowerCase();
      return name.includes(q) || email.includes(q);
    });
  }
}
