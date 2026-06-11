import { Component, Inject, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import { PurchaseOrderLineRow, PurchaseOrderRow, WarehouseRow } from '../../models/inventory.model';

export interface ReceiveGoodsDialogData {
  order: PurchaseOrderRow;
  warehouses: WarehouseRow[];
  receivedByUserId: number;
}

@Component({
  selector: 'app-receive-goods-dialog',
  templateUrl: './receive-goods-dialog.component.html',
  styleUrl: './receive-goods-dialog.component.scss',
  standalone: false,
})
export class ReceiveGoodsDialogComponent implements OnInit {
  form: FormGroup;
  loadingLines = true;
  loadError = '';
  submitting = false;
  submitError = '';
  lines: PurchaseOrderLineRow[] = [];

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly dialogRef: MatDialogRef<ReceiveGoodsDialogComponent, string>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ReceiveGoodsDialogData,
  ) {
    this.dialogRef.disableClose = true;
    this.form = this.fb.group({
      warehouseLocationId: [null, Validators.required],
      notes: [''],
      lines: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    this.inventoryService
      .listPurchaseOrderLines()
      .pipe(finalize(() => (this.loadingLines = false)))
      .subscribe({
        next: (allLines) => {
          this.lines = allLines.filter((line) => line.purchaseOrderId === this.data.order.id && line.remainingQuantity > 0);
          for (const line of this.lines) {
            this.lineItems.push(
              this.fb.group({
                purchaseOrderLineId: [line.id],
                productLabel: [{ value: line.productName || `Product #${line.productId}`, disabled: true }],
                remainingQuantity: [{ value: line.remainingQuantity, disabled: true }],
                quantityReceived: [line.remainingQuantity, [Validators.required, Validators.min(0.01), Validators.max(line.remainingQuantity)]],
                reason: [''],
              }),
            );
          }
          if (!this.lines.length) {
            this.loadError = 'No open lines remain on this purchase order.';
          }
        },
        error: (err: Error) => (this.loadError = err.message ?? 'Could not load order lines.'),
      });
  }

  get lineItems(): FormArray {
    return this.form.get('lines') as FormArray;
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  onSubmit(): void {
    if (this.form.invalid || !this.lines.length) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const receivedItems = (raw.lines as Array<Record<string, unknown>>)
      .map((line) => ({
        purchaseOrderLineId: Number(line['purchaseOrderLineId']),
        quantityReceived: Number(line['quantityReceived']),
        reason: String(line['reason'] ?? '').trim() || undefined,
      }))
      .filter((line) => line.quantityReceived > 0);

    if (!receivedItems.length) {
      this.submitError = 'Enter a quantity to receive for at least one line.';
      return;
    }

    this.submitting = true;
    this.submitError = '';

    this.inventoryService
      .receiveGoods({
        purchaseOrderId: this.data.order.id,
        warehouseLocationId: Number(raw.warehouseLocationId),
        receivedByUserId: this.data.receivedByUserId,
        notes: String(raw.notes ?? '').trim() || undefined,
        idempotencyKey: crypto.randomUUID(),
        receivedItems,
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (message) => this.dialogRef.close(message),
        error: (err: Error) => (this.submitError = err.message ?? 'Could not record goods received.'),
      });
  }
}
