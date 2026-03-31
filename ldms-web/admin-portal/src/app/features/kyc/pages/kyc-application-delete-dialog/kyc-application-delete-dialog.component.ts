import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface KycApplicationDeleteDialogData {
  applicant: string;
  id: string;
}

@Component({
  selector: 'app-kyc-application-delete-dialog',
  templateUrl: './kyc-application-delete-dialog.component.html',
  styleUrl: './kyc-application-delete-dialog.component.scss',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
})
export class KycApplicationDeleteDialogComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<
      KycApplicationDeleteDialogComponent,
      boolean | undefined
    >,
    @Inject(MAT_DIALOG_DATA) readonly data: KycApplicationDeleteDialogData,
  ) {}

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirmDelete(): void {
    this.dialogRef.close(true);
  }
}
