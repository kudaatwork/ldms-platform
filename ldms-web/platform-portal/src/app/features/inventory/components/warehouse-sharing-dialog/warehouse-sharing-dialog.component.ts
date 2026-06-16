import { ChangeDetectorRef, Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { WarehouseAccessGrant, WarehouseRow } from '../../models/inventory.model';
import { InventoryPortalService } from '../../services/inventory-portal.service';

export interface WarehouseSharingDialogData {
  warehouse: WarehouseRow;
}

@Component({
  selector: 'app-warehouse-sharing-dialog',
  templateUrl: './warehouse-sharing-dialog.component.html',
  styleUrl: './warehouse-sharing-dialog.component.scss',
  standalone: false,
})
export class WarehouseSharingDialogComponent implements OnInit {
  loading = true;
  saving = false;
  error = '';
  grants: WarehouseAccessGrant[] = [];
  form: FormGroup;

  readonly accessLevels: Array<{ value: 'READ' | 'FULFILL'; label: string }> = [
    { value: 'READ', label: 'Read — view stock only' },
    { value: 'FULFILL', label: 'Fulfill — view and dispatch stock' },
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialogRef: MatDialogRef<WarehouseSharingDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public readonly data: WarehouseSharingDialogData,
  ) {
    this.form = this.fb.group({
      grantedOrganizationId: [null, [Validators.required, Validators.min(1)]],
      accessLevel: ['READ', Validators.required],
    });
  }

  ngOnInit(): void {
    this.reload();
  }

  get warehouseName(): string {
    return this.data.warehouse.name;
  }

  reload(): void {
    this.loading = true;
    this.error = '';
    this.inventoryService
      .listWarehouseAccessGrants(this.data.warehouse.id)
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (grants) => {
          this.grants = grants;
        },
        error: (err: Error) => {
          this.error = err.message ?? 'Could not load sharing rules.';
        },
      });
  }

  grantAccess(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving = true;
    this.inventoryService
      .grantWarehouseAccess({
        warehouseLocationId: this.data.warehouse.id,
        grantedOrganizationId: Number(raw['grantedOrganizationId']),
        accessLevel: raw['accessLevel'] as 'READ' | 'FULFILL',
      })
      .pipe(finalize(() => {
        this.saving = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (grants) => {
          this.grants = grants;
          this.form.reset({ grantedOrganizationId: null, accessLevel: 'READ' });
          this.snackBar.open('Access granted.', 'Close', { duration: 3500 });
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not grant access.', 'Close', { duration: 5000 });
        },
      });
  }

  revoke(grant: WarehouseAccessGrant): void {
    if (this.saving) {
      return;
    }
    this.saving = true;
    this.inventoryService
      .revokeWarehouseAccess(grant.warehouseLocationId, grant.grantedOrganizationId)
      .pipe(finalize(() => {
        this.saving = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: (grants) => {
          this.grants = grants;
          this.snackBar.open('Access revoked.', 'Close', { duration: 3500 });
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not revoke access.', 'Close', { duration: 5000 });
        },
      });
  }

  close(): void {
    this.dialogRef.close(true);
  }
}
