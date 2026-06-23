import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export type DeleteConfirmDialogData = {
  entityLabel?: string;
  title?: string;
  message?: string;
  confirmLabel?: string;
  /** When false, confirm button uses primary styling instead of danger. */
  confirmDanger?: boolean;
  /** Runs synchronously when the user confirms, before the dialog closes (instant parent UI updates). */
  onConfirm?: () => void;
};

@Component({
  selector: 'app-delete-confirm-dialog',
  templateUrl: './delete-confirm-dialog.component.html',
  styleUrl: './delete-confirm-dialog.component.scss',
  standalone: false,
})
export class DeleteConfirmDialogComponent {
  readonly entityLabel: string;
  readonly title: string;
  readonly message: string;
  readonly confirmLabel: string;
  readonly confirmDanger: boolean;
  private readonly dialogData: DeleteConfirmDialogData | null;

  constructor(
    private readonly dialogRef: MatDialogRef<DeleteConfirmDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) data: DeleteConfirmDialogData | null,
  ) {
    this.dialogData = data;
    this.entityLabel = (data?.entityLabel || 'record').trim();
    this.title = (data?.title || `Delete ${this.entityLabel}?`).trim();
    this.message =
      (data?.message || 'This action cannot be undone. Are you sure you want to continue?').trim();
    this.confirmLabel = (data?.confirmLabel || 'Delete').trim();
    this.confirmDanger = data?.confirmDanger !== false;
  }

  close(confirm: boolean): void {
    if (confirm) {
      this.dialogData?.onConfirm?.();
    }
    this.dialogRef.close(confirm);
  }
}
