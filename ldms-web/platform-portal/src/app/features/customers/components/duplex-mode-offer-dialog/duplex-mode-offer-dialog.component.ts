import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export type DuplexModeOfferDialogData = {
  organizationName: string;
  organizationEmail?: string;
  /** Existing customer — link only; supplier — offer duplex. */
  linkMode: 'DUPLEX_OFFERED' | 'LINKABLE_CUSTOMER';
};

export type DuplexModeOfferDialogResult =
  | { action: 'link'; enableDuplexMode: boolean }
  | { action: 'cancel' };

@Component({
  selector: 'app-duplex-mode-offer-dialog',
  templateUrl: './duplex-mode-offer-dialog.component.html',
  styleUrl: './duplex-mode-offer-dialog.component.scss',
  standalone: false,
})
export class DuplexModeOfferDialogComponent {
  readonly isDuplexOffer: boolean;

  constructor(
    private readonly dialogRef: MatDialogRef<DuplexModeOfferDialogComponent, DuplexModeOfferDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: DuplexModeOfferDialogData,
  ) {
    this.isDuplexOffer = data.linkMode === 'DUPLEX_OFFERED';
    this.dialogRef.disableClose = true;
  }

  cancel(): void {
    this.dialogRef.close({ action: 'cancel' });
  }

  linkWithDuplex(): void {
    this.dialogRef.close({ action: 'link', enableDuplexMode: true });
  }

  linkExistingCustomer(): void {
    this.dialogRef.close({ action: 'link', enableDuplexMode: false });
  }
}
