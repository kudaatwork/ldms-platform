import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UsersPortalService } from '../../services/users-portal.service';

export interface UserAssignUserGroupDialogData {
  userId: number;
  /** When set, pre-selects this group in the list */
  currentGroupId?: number | null;
}

interface UserGroupOption {
  id: number;
  name: string;
  description: string;
}

@Component({
  selector: 'app-user-assign-user-group-dialog',
  templateUrl: './user-assign-user-group-dialog.component.html',
  styleUrl: './user-assign-user-group-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
})
export class UserAssignUserGroupDialogComponent implements OnInit {
  loading = false;
  saving = false;
  groups: UserGroupOption[] = [];
  selectedGroupId: number | null = null;
  groupSearch = '';
  error = '';

  constructor(
    private readonly dialogRef: MatDialogRef<UserAssignUserGroupDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public readonly data: UserAssignUserGroupDialogData,
    private readonly usersService: UsersPortalService,
    private readonly snackBar: MatSnackBar,
  ) {}

  get filteredGroups(): UserGroupOption[] {
    const q = this.groupSearch.trim().toLowerCase();
    if (!q) {
      return this.groups;
    }
    return this.groups.filter(
      (g) => g.name.toLowerCase().includes(q) || g.description.toLowerCase().includes(q),
    );
  }

  get selectedGroupDescription(): string {
    if (!Number.isFinite(this.selectedGroupId ?? NaN) || (this.selectedGroupId as number) <= 0) {
      return '';
    }
    return this.groups.find((g) => g.id === this.selectedGroupId)?.description?.trim() ?? '';
  }

  ngOnInit(): void {
    this.loadGroups();
  }

  loadGroups(): void {
    this.loading = true;
    this.error = '';
    this.usersService
      .queryUserGroups({
        page: 0,
        size: 500,
        searchQuery: '',
        columnFilters: { name: '', description: '' },
      })
      .subscribe({
        next: ({ rows }) => {
          this.groups = rows
            .map((r) => ({
              id: Number(r['id'] ?? 0),
              name: String(r['name'] ?? '').trim(),
              description: String(r['description'] ?? '').trim(),
            }))
            .filter((g) => Number.isFinite(g.id) && g.id > 0);
          const current = this.data.currentGroupId;
          if (Number.isFinite(current) && (current as number) > 0) {
            const hit = this.groups.find((g) => g.id === current);
            this.selectedGroupId = hit ? hit.id : null;
            if (!hit) {
              this.error = 'The user’s current group is no longer available. Pick another group.';
            }
          }
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error = 'Failed to load user groups from the server.';
        },
      });
  }

  onGroupChange(): void {
    if (this.error.startsWith('Select')) {
      this.error = '';
    }
  }

  save(): void {
    const gid = this.selectedGroupId;
    if (!Number.isFinite(gid) || (gid as number) <= 0) {
      this.error = 'Select a user group.';
      return;
    }
    const current = this.data.currentGroupId;
    if (Number.isFinite(current) && (current as number) > 0 && current === gid) {
      this.error = 'This user is already assigned to the selected group.';
      return;
    }
    this.error = '';
    this.saving = true;
    this.usersService.addUserToUserGroup(this.data.userId, gid as number).subscribe({
      next: (resp) => {
        this.saving = false;
        if (this.usersService.isUserMutationFailure(resp)) {
          this.error = this.usersService.formatUserMutationMessage(
            resp,
            'Could not update the user group. The user may already belong to this group.',
          );
          return;
        }
        const msg = this.usersService.formatUserMutationMessage(resp, 'User group saved for this user.');
        this.snackBar.open(msg, 'Close', { duration: 6000, panelClass: ['app-snackbar-success'] });
        this.dialogRef.close(true);
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.error = this.usersService.formatUserMutationMessage(
          err.error,
          'Could not update the user group. The user may already belong to this group.',
        );
      },
    });
  }

  close(): void {
    this.dialogRef.close(false);
  }

  /** Dropdown label: name only (description shown in hint below). */
  groupOptionLabel(g: UserGroupOption): string {
    return g.name?.trim() || `#${g.id}`;
  }
}
