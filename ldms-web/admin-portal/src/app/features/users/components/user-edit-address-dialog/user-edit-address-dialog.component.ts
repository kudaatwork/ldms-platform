import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { UsersAdminService } from '../../services/users-admin.service';

export interface UserEditAddressDialogData {
  address: Record<string, unknown>;
}

@Component({
  selector: 'app-user-edit-address-dialog',
  templateUrl: './user-edit-address-dialog.component.html',
  styleUrl: './user-edit-address-dialog.component.scss',
  standalone: false,
})
export class UserEditAddressDialogComponent {
  saving = false;
  error = '';

  line1 = '';
  line2 = '';
  postalCode = '';
  suburbIdStr = '';
  readonly seedSuburbId: number | null;

  private readonly addressId: number;
  private readonly locationAddressId: number;
  private readonly existingGeoCoordinatesId?: number;

  constructor(
    private readonly dialogRef: MatDialogRef<UserEditAddressDialogComponent, boolean>,
    private readonly usersAdmin: UsersAdminService,
    private readonly snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) data: UserEditAddressDialogData,
  ) {
    const a = data.address;
    this.addressId = Number(a['id'] ?? 0);
    this.locationAddressId = Number(a['locationAddressId'] ?? 0);
    this.line1 = String(a['line1'] ?? '').trim();
    this.line2 = String(a['line2'] ?? '').trim();
    this.postalCode = String(a['postalCode'] ?? '').trim();
    const suburbRaw = a['suburbId'];
    const suburbNum = Number(suburbRaw ?? 0);
    this.seedSuburbId =
      suburbRaw != null && Number.isFinite(suburbNum) && suburbNum > 0 ? suburbNum : null;
    this.suburbIdStr = this.seedSuburbId != null ? String(this.seedSuburbId) : '';
    const geoRaw = Number(a['geoCoordinatesId'] ?? 0);
    this.existingGeoCoordinatesId =
      Number.isFinite(geoRaw) && geoRaw > 0 ? geoRaw : undefined;
  }

  close(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    this.error = '';
    if (!Number.isFinite(this.addressId) || this.addressId <= 0) {
      this.error = 'Invalid address id.';
      return;
    }
    if (!this.line1.trim() || !this.postalCode.trim()) {
      this.error = 'Line 1 and postal code are required.';
      return;
    }
    const suburbId = Number(this.suburbIdStr.trim());
    if (!Number.isFinite(suburbId)) {
      this.error = 'Select a suburb using country, province, district, and city.';
      return;
    }
    const locId = this.locationAddressId > 0 ? this.locationAddressId : undefined;
    this.saving = true;
    this.usersAdmin
      .updateUserAddress({
        id: this.addressId,
        ...(locId != null ? { locationAddressId: locId } : {}),
        line1: this.line1.trim(),
        ...(this.line2.trim() ? { line2: this.line2.trim() } : {}),
        postalCode: this.postalCode.trim(),
        suburbId,
        ...(this.existingGeoCoordinatesId != null ? { geoCoordinatesId: this.existingGeoCoordinatesId } : {}),
      })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.snackBar.open('Address updated.', 'Close', { duration: 4000 });
          this.dialogRef.close(true);
        },
        error: (err: unknown) => {
          this.error = this.formatHttpError(err);
        },
      });
  }

  private formatHttpError(err: unknown): string {
    const e = err as { error?: { message?: string; errorMessages?: string[] } };
    const msgs = e?.error?.errorMessages;
    if (Array.isArray(msgs) && msgs.length) {
      return msgs.map((m) => String(m)).join(' ');
    }
    if (typeof e?.error?.message === 'string' && e.error.message.trim()) {
      return e.error.message.trim();
    }
    return 'Update failed. Check suburb and location service.';
  }
}
