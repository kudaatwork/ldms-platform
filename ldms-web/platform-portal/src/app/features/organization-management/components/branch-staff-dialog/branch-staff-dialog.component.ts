import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';
import { maximumDateOfBirthInput } from '../../../../core/utils/date-of-birth.util';
import { UsersPortalService, UserListRow } from '../../../users/services/users-portal.service';
import { BranchDetail } from '../../../../core/services/organization.service';

export type BranchStaffRole = 'manager' | 'clerk';

export interface BranchStaffDialogData {
  /** Fixed branch (when launched from a single branch). Omit to show a branch picker. */
  branch?: BranchDetail;
  /** Branch options for the picker (workforce pages). */
  branches?: BranchDetail[];
  organizationId: number;
  role: BranchStaffRole;
  /** When provided, the dialog edits this existing user instead of creating one. */
  user?: UserListRow;
}

export interface BranchStaffDialogResult {
  created: boolean;
  updated?: boolean;
}

const ROLE_META: Record<
  BranchStaffRole,
  { title: string; subtitle: string; userTypeName: string; userTypeDescription: string; icon: string; accent: string }
> = {
  manager: {
    title: 'Branch manager',
    subtitle: 'Oversees stock receipts, warehouse operations, and branch staff.',
    userTypeName: 'Branch Manager',
    userTypeDescription: 'Depot or branch manager — oversees stock receipts and warehouse operations',
    icon: 'supervisor_account',
    accent: 'violet',
  },
  clerk: {
    title: 'Branch clerk',
    subtitle: 'Receives stock, confirms deliveries, and supports day-to-day branch operations.',
    userTypeName: 'Branch Clerk',
    userTypeDescription: 'Depot or branch clerk — receives stock and confirms deliveries',
    icon: 'badge',
    accent: 'blue',
  },
};

@Component({
  selector: 'app-branch-staff-dialog',
  templateUrl: './branch-staff-dialog.component.html',
  styleUrl: './branch-staff-dialog.component.scss',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatSnackBarModule],
})
export class BranchStaffDialogComponent implements OnInit {
  readonly maxDob = maximumDateOfBirthInput();
  readonly meta: (typeof ROLE_META)[BranchStaffRole];
  readonly editMode: boolean;
  /** True when the branch picker should show (create from a workforce page). */
  readonly showBranchPicker: boolean;
  readonly branches: BranchDetail[];
  submitting = false;
  prefilling = false;
  saveError = '';
  form: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly usersPortal: UsersPortalService,
    private readonly snackBar: MatSnackBar,
    private readonly dialogRef: MatDialogRef<BranchStaffDialogComponent, BranchStaffDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: BranchStaffDialogData,
  ) {
    this.meta = ROLE_META[data.role];
    this.editMode = !!data.user;
    this.branches = data.branches ?? (data.branch ? [data.branch] : []);
    // Show the picker whenever the branch is not fixed and options exist — on create and on edit
    // (editing lets an admin move a manager/clerk to a different branch).
    this.showBranchPicker = !data.branch && this.branches.length > 0;

    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      // Username is admin-editable only in edit mode; on create it is auto-generated server-side.
      username: ['', this.editMode ? [Validators.required, Validators.maxLength(150)] : [Validators.maxLength(150)]],
      phoneNumber: ['', [Validators.required, Validators.maxLength(50)]],
      gender: ['MALE', Validators.required],
      dateOfBirth: ['1990-01-01', Validators.required],
      nationalIdNumber: ['', [Validators.required, Validators.maxLength(100)]],
      branchId: [
        data.branch?.id ?? data.user?.branchId ?? '',
        this.showBranchPicker ? [Validators.required] : [],
      ],
    });
  }

  ngOnInit(): void {
    if (this.editMode && this.data.user) {
      this.prefillFromUser(this.data.user);
    }
  }

  get dialogTitle(): string {
    return this.editMode ? `Edit ${this.meta.title.toLowerCase()}` : `Add ${this.meta.title.toLowerCase()}`;
  }

  get branchLabel(): string {
    if (this.data.branch) {
      return this.data.branch.branchName;
    }
    const selected = this.branches.find((b) => b.id === Number(this.form.get('branchId')?.value));
    return selected?.branchName ?? 'your organisation';
  }

  hasError(control: string, error: string): boolean {
    const c = this.form.get(control);
    return !!c && c.touched && c.hasError(error);
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close({ created: false });
    }
  }

  save(): void {
    this.saveError = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.saveError = 'Complete all required fields.';
      return;
    }
    const raw = this.form.getRawValue();

    if (this.editMode && this.data.user) {
      this.submitting = true;
      this.usersPortal
        .updateUser({
          id: this.data.user.id,
          username: (raw.username ?? '').trim(),
          email: (raw.email ?? '').trim(),
          firstName: (raw.firstName ?? '').trim(),
          lastName: (raw.lastName ?? '').trim(),
          gender: raw.gender ?? 'MALE',
          phoneNumber: (raw.phoneNumber ?? '').trim(),
          dateOfBirth: raw.dateOfBirth ?? '',
          nationalIdNumber: (raw.nationalIdNumber ?? '').trim(),
          branchId: this.showBranchPicker ? Number(raw.branchId) || undefined : undefined,
        })
        .pipe(finalize(() => (this.submitting = false)))
        .subscribe({
          next: () => {
            this.snackBar.open(`${this.meta.title} updated.`, 'Close', {
              duration: 4000,
              panelClass: ['app-snackbar-success'],
            });
            this.dialogRef.close({ created: false, updated: true });
          },
          error: (err: Error) => {
            this.saveError = err.message ?? 'Could not update user.';
          },
        });
      return;
    }

    const branchId = this.data.branch?.id ?? Number(raw.branchId);
    if (!branchId) {
      this.saveError = 'Select a branch for this user.';
      return;
    }

    const email = (raw.email ?? '').trim();

    this.submitting = true;
    this.usersPortal
      .createUser({
        organizationId: this.data.organizationId,
        branchId,
        email,
        firstName: (raw.firstName ?? '').trim(),
        lastName: (raw.lastName ?? '').trim(),
        gender: raw.gender ?? 'MALE',
        dateOfBirth: raw.dateOfBirth ?? '',
        phoneNumber: (raw.phoneNumber ?? '').trim(),
        nationalIdNumber: (raw.nationalIdNumber ?? '').trim(),
        userTypeName: this.meta.userTypeName,
        userTypeDescription: this.meta.userTypeDescription,
        preferredLanguage: 'en',
        timezone: 'Africa/Harare',
        issueTemporaryCredentials: true,
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.snackBar.open(
            `${this.meta.title} created — temporary sign-in credentials emailed to ${email}.`,
            'Close',
            {
              duration: 5000,
              panelClass: ['app-snackbar-success'],
            },
          );
          this.dialogRef.close({ created: true });
        },
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not create branch user.';
        },
      });
  }

  private prefillFromUser(user: UserListRow): void {
    // Seed from the row, then refine from the full profile bundle (ISO DOB, exact names).
    const [first, ...rest] = (user.name ?? '').trim().split(/\s+/);
    this.form.patchValue({
      firstName: first ?? '',
      lastName: rest.join(' '),
      email: user.email ?? '',
      username: user.username ?? '',
      phoneNumber: user.phoneNumber ?? '',
      gender: user.gender || 'MALE',
      nationalIdNumber: user.nationalIdNumber ?? '',
    });

    this.prefilling = true;
    this.usersPortal
      .getUserProfileBundle(user.id)
      .pipe(finalize(() => (this.prefilling = false)))
      .subscribe({
        next: (bundle) => {
          const u = bundle.user ?? {};
          const dob = String(u['dateOfBirth'] ?? '').slice(0, 10);
          this.form.patchValue({
            firstName: String(u['firstName'] ?? this.form.get('firstName')?.value ?? ''),
            lastName: String(u['lastName'] ?? this.form.get('lastName')?.value ?? ''),
            gender: String(u['gender'] ?? this.form.get('gender')?.value ?? 'MALE'),
            nationalIdNumber: String(u['nationalIdNumber'] ?? this.form.get('nationalIdNumber')?.value ?? ''),
            dateOfBirth: dob || this.form.get('dateOfBirth')?.value,
          });
        },
        error: () => {
          /* keep row-based prefill */
        },
      });
  }
}
