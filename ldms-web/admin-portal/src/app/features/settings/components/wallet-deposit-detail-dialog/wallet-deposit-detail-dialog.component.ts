import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import type { WalletDepositRow } from '../../services/platform-wallet-admin.service';
import { PlatformWalletAdminService } from '../../services/platform-wallet-admin.service';

export interface WalletDepositDetailDialogData {
  deposit: WalletDepositRow;
}

@Component({
  selector: 'app-wallet-deposit-detail-dialog',
  templateUrl: './wallet-deposit-detail-dialog.component.html',
  styleUrl: './wallet-deposit-detail-dialog.component.scss',
  standalone: false,
})
export class WalletDepositDetailDialogComponent {
  constructor(
    private readonly walletAdmin: PlatformWalletAdminService,
    private readonly dialogRef: MatDialogRef<WalletDepositDetailDialogComponent, void>,
    @Inject(MAT_DIALOG_DATA) readonly data: WalletDepositDetailDialogData,
  ) {}

  get deposit(): WalletDepositRow {
    return this.data.deposit;
  }

  formatMoney(cents?: number, currency?: string): string {
    return this.walletAdmin.formatCents(cents ?? 0, currency ?? 'USD');
  }

  formatWhen(iso?: string): string {
    return this.walletAdmin.formatWhen(iso);
  }

  close(): void {
    this.dialogRef.close();
  }
}
