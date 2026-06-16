import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { OrganizationService, BranchAllocationOption } from '../../../../core/services/organization.service';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import {
  WAREHOUSE_LOCATION_TYPE_OPTIONS,
  WarehouseLocationType,
  WarehouseRow,
} from '../../models/inventory.model';

export interface AddWarehouseDialogData {
  supplierId: number;
  mode?: 'create' | 'edit';
  warehouse?: WarehouseRow;
  /** Pre-select branch / sub-branch when opened from organisation drill-down. */
  preselectedBranchId?: number;
}

@Component({
  selector: 'app-add-warehouse-dialog',
  templateUrl: './add-warehouse-dialog.component.html',
  styleUrl: './add-warehouse-dialog.component.scss',
  standalone: false,
})
export class AddWarehouseDialogComponent implements OnInit {
  form: FormGroup;
  submitting = false;
  submitError = '';
  addressLine1 = '';
  addressLine2 = '';
  postalCode = '';
  suburbIdStr = '';
  addressError = '';
  branches: BranchAllocationOption[] = [];
  branchesLoading = false;
  branchesError = '';

  readonly warehouseTypeOptions = WAREHOUSE_LOCATION_TYPE_OPTIONS;
  readonly isEdit: boolean;
  readonly addressReadOnly: string;

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly organizationService: OrganizationService,
    private readonly dialogRef: MatDialogRef<AddWarehouseDialogComponent, WarehouseRow>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AddWarehouseDialogData,
  ) {
    this.dialogRef.disableClose = true;
    this.isEdit = data.mode === 'edit';
    this.addressReadOnly = data.warehouse?.addressLabel?.trim() || 'Linked location (address cannot be changed here)';
    this.form = this.buildForm(data.warehouse);
  }

  get topLevelBranchOptions(): BranchAllocationOption[] {
    return this.branches.filter((b) => b.level === 'BRANCH');
  }

  get subLevelBranchOptions(): BranchAllocationOption[] {
    return this.branches.filter((b) => b.level === 'SUB_BRANCH');
  }

  ngOnInit(): void {
    this.branchesLoading = true;
    this.organizationService.listBranchesForAllocation().subscribe({
      next: (branches) => {
        this.branches = branches;
        this.branchesLoading = false;
        const preset = this.data.preselectedBranchId ?? this.data.warehouse?.branchId;
        if (preset && branches.some((b) => b.id === preset)) {
          this.form.patchValue({ branchId: preset });
        } else if (!this.isEdit && branches.length === 1) {
          this.form.patchValue({ branchId: branches[0].id });
        }
      },
      error: (err: Error) => {
        this.branchesLoading = false;
        this.branchesError = err.message ?? 'Could not load branches.';
      },
    });
  }

  get title(): string {
    return this.isEdit ? 'Edit warehouse' : 'Add warehouse';
  }

  get subtitle(): string {
    return this.isEdit
      ? 'Update warehouse name, branch, type, or description. The physical address stays linked to the existing location record.'
      : 'Register a storage location tied to a branch or depot in your organisation hierarchy.';
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  onSubmit(): void {
    this.addressError = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.isEdit) {
      this.submitEdit();
      return;
    }

    const suburbId = Number(this.suburbIdStr);
    if (!Number.isFinite(suburbId) || suburbId <= 0) {
      this.addressError = 'Select country, province, district, city, and suburb for the warehouse address.';
      return;
    }

    const line1 = this.addressLine1.trim();
    if (!line1) {
      this.addressError = 'Address line 1 is required.';
      return;
    }

    this.submitting = true;
    this.submitError = '';
    const raw = this.form.value;

    this.inventoryService
      .createWarehouse({
        name: raw['name'],
        description: raw['description'],
        line1,
        line2: this.addressLine2.trim() || undefined,
        postalCode: this.postalCode.trim() || undefined,
        suburbId,
        supplierId: this.data.supplierId,
        branchId: Number(raw['branchId']),
        warehouseType: raw['warehouseType'] as WarehouseLocationType,
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (warehouse) => this.dialogRef.close(warehouse),
        error: (err: Error) => (this.submitError = err.message ?? 'Could not create warehouse.'),
      });
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  private submitEdit(): void {
    const warehouse = this.data.warehouse;
    if (!warehouse) {
      this.submitError = 'Warehouse record is missing.';
      return;
    }

    this.submitting = true;
    this.submitError = '';
    const raw = this.form.value;
    const supplierId = warehouse.supplierId || this.data.supplierId;
    const branchId = raw['branchId'] != null ? Number(raw['branchId']) : warehouse.branchId;

    this.inventoryService
      .updateWarehouse({
        warehouseLocationId: warehouse.id,
        name: raw['name'],
        description: raw['description'],
        locationId: warehouse.locationId,
        supplierId,
        branchId,
        warehouseType: raw['warehouseType'] as WarehouseLocationType,
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (row) =>
          this.dialogRef.close({
            ...row,
            addressLabel: warehouse.addressLabel || row.addressLabel,
          }),
        error: (err: Error) => (this.submitError = err.message ?? 'Could not update warehouse.'),
      });
  }

  private buildForm(warehouse?: WarehouseRow): FormGroup {
    const warehouseType = (warehouse?.warehouseType?.trim().toUpperCase() || 'SUPPLIER') as WarehouseLocationType;
    const safeType = warehouseType === 'TRANSIT' ? 'SUPPLIER' : warehouseType;
    return this.fb.group({
      name: [warehouse?.name ?? '', [Validators.required, Validators.maxLength(150)]],
      description: [warehouse?.description ?? '', [Validators.required, Validators.maxLength(500)]],
      warehouseType: [safeType, Validators.required],
      branchId: [warehouse?.branchId ?? null, Validators.required],
    });
  }
}
