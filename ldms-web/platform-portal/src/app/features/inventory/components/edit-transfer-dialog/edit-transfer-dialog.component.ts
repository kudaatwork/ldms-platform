import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import { ProductRow, StockRow, TransferRow, WarehouseRow, LogisticsRouteStopRow } from '../../models/inventory.model';
import { buildRoutePreviewStops } from '../../utils/route-stops.util';

export interface EditTransferDialogData {
  transfer: TransferRow;
  products: ProductRow[];
  warehouses: WarehouseRow[];
  stock: StockRow[];
  updatedByUserId: number;
}

@Component({
  selector: 'app-edit-transfer-dialog',
  templateUrl: './edit-transfer-dialog.component.html',
  styleUrl: './edit-transfer-dialog.component.scss',
  standalone: false,
})
export class EditTransferDialogComponent implements OnInit {
  form: FormGroup;
  submitting = false;
  submitError = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly dialogRef: MatDialogRef<EditTransferDialogComponent, TransferRow>,
    @Inject(MAT_DIALOG_DATA) public readonly data: EditTransferDialogData,
  ) {
    const t = data.transfer;
    const depotIds =
      t.routeStops
        ?.filter((s) => s.stopType === 'EN_ROUTE_DEPOT' && s.warehouseLocationId)
        .map((s) => s.warehouseLocationId as number) ?? [];

    this.dialogRef.disableClose = true;
    this.form = this.fb.group({
      productId: [{ value: t.productId, disabled: true }],
      fromLocationId: [t.fromLocationId, Validators.required],
      toLocationId: [t.toLocationId, Validators.required],
      quantity: [t.quantity, [Validators.required, Validators.min(0.01)]],
      reference: [t.reference ?? ''],
      crossBorder: [!!t.crossBorder],
      enRouteDepotIds: [depotIds as number[]],
    });
  }

  ngOnInit(): void {
    this.updateQuantityValidators();
  }

  get transfer(): TransferRow {
    return this.data.transfer;
  }

  get warehouses(): WarehouseRow[] {
    return this.data.warehouses;
  }

  get fromLocationId(): number {
    return Number(this.form.get('fromLocationId')?.value ?? 0);
  }

  get depotWarehouses(): WarehouseRow[] {
    const toId = Number(this.form.get('toLocationId')?.value ?? 0);
    return this.warehouses.filter(
      (w) =>
        w.id !== this.fromLocationId &&
        w.id !== toId &&
        (w.warehouseType?.toUpperCase() === 'DEPOT' || (w as WarehouseRow & { depot?: boolean }).depot === true),
    );
  }

  get toWarehouses(): WarehouseRow[] {
    return this.warehouses.filter((w) => w.id !== this.fromLocationId);
  }

  get selectedDepotIds(): number[] {
    return (this.form.get('enRouteDepotIds')?.value as number[]) ?? [];
  }

  isDepotSelected(warehouseId: number): boolean {
    return this.selectedDepotIds.includes(warehouseId);
  }

  toggleDepot(warehouseId: number): void {
    if (this.submitting) return;
    const updated = this.isDepotSelected(warehouseId)
      ? this.selectedDepotIds.filter((id) => id !== warehouseId)
      : [...this.selectedDepotIds, warehouseId];
    this.form.patchValue({ enRouteDepotIds: updated });
  }

  onFromWarehouseChange(): void {
    const toId = Number(this.form.get('toLocationId')?.value ?? 0);
    if (toId === this.fromLocationId) {
      this.form.patchValue({ toLocationId: null });
    }
    this.pruneEnRouteStops();
    this.updateQuantityValidators();
  }

  onEnRouteStopsChange(stopIds: number[]): void {
    this.form.patchValue({ enRouteDepotIds: stopIds });
  }

  private pruneEnRouteStops(): void {
    const toId = Number(this.form.get('toLocationId')?.value ?? 0);
    const valid = this.selectedDepotIds.filter(
      (id) => id > 0 && id !== this.fromLocationId && id !== toId,
    );
    if (valid.length !== this.selectedDepotIds.length) {
      this.form.patchValue({ enRouteDepotIds: valid });
    }
  }

  get originLabel(): string {
    return this.warehouses.find((w) => w.id === this.fromLocationId)?.name || this.transfer.fromWarehouse;
  }

  get destinationLabel(): string {
    const toId = Number(this.form.get('toLocationId')?.value ?? 0);
    return this.warehouses.find((w) => w.id === toId)?.name || this.transfer.toWarehouse;
  }

  get toLocationId(): number {
    return Number(this.form.get('toLocationId')?.value ?? 0);
  }

  get previewRouteStops(): LogisticsRouteStopRow[] {
    return buildRoutePreviewStops(this.fromLocationId, this.toLocationId, this.selectedDepotIds, this.warehouses, {
      fromLabel: this.originLabel,
      toLabel: this.destinationLabel,
    });
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    if (raw.fromLocationId === raw.toLocationId) {
      this.submitError = 'Source and destination warehouses must differ.';
      return;
    }

    this.submitting = true;
    this.submitError = '';
    const depotIds = (raw.enRouteDepotIds as number[] | undefined)?.filter((id) => id > 0) ?? [];

    this.inventoryService
      .updateTransfer({
        inventoryTransferId: this.transfer.id,
        fromLocationId: Number(raw.fromLocationId),
        toLocationId: Number(raw.toLocationId),
        quantity: Number(raw.quantity),
        reference: String(raw.reference ?? '').trim() || undefined,
        crossBorder: !!raw.crossBorder,
        updatedByUserId: this.data.updatedByUserId,
        enRouteDepotIds: depotIds,
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (transfer) => this.dialogRef.close(transfer),
        error: (err: Error) => (this.submitError = err.message ?? 'Could not update transfer.'),
      });
  }

  private updateQuantityValidators(): void {
    const control = this.form.get('quantity');
    if (!control) return;
    control.setValidators([Validators.required, Validators.min(0.01)]);
    control.updateValueAndValidity({ emitEvent: false });
  }
}
