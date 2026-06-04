import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UsersPortalService, UserListRow } from '../../services/users-portal.service';

export interface AddUserToGroupDialogData {
  userGroupId: number;
  groupLabel?: string;
}

@Component({
  selector: 'app-add-user-to-group-dialog',
  templateUrl: './add-user-to-group-dialog.component.html',
  styleUrl: './add-user-to-group-dialog.component.scss',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatDialogModule, MatIconModule, MatProgressBarModule],
})
export class AddUserToGroupDialogComponent implements OnInit {
  loading = false;
  saving = false;
  users: UserListRow[] = [];
  selectedUserId: number | null = null;
  error = '';

  readonly dialogTitle: string;

  constructor(
    private readonly dialogRef: MatDialogRef<AddUserToGroupDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AddUserToGroupDialogData,
    private readonly usersService: UsersPortalService,
    private readonly snackBar: MatSnackBar,
  ) {
    const label = data.groupLabel?.trim();
    this.dialogTitle = label ? `Add user to group — ${label}` : `Add user to group #${data.userGroupId}`;
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.error = '';
    this.usersService
      .queryUsers({
        page: 0,
        size: 500,
        searchQuery: '',
        columnFilters: {
          email: '',
          firstName: '',
          lastName: '',
          username: '',
          phoneNumber: '',
          nationalIdNumber: '',
          passportNumber: '',
          statusLabel: '',
        },
      })
      .subscribe({
        next: ({ rows }) => {
          this.users = rows.filter((u) => Number.isFinite(u.id) && u.id > 0);
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error = 'Failed to load users from the server.';
        },
      });
  }

  userOptionLabel(u: UserListRow): string {
    const name = u.name?.trim() || '—';
    const un = u.username?.trim();
    const em = u.email?.trim();
    const parts = [name];
    if (un) parts.push(`@${un}`);
    if (em) parts.push(em);
    return parts.join(' · ');
  }

  save(): void {
    const uid = this.selectedUserId;
    if (!Number.isFinite(uid) || (uid as number) <= 0) {
      this.error = 'Select a user.';
      return;
    }
    const selected = this.users.find((u) => u.id === uid);
    if (selected?.userGroupId === this.data.userGroupId) {
      this.error = 'This user is already in this group.';
      return;
    }
    this.error = '';
    this.saving = true;
    this.usersService.addUserToUserGroup(uid as number, this.data.userGroupId).subscribe({
      next: (resp) => {
        this.saving = false;
        if (this.usersService.isUserMutationFailure(resp)) {
          this.error = this.usersService.formatUserMutationMessage(
            resp,
            'Could not add the user to this group. They may already be a member.',
          );
          return;
        }
        const msg = this.usersService.formatUserMutationMessage(resp, 'User was added to the group.');
        this.snackBar.open(msg, 'Close', { duration: 6000, panelClass: ['app-snackbar-success'] });
        this.dialogRef.close(true);
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.error = this.usersService.formatUserMutationMessage(
          err.error,
          'Could not add the user to this group. They may already be a member.',
        );
      },
    });
  }

  close(): void {
    this.dialogRef.close(false);
  }
}
