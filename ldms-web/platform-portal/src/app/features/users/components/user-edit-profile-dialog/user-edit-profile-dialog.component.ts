import { Component, Inject, Optional } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize, switchMap } from 'rxjs/operators';
import { of, throwError } from 'rxjs';
import { UsersPortalService } from '../../services/users-portal.service';
import {
  dateOfBirthMinimumAgeMessage,
  isDateOfBirthAtLeastMinimumAge,
  maximumDateOfBirthInput,
} from '@core/utils/date-of-birth.util';

export interface UserEditProfileDialogData {
  user?: Record<string, unknown> | null;
  /** When `profile-only`, hides address, security, documents, and admin-only sections. */
  scope?: 'full' | 'profile-only';
}

function coerceUserRecord(raw: unknown): Record<string, unknown> {
  if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
    return raw as Record<string, unknown>;
  }
  return {};
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
  emailVerified = false;
  phoneVerified = false;
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
  organizationKycApprover = false;
  private readonly preferencesId: number;
  private readonly organizationId?: number;

  readonly genderOptions = ['MALE', 'FEMALE', 'PREFER_NOT_TO_SAY', 'NON_BINARY'];

  addressLine1 = '';
  addressLine2 = '';
  postalCode = '';
  suburbIdStr = '';
  readonly seedSuburbId: number | null;

  securityQuestion1 = '';
  securityAnswer1 = '';
  securityQuestion2 = '';
  securityAnswer2 = '';
  twoFactorAuthSecret = '';
  isTwoFactorEnabled = false;
  private securityId = 0;
  private readonly initialSecuritySnapshot: {
    q1: string;
    a1: string;
    q2: string;
    a2: string;
    secret: string;
    enabled: boolean;
  };

  private readonly userId: number;
  readonly scope: 'full' | 'profile-only';

  get hasPreferences(): boolean {
    return this.preferencesId > 0;
  }

  /** KYC approver flag only applies to admin-portal users with no organisation. */
  get canEditKycApprover(): boolean {
    return !this.organizationId;
  }

  get maximumDateOfBirth(): string {
    return maximumDateOfBirthInput();
  }

  constructor(
    private readonly dialogRef: MatDialogRef<UserEditProfileDialogComponent, boolean>,
    private readonly usersPortal: UsersPortalService,
    private readonly snackBar: MatSnackBar,
    @Optional() @Inject(MAT_DIALOG_DATA) data: UserEditProfileDialogData | null,
  ) {
    const safeData = data ?? { user: {} };
    this.scope = safeData.scope === 'profile-only' ? 'profile-only' : 'full';
    const u = coerceUserRecord(safeData.user);
    this.userId = Number(u['id'] ?? 0);
    const orgId = Number(u['organizationId'] ?? 0);
    this.organizationId = Number.isFinite(orgId) && orgId > 0 ? orgId : undefined;
    this.organizationKycApprover = this.readOrganizationKycApprover(u['organizationKycApprover']);
    this.username = String(u['username'] ?? '').trim();
    this.email = String(u['email'] ?? '').trim();
    this.firstName = String(u['firstName'] ?? '').trim();
    this.lastName = String(u['lastName'] ?? '').trim();
    const rawGender = String(u['gender'] ?? '').trim().toUpperCase();
    this.gender = rawGender === 'OTHER' ? 'PREFER_NOT_TO_SAY' : rawGender;
    this.phoneNumber = String(u['phoneNumber'] ?? '').trim();
    this.emailVerified = u['emailVerified'] === true;
    this.phoneVerified = u['phoneVerified'] === true;
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
    this.seedSuburbId = null;
    const ad = u['addressDto'];
    if (ad !== null && typeof ad === 'object' && !Array.isArray(ad)) {
      const address = ad as Record<string, unknown>;
      this.addressLine1 = String(address['line1'] ?? '').trim();
      this.addressLine2 = String(address['line2'] ?? '').trim();
      this.postalCode = String(address['postalCode'] ?? '').trim();
      const suburbRaw = address['suburbId'];
      const suburbNum = Number(suburbRaw ?? 0);
      this.seedSuburbId =
        suburbRaw != null && Number.isFinite(suburbNum) && suburbNum > 0 ? suburbNum : null;
      this.suburbIdStr = this.seedSuburbId != null ? String(this.seedSuburbId) : '';
    } else {
      this.seedSuburbId = null;
    }
    const sec = u['userSecurityDto'];
    if (sec !== null && typeof sec === 'object' && !Array.isArray(sec)) {
      this.applySecurityRecord(sec as Record<string, unknown>);
    }
    this.initialSecuritySnapshot = this.currentSecuritySnapshot();
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
    if (!isDateOfBirthAtLeastMinimumAge(this.dateOfBirth)) {
      this.error = dateOfBirthMinimumAgeMessage();
      return;
    }
    if (this.scope !== 'profile-only' && this.preferencesId > 0) {
      const lang = this.preferredLanguage.trim();
      const tz = this.timezone.trim();
      if (!lang || !tz) {
        this.error = 'Preferred language and timezone are required when preferences exist on this user.';
        return;
      }
    }
    const suburbId = this.suburbIdStr.trim() ? Number(this.suburbIdStr.trim()) : NaN;
    const hasAddressInput =
      this.scope !== 'profile-only' &&
      (this.addressLine1.trim().length > 0 ||
        this.postalCode.trim().length > 0 ||
        this.suburbIdStr.trim().length > 0);
    if (hasAddressInput) {
      if (!this.addressLine1.trim() || !this.postalCode.trim() || !Number.isFinite(suburbId)) {
        this.error = 'Address requires line 1, postal code, and a selected suburb.';
        return;
      }
    }
    const securityPayload = this.scope === 'profile-only' ? null : this.buildSecuritySavePayload();
    if (securityPayload === 'invalid') {
      return;
    }
    this.saving = true;
    this.usersPortal
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
        ...(hasAddressInput
          ? {
              addressLine1: this.addressLine1.trim(),
              addressLine2: this.addressLine2.trim() || undefined,
              postalCode: this.postalCode.trim(),
              suburbId,
            }
          : {}),
      })
      .pipe(
        switchMap((userResp) => {
          if (this.usersPortal.isUserMutationFailure(userResp)) {
            return throwError(() => userResp);
          }
          if (this.scope === 'profile-only' || !this.canEditKycApprover) {
            return of(userResp);
          }
          return this.usersPortal.setOrganizationKycApprover(this.userId, this.organizationKycApprover).pipe(
            switchMap((kycResp) => {
              if (this.usersPortal.isUserMutationFailure(kycResp)) {
                return throwError(() => kycResp);
              }
              return of(userResp);
            }),
          );
        }),
        switchMap((userResp) => {
          if (this.scope === 'profile-only' || this.preferencesId <= 0) {
            return of(userResp);
          }
          const lang = this.preferredLanguage.trim();
          const tz = this.timezone.trim();
          if (!lang || !tz) {
            return of(userResp);
          }
          return this.usersPortal.updateUserPreferences({
            id: this.preferencesId,
            userId: this.userId,
            preferredLanguage: lang,
            timezone: tz,
          });
        }),
        switchMap((prefResp) => {
          if (this.usersPortal.isUserMutationFailure(prefResp)) {
            return throwError(() => prefResp);
          }
          return of(prefResp);
        }),
        switchMap((prev) => {
          if (!securityPayload) {
            return of(prev);
          }
          const save$ =
            this.securityId > 0
              ? this.usersPortal.updateUserSecurity({ id: this.securityId, ...securityPayload })
              : this.usersPortal.createUserSecurity(securityPayload);
          return save$;
        }),
        switchMap((secResp) => {
          if (this.usersPortal.isUserMutationFailure(secResp)) {
            return throwError(() => secResp);
          }
          return of(secResp);
        }),
        finalize(() => (this.saving = false)),
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Profile updated.', 'Close', { duration: 4000 });
          this.dialogRef.close(true);
        },
        error: (err: unknown) => {
          if (this.usersPortal.isUserMutationFailure(err)) {
            this.error = this.usersPortal.formatUserMutationError(
              err,
              'Update failed. Check required fields and try again.',
            );
            return;
          }
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

  /**
   * Returns payload when security should be saved, null when skipped, `'invalid'` when validation failed.
   */
  private buildSecuritySavePayload():
    | null
    | 'invalid'
    | {
        userId: number;
        securityQuestion_1: string;
        securityAnswer_1: string;
        securityQuestion_2: string;
        securityAnswer_2: string;
        twoFactorAuthSecret: string;
        isTwoFactorEnabled: boolean;
      } {
    const q1 = this.securityQuestion1.trim();
    const a1 = this.securityAnswer1.trim();
    const q2 = this.securityQuestion2.trim();
    const a2 = this.securityAnswer2.trim();
    const secret = this.twoFactorAuthSecret.trim();
    const anyField = !!(q1 || a1 || q2 || a2 || secret);
    if (this.securityId > 0 && !this.securityFieldsChanged()) {
      return null;
    }
    if (this.securityId <= 0 && !anyField) {
      return null;
    }
    if (!q1 || !a1 || !a2) {
      this.error =
        'Security: question 1, answer 1, and answer 2 are required when updating security (same as create user).';
      return 'invalid';
    }
    if (!secret) {
      this.error =
        'Security: two-factor secret is required. Generate one or paste the existing secret from the profile.';
      return 'invalid';
    }
    return {
      userId: this.userId,
      securityQuestion_1: q1,
      securityAnswer_1: a1,
      securityQuestion_2: q2,
      securityAnswer_2: a2,
      twoFactorAuthSecret: secret,
      isTwoFactorEnabled: this.isTwoFactorEnabled,
    };
  }

  private currentSecuritySnapshot(): {
    q1: string;
    a1: string;
    q2: string;
    a2: string;
    secret: string;
    enabled: boolean;
  } {
    return {
      q1: this.securityQuestion1,
      a1: this.securityAnswer1,
      q2: this.securityQuestion2,
      a2: this.securityAnswer2,
      secret: this.twoFactorAuthSecret,
      enabled: this.isTwoFactorEnabled,
    };
  }

  private securityFieldsChanged(): boolean {
    const cur = this.currentSecuritySnapshot();
    const init = this.initialSecuritySnapshot;
    return (
      cur.q1 !== init.q1 ||
      cur.a1 !== init.a1 ||
      cur.q2 !== init.q2 ||
      cur.a2 !== init.a2 ||
      cur.secret !== init.secret ||
      cur.enabled !== init.enabled
    );
  }

  private applySecurityRecord(s: Record<string, unknown>): void {
    this.securityQuestion1 = this.pickStr(s, 'securityQuestion_1', 'securityQuestion1');
    this.securityAnswer1 = this.pickStr(s, 'securityAnswer_1', 'securityAnswer1');
    this.securityQuestion2 = this.pickStr(s, 'securityQuestion_2', 'securityQuestion2');
    this.securityAnswer2 = this.pickStr(s, 'securityAnswer_2', 'securityAnswer2');
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

  private readOrganizationKycApprover(raw: unknown): boolean {
    if (raw === true) {
      return true;
    }
    if (typeof raw === 'string') {
      return raw.trim().toLowerCase() === 'true';
    }
    return false;
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
