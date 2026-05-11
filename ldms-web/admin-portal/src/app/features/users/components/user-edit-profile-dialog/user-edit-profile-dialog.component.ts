import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { UsersAdminService } from '../../services/users-admin.service';

export interface UserEditProfileDialogData {
  user: Record<string, unknown>;
}

@Component({
  selector: 'app-user-edit-profile-dialog',
  templateUrl: './user-edit-profile-dialog.component.html',
  styleUrl: './user-edit-profile-dialog.component.scss',
  standalone: false,
})
export class UserEditProfileDialogComponent {
  saving = false;
  error = '';

  username = '';
  email = '';
  firstName = '';
  lastName = '';
  gender = '';
  phoneNumber = '';
  dateOfBirth = '';
  nationalIdNumber = '';
  nationalIdExpiryDate = '';
  passportNumber = '';
  passportExpiryDate = '';
  nationalIdUpload: File | null = null;
  passportUpload: File | null = null;
  /** Existing file-upload ids on the user (re-sent on save when no replacement file is chosen). */
  private readonly existingNationalIdUploadId?: number;
  private readonly existingPassportUploadId?: number;

  preferredLanguage = '';
  timezone = '';
  private readonly preferencesId: number;

  readonly genderOptions = ['MALE', 'FEMALE', 'OTHER'];

  private readonly userId: number;

  get hasPreferences(): boolean {
    return this.preferencesId > 0;
  }

  constructor(
    private readonly dialogRef: MatDialogRef<UserEditProfileDialogComponent, boolean>,
    private readonly usersAdmin: UsersAdminService,
    private readonly snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) data: UserEditProfileDialogData,
  ) {
    const u = data.user;
    this.userId = Number(u['id'] ?? 0);
    this.username = String(u['username'] ?? '').trim();
    this.email = String(u['email'] ?? '').trim();
    this.firstName = String(u['firstName'] ?? '').trim();
    this.lastName = String(u['lastName'] ?? '').trim();
    this.gender = String(u['gender'] ?? '').trim().toUpperCase();
    this.phoneNumber = String(u['phoneNumber'] ?? '').trim();
    this.dateOfBirth = this.dateOfBirthToInput(u['dateOfBirth']);
    this.nationalIdNumber = String(u['nationalIdNumber'] ?? '').trim();
    this.nationalIdExpiryDate = this.dateToInput(u['nationalIdExpiryDate']);
    this.passportNumber = String(u['passportNumber'] ?? '').trim();
    this.passportExpiryDate = this.dateToInput(u['passportExpiryDate']);
    const natUp = Number(u['nationalIdUploadId'] ?? 0);
    this.existingNationalIdUploadId = Number.isFinite(natUp) && natUp > 0 ? natUp : undefined;
    const passUp = Number(u['passportUploadId'] ?? 0);
    this.existingPassportUploadId = Number.isFinite(passUp) && passUp > 0 ? passUp : undefined;
    const pref = u['userPreferencesDto'];
    let pid = 0;
    if (pref !== null && typeof pref === 'object' && !Array.isArray(pref)) {
      const p = pref as Record<string, unknown>;
      pid = Number(p['id'] ?? 0);
      this.preferredLanguage = String(p['preferredLanguage'] ?? '').trim();
      this.timezone = String(p['timezone'] ?? '').trim();
    }
    this.preferencesId = Number.isFinite(pid) && pid > 0 ? pid : 0;
  }

  close(): void {
    this.dialogRef.close(false);
  }

  onNationalFile(ev: Event): void {
    const f = (ev.target as HTMLInputElement).files?.[0];
    this.nationalIdUpload = f ?? null;
  }

  onPassportFile(ev: Event): void {
    const f = (ev.target as HTMLInputElement).files?.[0];
    this.passportUpload = f ?? null;
  }

  save(): void {
    this.error = '';
    if (!Number.isFinite(this.userId) || this.userId <= 0) {
      this.error = 'Invalid user id.';
      return;
    }
    const required = [this.username, this.email, this.firstName, this.lastName, this.gender, this.phoneNumber, this.dateOfBirth];
    if (!required.every((v) => String(v).trim().length > 0)) {
      this.error = 'Username, email, names, gender, phone, and date of birth are required.';
      return;
    }
    if (this.preferencesId > 0) {
      const lang = this.preferredLanguage.trim();
      const tz = this.timezone.trim();
      if (!lang || !tz) {
        this.error = 'Preferred language and timezone are required when preferences exist on this user.';
        return;
      }
    }
    this.saving = true;
    this.usersAdmin
      .updateUser({
        id: this.userId,
        username: this.username.trim(),
        email: this.email.trim(),
        firstName: this.firstName.trim(),
        lastName: this.lastName.trim(),
        gender: this.gender.trim(),
        phoneNumber: this.phoneNumber.trim(),
        dateOfBirth: this.dateOfBirth.trim(),
        nationalIdNumber: this.nationalIdNumber.trim() || undefined,
        nationalIdExpiryDate: this.nationalIdExpiryDate.trim() || undefined,
        nationalIdUpload: this.nationalIdUpload ?? undefined,
        nationalIdUploadId:
          !this.nationalIdUpload && this.existingNationalIdUploadId ? this.existingNationalIdUploadId : undefined,
        passportNumber: this.passportNumber.trim() || undefined,
        passportExpiryDate: this.passportExpiryDate.trim() || undefined,
        passportUpload: this.passportUpload ?? undefined,
        passportUploadId: !this.passportUpload && this.existingPassportUploadId ? this.existingPassportUploadId : undefined,
      })
      .pipe(
        switchMap(() => {
          if (this.preferencesId <= 0) {
            return of(true);
          }
          const lang = this.preferredLanguage.trim();
          const tz = this.timezone.trim();
          if (!lang || !tz) {
            return of(true);
          }
          return this.usersAdmin.updateUserPreferences({
            id: this.preferencesId,
            userId: this.userId,
            preferredLanguage: lang,
            timezone: tz,
          });
        }),
        finalize(() => (this.saving = false)),
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Profile updated.', 'Close', { duration: 4000 });
          this.dialogRef.close(true);
        },
        error: (err: unknown) => {
          this.error = this.formatHttpError(err);
        },
      });
  }

  private dateOfBirthToInput(v: unknown): string {
    return this.dateToInput(v);
  }

  private dateToInput(v: unknown): string {
    if (v == null || v === '') {
      return '';
    }
    if (typeof v === 'string') {
      const s = v.trim();
      if (/^\d{4}-\d{2}-\d{2}/.test(s)) {
        return s.slice(0, 10);
      }
      const d = new Date(s);
      return Number.isNaN(d.getTime()) ? '' : d.toISOString().slice(0, 10);
    }
    if (Array.isArray(v) && v.length >= 3) {
      const y = Number(v[0]);
      const m = Number(v[1]);
      const day = Number(v[2]);
      if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(day)) {
        return '';
      }
      return `${String(y).padStart(4, '0')}-${String(m).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }
    return '';
  }

  private formatHttpError(err: unknown): string {
    const e = err as { error?: { message?: string; errorMessages?: string[] }; message?: string; status?: number };
    const msgs = e?.error?.errorMessages;
    if (Array.isArray(msgs) && msgs.length) {
      return msgs.map((m) => String(m)).join(' ');
    }
    if (typeof e?.error?.message === 'string' && e.error.message.trim()) {
      return e.error.message.trim();
    }
    return e?.message?.trim() || 'Update failed. Check required fields and try again.';
  }
}
