import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import { ProductRow, StockRow, TransferRow, WarehouseRow } from '../../models/inventory.model';

export interface CreateTransferDialogData {
  products: ProductRow[];
  warehouses: WarehouseRow[];
  stock: StockRow[];
  createdByUserId: number;
  prefill?: {
    productId?: number;
    fromLocationId?: number;
  };
}

@Component({
  selector: 'app-create-transfer-dialog',
  templateUrl: './create-transfer-dialog.component.html',
  styleUrl: './create-transfer-dialog.component.scss',
  standalone: false,
})
export class CreateTransferDialogComponent implements OnInit {
  form: FormGroup;
  submitting = false;
  submitError = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly dialogRef: MatDialogRef<CreateTransferDialogComponent, TransferRow>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CreateTransferDialogData,
  ) {
    this.dialogRef.disableClose = true;
    this.form = this.fb.group({
      productId: [data.prefill?.productId ?? null, Validators.required],
      fromLocationId: [data.prefill?.fromLocationId ?? null, Validators.required],
      toLocationId: [null, Validators.required],
      quantity: [null, [Validators.required, Validators.min(0.01)]],
      unitCost: [0, [Validators.min(0)]],
      reference: [''],
      crossBorder: [false],
    });
  }

  ngOnInit(): void {
    this.syncWarehouseSelections();
    this.applyCapturedUnitCost();
    this.updateQuantityValidators();
  }

  get products(): ProductRow[] {
    return this.data.products;
  }

  get warehouses(): WarehouseRow[] {
    return this.data.warehouses;
  }

  get selectedProductId(): number {
    return Number(this.form.get('productId')?.value ?? 0);
  }

  get productSelected(): boolean {
    return this.selectedProductId > 0;
  }

  get selectedProduct(): ProductRow | null {
    if (!this.productSelected) {
      return null;
    }
    return this.products.find((product) => product.id === this.selectedProductId) ?? null;
  }

  /** On-hand stock rows for the selected product, one per warehouse. */
  get productStockLocations(): StockRow[] {
    if (!this.productSelected) {
      return [];
    }
    return this.data.stock
      .filter((row) => row.productId === this.selectedProductId && row.quantityOnHand > 0)
      .sort((a, b) => a.warehouseName.localeCompare(b.warehouseName));
  }

  get fromLocationId(): number {
    return Number(this.form.get('fromLocationId')?.value ?? 0);
  }

  get fromWarehouseSelected(): boolean {
    return this.fromLocationId > 0;
  }

  /** Source warehouses that currently hold the selected product. */
  get fromWarehouses(): WarehouseRow[] {
    if (!this.productSelected) {
      return [];
    }
    const stockedIds = new Set(this.productStockLocations.map((row) => row.warehouseLocationId));
    return this.warehouses.filter((warehouse) => stockedIds.has(warehouse.id));
  }

  get fromWarehouseHint(): string {
    if (!this.productSelected) {
      return 'Select a product first to choose the source warehouse.';
    }
    if (!this.productStockLocations.length) {
      return 'This product has no on-hand stock at any warehouse.';
    }
    return '';
  }

  /** Destination choices — everything except the selected source warehouse. */
  get toWarehouses(): WarehouseRow[] {
    if (!this.fromWarehouseSelected) {
      return [];
    }
    return this.warehouses.filter((warehouse) => warehouse.id !== this.fromLocationId);
  }

  get toWarehouseHint(): string {
    return this.fromWarehouseSelected ? '' : 'Select the source warehouse first.';
  }

  /** Suggest cross-border when source and destination addresses look like different countries. */
  get suggestCrossBorder(): boolean {
    const from = this.warehouses.find((w) => w.id === this.fromLocationId);
    const to = this.warehouses.find((w) => w.id === Number(this.form.get('toLocationId')?.value ?? 0));
    if (!from?.addressLabel || !to?.addressLabel) {
      return false;
    }
    const fromCountry = from.addressLabel.split(',').pop()?.trim().toLowerCase() ?? '';
    const toCountry = to.addressLabel.split(',').pop()?.trim().toLowerCase() ?? '';
    return fromCountry.length > 0 && toCountry.length > 0 && fromCountry !== toCountry;
  }

  onToWarehouseChange(): void {
    if (this.suggestCrossBorder && !this.form.get('crossBorder')?.dirty) {
      this.form.patchValue({ crossBorder: true });
    }
  }

  get selectedFromStock(): StockRow | null {
    if (!this.productSelected || !this.fromWarehouseSelected) {
      return null;
    }
    return (
      this.productStockLocations.find((row) => row.warehouseLocationId === this.fromLocationId) ?? null
    );
  }

  get maxTransferQuantity(): number | null {
    const row = this.selectedFromStock;
    if (!row || row.availableQuantity <= 0) {
      return null;
    }
    return row.availableQuantity;
  }

  onProductChange(): void {
    this.syncWarehouseSelections();
    this.updateQuantityValidators();
  }

  onFromWarehouseChange(): void {
    const toId = Number(this.form.get('toLocationId')?.value ?? 0);
    const patch: { toLocationId?: null; unitCost?: number } = {};
    if (!this.fromWarehouseSelected || toId === this.fromLocationId) {
      patch.toLocationId = null;
    }
    this.form.patchValue(patch);
    this.applyCapturedUnitCost();
    this.updateQuantityValidators();
  }

  selectFromWarehouse(warehouseId: number): void {
    if (this.submitting) {
      return;
    }
    this.form.patchValue({ fromLocationId: warehouseId });
    this.onFromWarehouseChange();
  }

  isFromWarehouseSelected(warehouseId: number): boolean {
    return this.fromLocationId === warehouseId;
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
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
    const raw = this.form.value;
    if (raw.fromLocationId === raw.toLocationId) {
      this.submitError = 'Source and destination warehouses must differ.';
      return;
    }

    const maxQty = this.maxTransferQuantity;
    if (maxQty != null && Number(raw.quantity) > maxQty) {
      this.submitError = `Transfer quantity cannot exceed available stock (${maxQty}).`;
      return;
    }

    this.submitting = true;
    this.submitError = '';
    const transferNumber = `TRF-${Date.now()}`;

    this.inventoryService
      .createTransfer({
        transferNumber,
        productId: Number(raw.productId),
        fromLocationId: Number(raw.fromLocationId),
        toLocationId: Number(raw.toLocationId),
        quantity: Number(raw.quantity),
        unitCost: Number(raw.unitCost ?? 0),
        status: 'REQUESTED',
        reference: String(raw.reference ?? '').trim() || undefined,
        crossBorder: !!raw.crossBorder,
        createdByUserId: this.data.createdByUserId,
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (transfer) => this.dialogRef.close(transfer),
        error: (err: Error) => (this.submitError = err.message ?? 'Could not create transfer.'),
      });
  }

  private syncWarehouseSelections(): void {
    const fromId = this.fromLocationId;
    const stockedAtFrom = this.productStockLocations.some((row) => row.warehouseLocationId === fromId);
    if (!this.productSelected || !stockedAtFrom) {
      this.form.patchValue({ fromLocationId: null, toLocationId: null });
      return;
    }
    const toId = Number(this.form.get('toLocationId')?.value ?? 0);
    if (toId === fromId) {
      this.form.patchValue({ toLocationId: null });
    }
  }

  private applyCapturedUnitCost(): void {
    const capturedUnitCost = this.selectedFromStock?.unitCost ?? 0;
    if (capturedUnitCost > 0) {
      this.form.patchValue({ unitCost: capturedUnitCost });
    }
  }

  private updateQuantityValidators(): void {
    const control = this.form.get('quantity');
    if (!control) {
      return;
    }
    const validators = [Validators.required, Validators.min(0.01)];
    const maxQty = this.maxTransferQuantity;
    if (maxQty != null) {
      validators.push(Validators.max(maxQty));
    }
    control.setValidators(validators);
    control.updateValueAndValidity({ emitEvent: false });
  }
}
