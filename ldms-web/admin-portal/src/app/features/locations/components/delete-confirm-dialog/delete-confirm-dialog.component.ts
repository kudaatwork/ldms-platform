import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

type DeleteConfirmDialogData = {
  entityLabel?: string;
};

@Component({
  selector: 'app-delete-confirm-dialog',
  templateUrl: './delete-confirm-dialog.component.html',
  styleUrl: './delete-confirm-dialog.component.scss',
  standalone: false,
})
export class DeleteConfirmDialogComponent {
  readonly entityLabel: string;

  constructor(
    private readonly dialogRef: MatDialogRef<DeleteConfirmDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) data: DeleteConfirmDialogData | null,
  ) {
    this.entityLabel = (data?.entityLabel || 'record').trim();
  }

  close(confirm: boolean): void {
    this.dialogRef.close(confirm);
  }
}
