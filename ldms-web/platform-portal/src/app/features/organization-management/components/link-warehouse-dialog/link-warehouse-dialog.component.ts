import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { InventoryPortalService } from '../../../inventory/services/inventory-portal.service';
import { WarehouseLocationType, WarehouseRow } from '../../../inventory/models/inventory.model';
import { BranchDetail } from '../../../../core/services/organization.service';

export interface LinkWarehouseDialogData {
  branch: BranchDetail;
  /** Warehouses that can be linked (unlinked or assigned elsewhere). */
  candidates: WarehouseRow[];
}

export interface LinkWarehouseDialogResult {
  linked: WarehouseRow;
}

@Component({
  selector: 'app-link-warehouse-dialog',
  templateUrl: './link-warehouse-dialog.component.html',
  styleUrl: './link-warehouse-dialog.component.scss',
  standalone: false,
})
export class LinkWarehouseDialogComponent implements OnInit {
  searchQuery = '';
  linkingId: number | null = null;
  linkError = '';

  readonly rows: WarehouseRow[];

  constructor(
    private readonly inventoryPortal: InventoryPortalService,
    private readonly dialogRef: MatDialogRef<LinkWarehouseDialogComponent, LinkWarehouseDialogResult>,
    @Inject(MAT_DIALOG_DATA) public readonly data: LinkWarehouseDialogData,
  ) {
    this.dialogRef.disableClose = true;
    this.rows = [...data.candidates].sort((a, b) => a.name.localeCompare(b.name));
  }

  ngOnInit(): void {
    if (!this.rows.length) {
      this.dialogRef.disableClose = false;
    }
  }

  get filteredRows(): WarehouseRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) {
      return this.rows;
    }
    return this.rows.filter((row) => {
      const haystack = [row.name, row.description, row.addressLabel, row.branchLabel, row.warehouseType]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return haystack.includes(q);
    });
  }

  branchLabel(row: WarehouseRow): string {
    if (!row.branchId) {
      return 'Not linked';
    }
    if (row.branchLabel?.trim()) {
      return row.branchLabel.trim();
    }
    return `Branch #${row.branchId}`;
  }

  linkWarehouse(row: WarehouseRow): void {
    if (this.linkingId != null || row.warehouseType === 'TRANSIT') {
      return;
    }
    this.linkingId = row.id;
    this.linkError = '';
    const warehouseType = (row.warehouseType?.trim().toUpperCase() || 'SUPPLIER') as WarehouseLocationType;
    const safeType = warehouseType === 'TRANSIT' ? 'SUPPLIER' : warehouseType;

    this.inventoryPortal
      .updateWarehouse({
        warehouseLocationId: row.id,
        name: row.name,
        description: row.description,
        locationId: row.locationId,
        supplierId: row.supplierId,
        branchId: this.data.branch.id,
        warehouseType: safeType,
      })
      .pipe(finalize(() => (this.linkingId = null)))
      .subscribe({
        next: (linked) =>
          this.dialogRef.close({
            linked: {
              ...linked,
              branchId: this.data.branch.id,
              branchLabel: this.data.branch.branchName,
              addressLabel: linked.addressLabel || row.addressLabel,
            },
          }),
        error: (err: Error) => {
          this.linkError = err.message ?? 'Could not link warehouse.';
        },
      });
  }

  close(): void {
    if (this.linkingId == null) {
      this.dialogRef.close();
    }
  }
}
