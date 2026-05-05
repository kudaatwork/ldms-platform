import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

type ChurnOutConfirmDialogData = {
  entityLabel?: string;
};

@Component({
  selector: 'app-churn-out-confirm-dialog',
  templateUrl: './churn-out-confirm-dialog.component.html',
  styleUrl: './churn-out-confirm-dialog.component.scss',
  standalone: false,
})
export class ChurnOutConfirmDialogComponent {
  readonly entityLabel: string;

  constructor(
    private readonly dialogRef: MatDialogRef<ChurnOutConfirmDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) data: ChurnOutConfirmDialogData | null,
  ) {
    this.entityLabel = (data?.entityLabel || 'request logs').trim();
  }

  close(confirm: boolean): void {
    this.dialogRef.close(confirm);
  }
}
