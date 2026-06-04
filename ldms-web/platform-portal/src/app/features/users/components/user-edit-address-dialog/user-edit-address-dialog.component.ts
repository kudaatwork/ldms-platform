import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { UsersPortalService } from '../../services/users-portal.service';

export interface UserEditAddressDialogData {
  address: Record<string, unknown>;
  /** Required when creating an address for a user who has none yet. */
  user?: Record<string, unknown>;
  createMode?: boolean;
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
  readonly createMode: boolean;

  line1 = '';
  line2 = '';
  postalCode = '';
  suburbIdStr = '';
  readonly seedSuburbId: number | null;

  private readonly addressId: number;
  private readonly locationAddressId: number;
  private readonly existingGeoCoordinatesId?: number;
  private readonly user?: Record<string, unknown>;

  constructor(
    private readonly dialogRef: MatDialogRef<UserEditAddressDialogComponent, boolean>,
    private readonly usersPortal: UsersPortalService,
    private readonly snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) data: UserEditAddressDialogData,
  ) {
    this.createMode = data.createMode === true || Number(data.address['id'] ?? 0) <= 0;
    this.user = data.user;
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
    if (!this.line1.trim() || !this.postalCode.trim()) {
      this.error = 'Line 1 and postal code are required.';
      return;
    }
    const suburbId = Number(this.suburbIdStr.trim());
    if (!Number.isFinite(suburbId) || suburbId <= 0) {
      this.error = 'Select a suburb using country, province, district, and city.';
      return;
    }

    const fields = {
      line1: this.line1.trim(),
      ...(this.line2.trim() ? { line2: this.line2.trim() } : {}),
      postalCode: this.postalCode.trim(),
      suburbId,
      ...(this.existingGeoCoordinatesId != null ? { geoCoordinatesId: this.existingGeoCoordinatesId } : {}),
    };

    if (this.createMode) {
      if (!this.user) {
        this.error = 'User context is missing.';
        return;
      }
      this.saving = true;
      this.usersPortal
        .upsertUserAddressForUser(this.user, fields)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: (resp) => {
            if (this.usersPortal.isUserMutationFailure(resp)) {
              this.error = this.usersPortal.formatUserMutationError(resp, 'Could not save address.');
              return;
            }
            this.snackBar.open('Address saved.', 'Close', { duration: 4000 });
            this.dialogRef.close(true);
          },
          error: (err: unknown) => {
            this.error = this.formatHttpError(err);
          },
        });
      return;
    }

    if (!Number.isFinite(this.addressId) || this.addressId <= 0) {
      this.error = 'Invalid address id.';
      return;
    }
    const locId = this.locationAddressId > 0 ? this.locationAddressId : undefined;
    this.saving = true;
    this.usersPortal
      .updateUserAddress({
        id: this.addressId,
        ...(locId != null ? { locationAddressId: locId } : {}),
        ...fields,
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
