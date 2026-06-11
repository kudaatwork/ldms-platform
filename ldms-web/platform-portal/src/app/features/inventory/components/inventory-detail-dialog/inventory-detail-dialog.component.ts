import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface InventoryDetailField {
  label: string;
  value: string;
}

export interface InventoryDetailDialogData {
  title: string;
  subtitle?: string;
  fields: InventoryDetailField[];
  width?: string;
}

@Component({
  selector: 'app-inventory-detail-dialog',
  templateUrl: './inventory-detail-dialog.component.html',
  styleUrl: './inventory-detail-dialog.component.scss',
  standalone: false,
})
export class InventoryDetailDialogComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<InventoryDetailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public readonly data: InventoryDetailDialogData,
  ) {}

  close(): void {
    this.dialogRef.close();
  }
}
