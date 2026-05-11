import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export type DeleteConfirmDialogData = {
  entityLabel?: string;
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
  private readonly dialogData: DeleteConfirmDialogData | null;

  constructor(
    private readonly dialogRef: MatDialogRef<DeleteConfirmDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) data: DeleteConfirmDialogData | null,
  ) {
    this.dialogData = data;
    this.entityLabel = (data?.entityLabel || 'record').trim();
  }

  close(confirm: boolean): void {
    if (confirm) {
      this.dialogData?.onConfirm?.();
    }
    this.dialogRef.close(confirm);
  }
}
