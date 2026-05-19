import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { UsersAdminService } from '../../services/users-admin.service';

export interface UserEditSecurityDialogData {
  security: Record<string, unknown>;
  userId: number;
  /** `recovery` from Preferences (Q&A); `full` from Security details (all fields). */
  emphasis?: 'recovery' | 'full';
}

@Component({
  selector: 'app-user-edit-security-dialog',
  templateUrl: './user-edit-security-dialog.component.html',
  styleUrl: './user-edit-security-dialog.component.scss',
  standalone: false,
})
export class UserEditSecurityDialogComponent implements OnInit {
  saving = false;
  loading = true;
  error = '';

  readonly emphasis: 'recovery' | 'full';
  readonly dialogTitle: string;

  securityQuestion_1 = '';
  securityAnswer_1 = '';
  securityQuestion_2 = '';
  securityAnswer_2 = '';
  twoFactorAuthSecret = '';
  isTwoFactorEnabled = false;

  private securityId = 0;
  private readonly initialUserId: number;

  constructor(
    private readonly dialogRef: MatDialogRef<UserEditSecurityDialogComponent, boolean>,
    private readonly usersAdmin: UsersAdminService,
    private readonly snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) private readonly data: UserEditSecurityDialogData,
  ) {
    this.emphasis = data.emphasis ?? 'full';
    this.dialogTitle =
      this.emphasis === 'recovery'
        ? 'Edit security questions & answers'
        : 'Edit security & authentication';
    this.securityId = Number(data.security['id'] ?? 0);
    this.initialUserId = Number(data.userId ?? 0);
    this.applySecurityRecord(data.security);
  }

  ngOnInit(): void {
    if (Number.isFinite(this.securityId) && this.securityId > 0) {
      this.usersAdmin.getUserSecurityById(this.securityId).subscribe({
        next: (row) => {
          if (row) {
            this.applySecurityRecord(row);
          }
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error =
            'Could not reload security from the server. You can still edit using the values already on this screen.';
        },
      });
    } else {
      this.loading = false;
      this.error =
        'No security record exists yet for this user. Fill in the details below and Save to create one.';
    }
  }

  close(): void {
    this.dialogRef.close(false);
  }

  generateTwoFactorSecret(): void {
    const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    const targetLen = 32;
    const bytes = new Uint8Array(targetLen);
    if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
      crypto.getRandomValues(bytes);
    } else {
      for (let i = 0; i < targetLen; i++) {
        bytes[i] = Math.floor(Math.random() * 256);
      }
    }
    let out = '';
    for (let i = 0; i < targetLen; i++) {
      out += alphabet[bytes[i] % alphabet.length];
    }
    this.twoFactorAuthSecret = out;
    this.error = '';
    this.snackBar.open('Generated a new authenticator secret.', 'Close', { duration: 3000 });
  }

  save(): void {
    this.error = '';
    if (!Number.isFinite(this.initialUserId) || this.initialUserId <= 0) {
      this.error = 'Invalid user id.';
      return;
    }
    if (!this.securityQuestion_1.trim() || !this.securityAnswer_1.trim() || !this.securityAnswer_2.trim()) {
      this.error = 'Question 1, answer 1, and answer 2 are required.';
      return;
    }
    if (!this.twoFactorAuthSecret.trim()) {
      this.error =
        'Two-factor secret is required by the API. If it stayed empty after load, refresh the profile or paste the current base32 secret.';
      return;
    }
    this.saving = true;
    const q2 = this.securityQuestion_2.trim();
    const basePayload = {
      userId: this.initialUserId,
      securityQuestion_1: this.securityQuestion_1.trim(),
      securityAnswer_1: this.securityAnswer_1.trim(),
      securityQuestion_2: q2,
      securityAnswer_2: this.securityAnswer_2.trim(),
      twoFactorAuthSecret: this.twoFactorAuthSecret.trim(),
      isTwoFactorEnabled: this.isTwoFactorEnabled,
    };
    const save$ =
      Number.isFinite(this.securityId) && this.securityId > 0
        ? this.usersAdmin.updateUserSecurity({ id: this.securityId, ...basePayload })
        : this.usersAdmin.createUserSecurity(basePayload);
    const okMessage =
      Number.isFinite(this.securityId) && this.securityId > 0
        ? 'Security settings updated.'
        : 'Security settings created.';
    save$
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.snackBar.open(okMessage, 'Close', { duration: 4000 });
          this.dialogRef.close(true);
        },
        error: (err: unknown) => {
          this.error = this.formatHttpError(err);
        },
      });
  }

  private applySecurityRecord(s: Record<string, unknown>): void {
    this.securityQuestion_1 = this.pickStr(s, 'securityQuestion_1', 'securityQuestion1');
    this.securityAnswer_1 = this.pickStr(s, 'securityAnswer_1', 'securityAnswer1');
    this.securityQuestion_2 = this.pickStr(s, 'securityQuestion_2', 'securityQuestion2');
    this.securityAnswer_2 = this.pickStr(s, 'securityAnswer_2', 'securityAnswer2');
    this.twoFactorAuthSecret = this.pickStr(s, 'twoFactorAuthSecret', 'twoFactorSecret');
    const t = s['isTwoFactorEnabled'];
    this.isTwoFactorEnabled = t === true || t === 'true';
    const id = Number(s['id'] ?? 0);
    if (Number.isFinite(id) && id > 0) {
      this.securityId = id;
    }
  }

  private pickStr(s: Record<string, unknown>, ...keys: string[]): string {
    for (const k of keys) {
      const v = s[k];
      if (v != null && String(v).trim() !== '') {
        return String(v).trim();
      }
    }
    return '';
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
    return 'Update failed. Check required fields.';
  }
}
