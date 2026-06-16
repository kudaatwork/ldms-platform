import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import type { InventoryDetailField } from '../inventory-detail-dialog/inventory-detail-dialog.component';
import type { TransferRow, TransferStatus } from '../../models/inventory.model';
import type { ShipmentRow } from '../../../trip-tracking/models/trip-tracking.model';
import { TripTrackingPortalService } from '../../../trip-tracking/services/trip-tracking-portal.service';
import { transferStatusCssClass } from '../../utils/inventory-status.util';

export type ViewTransferDialogResult =
  | { action: 'approved' }
  | { action: 'rejected'; reason: string };

export interface ViewTransferDialogData {
  transfer: TransferRow;
  fields: InventoryDetailField[];
  canApprove: boolean;
  canReject: boolean;
}

interface TransferWorkflowStep {
  key: TransferStatus;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-view-transfer-dialog',
  templateUrl: './view-transfer-dialog.component.html',
  styleUrl: './view-transfer-dialog.component.scss',
  standalone: false,
})
export class ViewTransferDialogComponent implements OnInit, OnDestroy {
  rejectMode = false;
  rejectionReason = '';
  rejectionError = '';

  shipment: ShipmentRow | null = null;
  shipmentLoading = false;

  readonly workflowSteps: TransferWorkflowStep[] = [
    { key: 'REQUESTED', label: 'Requested', icon: 'inbox' },
    { key: 'APPROVED', label: 'Approved', icon: 'verified' },
    { key: 'IN_TRANSIT', label: 'In transit', icon: 'local_shipping' },
    { key: 'COMPLETED', label: 'Completed', icon: 'done_all' },
  ];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly dialogRef: MatDialogRef<ViewTransferDialogComponent, ViewTransferDialogResult | undefined>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ViewTransferDialogData,
    private readonly tripTracking: TripTrackingPortalService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    if (this.canTrackShipment) {
      this.shipmentLoading = true;
      this.tripTracking
        .getShipmentByTransfer(this.data.transfer.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (s) => {
            this.shipment = s;
            this.shipmentLoading = false;
          },
          error: () => (this.shipmentLoading = false),
        });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get transfer(): TransferRow {
    return this.data.transfer;
  }

  get statusClass(): string {
    return transferStatusCssClass(this.transfer.status);
  }

  get isRejected(): boolean {
    return this.transfer.status === 'REJECTED';
  }

  get isCancelled(): boolean {
    return this.transfer.status === 'CANCELLED';
  }

  get quantityLabel(): string {
    const qty = this.transfer.quantity;
    const uom = this.transfer.unitOfMeasure?.trim();
    return uom ? `${qty} ${uom}` : String(qty);
  }

  get showWorkflowActions(): boolean {
    return this.data.canApprove || this.data.canReject;
  }

  get canTrackShipment(): boolean {
    const trackableStatuses: Array<TransferStatus> = ['APPROVED', 'IN_TRANSIT', 'COMPLETED'];
    return trackableStatuses.includes(this.transfer.status);
  }

  get metadataFields(): InventoryDetailField[] {
    const skip = new Set(['Reference', 'Product', 'Quantity', 'From warehouse', 'To warehouse', 'Status', 'Rejection reason', 'Rejected at']);
    return this.data.fields.filter((f) => !skip.has(f.label));
  }

  workflowStepState(step: TransferWorkflowStep): 'done' | 'active' | 'upcoming' | 'failed' {
    const status = this.transfer.status;
    const order: TransferStatus[] = ['REQUESTED', 'APPROVED', 'IN_TRANSIT', 'COMPLETED'];
    const currentIndex = order.indexOf(status);
    const stepIndex = order.indexOf(step.key);

    if (this.isRejected && step.key === 'REQUESTED') {
      return 'done';
    }
    if (this.isRejected && step.key === 'APPROVED') {
      return 'failed';
    }
    if (this.isCancelled && step.key === 'REQUESTED') {
      return 'done';
    }
    if (this.isCancelled) {
      return step.key === 'REQUESTED' ? 'done' : 'upcoming';
    }

    if (currentIndex < 0) {
      return 'upcoming';
    }
    if (stepIndex < currentIndex) {
      return 'done';
    }
    if (stepIndex === currentIndex) {
      return 'active';
    }
    return 'upcoming';
  }

  trackShipment(): void {
    this.dialogRef.close();
    void this.router.navigate(['/shipments/shipments'], {
      queryParams: { transferId: this.transfer.id },
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  approve(): void {
    this.dialogRef.close({ action: 'approved' });
  }

  startReject(): void {
    this.rejectMode = true;
    this.rejectionError = '';
  }

  cancelReject(): void {
    this.rejectMode = false;
    this.rejectionReason = '';
    this.rejectionError = '';
  }

  confirmReject(): void {
    const reason = this.rejectionReason.trim();
    if (reason.length < 3) {
      this.rejectionError = 'Enter a rejection reason (at least 3 characters).';
      return;
    }
    this.rejectionError = '';
    this.dialogRef.close({ action: 'rejected', reason });
  }
}
