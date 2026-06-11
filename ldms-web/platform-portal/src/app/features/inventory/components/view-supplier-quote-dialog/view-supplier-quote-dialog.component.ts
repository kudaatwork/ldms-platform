import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../../../core/services/notification.service';
import { SharedModule } from '../../../../shared/shared.module';
import type { SupplierQuoteDetail } from '../../models/inventory.model';
import { InventoryPortalService } from '../../services/inventory-portal.service';

export type ViewSupplierQuoteDialogData = {
  requisitionId: number;
  requisitionNumber: string;
  quote?: SupplierQuoteDetail;
};

@Component({
  selector: 'app-view-supplier-quote-dialog',
  templateUrl: './view-supplier-quote-dialog.component.html',
  styleUrl: './view-supplier-quote-dialog.component.scss',
  standalone: true,
  imports: [SharedModule],
})
export class ViewSupplierQuoteDialogComponent implements OnInit {
  loading = true;
  error = '';
  quote: SupplierQuoteDetail | null = null;

  constructor(
    @Inject(MAT_DIALOG_DATA) readonly data: ViewSupplierQuoteDialogData,
    private readonly dialogRef: MatDialogRef<ViewSupplierQuoteDialogComponent>,
    private readonly inventory: InventoryPortalService,
    private readonly notifications: NotificationService,
  ) {}

  ngOnInit(): void {
    if (this.data.quote) {
      this.quote = this.data.quote;
      this.loading = false;
      return;
    }
    this.inventory
      .getQuoteByRequisition(this.data.requisitionId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (detail) => {
          this.quote = detail;
        },
        error: (err: Error) => {
          this.error = err.message ?? 'Could not load quotation.';
          this.notifications.error(this.error);
        },
      });
  }

  close(): void {
    this.dialogRef.close();
  }
}
