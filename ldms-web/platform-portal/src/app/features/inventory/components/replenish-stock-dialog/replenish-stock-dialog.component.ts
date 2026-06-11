import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import { StockRow } from '../../models/inventory.model';

export interface ReplenishStockDialogData {
  stock: StockRow;
  adjustedByUserId: number;
}

@Component({
  selector: 'app-replenish-stock-dialog',
  templateUrl: './replenish-stock-dialog.component.html',
  styleUrl: './replenish-stock-dialog.component.scss',
  standalone: false,
})
export class ReplenishStockDialogComponent {
  form: FormGroup;
  submitting = false;
  submitError = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly dialogRef: MatDialogRef<ReplenishStockDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ReplenishStockDialogData,
  ) {
    this.dialogRef.disableClose = true;
    this.form = this.fb.group({
      quantity: [null, [Validators.required, Validators.min(0.01)]],
      unitCost: [null, [Validators.required, Validators.min(0)]],
      notes: [''],
    });
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close(false);
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.value;
    const notes = String(raw.notes ?? '').trim();
    this.submitting = true;
    this.submitError = '';

    this.inventoryService
      .replenishStock({
        inventoryItemId: this.data.stock.id,
        quantity: Number(raw.quantity),
        unitCost: Number(raw.unitCost),
        reason: notes || 'Stock replenishment',
        adjustedByUserId: this.data.adjustedByUserId,
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => this.dialogRef.close(true),
        error: (err: Error) => (this.submitError = err.message ?? 'Could not replenish stock.'),
      });
  }
}
