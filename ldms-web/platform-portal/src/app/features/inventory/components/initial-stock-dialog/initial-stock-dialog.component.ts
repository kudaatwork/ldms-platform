import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import { ProductRow, StockRow, WarehouseRow } from '../../models/inventory.model';

export interface InitialStockDialogData {
  products: ProductRow[];
  warehouses: WarehouseRow[];
  existingStock: StockRow[];
  supplierId: number;
  createdByUserId: number;
  prefill?: {
    productId?: number;
    warehouseLocationId?: number;
  };
}

@Component({
  selector: 'app-initial-stock-dialog',
  templateUrl: './initial-stock-dialog.component.html',
  styleUrl: './initial-stock-dialog.component.scss',
  standalone: false,
})
export class InitialStockDialogComponent {
  form: FormGroup;
  submitting = false;
  submitError = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly dialogRef: MatDialogRef<InitialStockDialogComponent, StockRow>,
    @Inject(MAT_DIALOG_DATA) public readonly data: InitialStockDialogData,
  ) {
    this.dialogRef.disableClose = true;
    const prefillProductId = data.prefill?.productId ?? null;
    const prefillWarehouseId = data.prefill?.warehouseLocationId ?? null;
    this.form = this.fb.group({
      productId: [prefillProductId, Validators.required],
      warehouseLocationId: [prefillWarehouseId, Validators.required],
      quantity: [null, [Validators.required, Validators.min(0.01)]],
      unitCost: [null, [Validators.required, Validators.min(0)]],
      minStockLevel: [0, [Validators.min(0)]],
      reorderQuantity: [0, [Validators.min(0)]],
      notes: [''],
    });
  }

  get availableProducts(): ProductRow[] {
    return this.data.products.filter((product) =>
      this.data.warehouses.some((warehouse) => this.canSetInitialStock(product.id, warehouse.id)),
    );
  }

  get selectedProductId(): number {
    return Number(this.form.get('productId')?.value ?? 0);
  }

  get productSelected(): boolean {
    return this.selectedProductId > 0;
  }

  get warehousePickerHint(): string {
    if (!this.productSelected) {
      return 'Select a product first, then search for a warehouse.';
    }
    if (!this.data.warehouses.some((warehouse) => this.canSetInitialStock(this.selectedProductId, warehouse.id))) {
      return 'This product already has stock at every warehouse. Use replenishment instead.';
    }
    return '';
  }

  isWarehouseIneligible = (warehouse: WarehouseRow): boolean => {
    if (!this.productSelected) {
      return true;
    }
    return !this.canSetInitialStock(this.selectedProductId, warehouse.id);
  };

  onProductChange(): void {
    const productId = this.selectedProductId;
    const warehouseId = Number(this.form.get('warehouseLocationId')?.value ?? 0);
    if (productId && warehouseId && !this.canSetInitialStock(productId, warehouseId)) {
      this.form.patchValue({ warehouseLocationId: null });
    }
  }

  private canSetInitialStock(productId: number, warehouseLocationId: number): boolean {
    const row = this.data.existingStock.find(
      (item) => item.productId === productId && item.warehouseLocationId === warehouseLocationId,
    );
    return !row || row.quantityOnHand <= 0;
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
    const productId = Number(raw.productId);
    const warehouseLocationId = Number(raw.warehouseLocationId);
    if (!this.canSetInitialStock(productId, warehouseLocationId)) {
      this.submitError = 'Initial stock is already set for this product at this warehouse. Use replenishment instead.';
      return;
    }

    this.submitting = true;
    this.submitError = '';

    this.inventoryService
      .createInitialStock({
        productId,
        warehouseLocationId,
        supplierId: this.data.supplierId,
        quantity: Number(raw.quantity),
        unitCost: Number(raw.unitCost),
        minStockLevel: Number(raw.minStockLevel ?? 0) || undefined,
        reorderQuantity: Number(raw.reorderQuantity ?? 0) || undefined,
        notes: String(raw.notes ?? '').trim() || undefined,
        createdByUserId: this.data.createdByUserId,
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (stock) => this.dialogRef.close(stock),
        error: (err: Error) => (this.submitError = err.message ?? 'Could not record initial stock.'),
      });
  }
}
