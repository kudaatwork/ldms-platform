import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { UsersAdminService } from '../../services/users-admin.service';

export interface UserEditAccountDialogData {
  account: Record<string, unknown>;
  userId: number;
}

@Component({
  selector: 'app-user-edit-account-dialog',
  templateUrl: './user-edit-account-dialog.component.html',
  styleUrl: './user-edit-account-dialog.component.scss',
  standalone: false,
})
export class UserEditAccountDialogComponent {
  saving = false;
  error = '';

  phoneNumber = '';
  accountNumber = '';
  isAccountLocked = false;

  private readonly accountId: number;
  private readonly userId: number;

  constructor(
    private readonly dialogRef: MatDialogRef<UserEditAccountDialogComponent, boolean>,
    private readonly usersAdmin: UsersAdminService,
    private readonly snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) data: UserEditAccountDialogData,
  ) {
    const a = data.account;
    this.accountId = Number(a['id'] ?? 0);
    this.userId = Number(data.userId ?? 0);
    this.phoneNumber = String(a['phoneNumber'] ?? '').trim();
    this.accountNumber = String(a['accountNumber'] ?? '').trim();
    this.isAccountLocked = a['isAccountLocked'] === true;
  }

  close(): void {
    this.dialogRef.close(false);
  }

  save(): void {
    this.error = '';
    if (!Number.isFinite(this.accountId) || this.accountId <= 0) {
      this.error = 'Invalid account id.';
      return;
    }
    if (!Number.isFinite(this.userId) || this.userId <= 0) {
      this.error = 'Invalid user id.';
      return;
    }
    this.saving = true;
    this.usersAdmin
      .updateUserAccount({
        id: this.accountId,
        userId: this.userId,
        phoneNumber: this.phoneNumber.trim() || undefined,
        accountNumber: this.accountNumber.trim() || undefined,
        isAccountLocked: this.isAccountLocked,
      })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.snackBar.open('Account updated.', 'Close', { duration: 4000 });
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
    return 'Update failed. Check phone number format.';
  }
}
