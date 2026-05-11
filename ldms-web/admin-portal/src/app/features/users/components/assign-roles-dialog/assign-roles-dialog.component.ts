import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UsersAdminService } from '../../services/users-admin.service';

export interface AssignRolesDialogData {
  userGroupId: number;
  /** Shown in the dialog title for context */
  groupLabel?: string;
}

@Component({
  selector: 'app-assign-roles-dialog',
  templateUrl: './assign-roles-dialog.component.html',
  styleUrl: './assign-roles-dialog.component.scss',
  standalone: false,
})
export class AssignRolesDialogComponent implements OnInit {
  loading = false;
  assigning = false;
  roles: { id: number; role: string; description: string; selected: boolean }[] = [];
  error = '';

  readonly title: string;

  constructor(
    private readonly dialogRef: MatDialogRef<AssignRolesDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AssignRolesDialogData,
    private readonly usersService: UsersAdminService,
    private readonly snackBar: MatSnackBar,
  ) {
    this.title = data.groupLabel?.trim() ? `Assign roles — ${data.groupLabel.trim()}` : `Assign roles — group #${data.userGroupId}`;
  }

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    this.loading = true;
    this.error = '';
    this.usersService
      .queryUserRoles({
        page: 0,
        size: 500,
        searchQuery: '',
        columnFilters: { role: '', description: '' },
      })
      .subscribe({
        next: ({ rows }) => {
          this.roles = rows
            .map((r) => ({
              id: Number(r['id'] ?? 0),
              role: String(r['role'] ?? ''),
              description: String(r['description'] ?? '—'),
              selected: false,
            }))
            .filter((r) => Number.isFinite(r.id) && r.id > 0);
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error = 'Failed to load roles from the server.';
        },
      });
  }

  setAllSelected(selected: boolean): void {
    this.roles = this.roles.map((r) => ({ ...r, selected }));
  }

  assign(): void {
    const userRoleIds = this.roles.filter((r) => r.selected).map((r) => r.id);
    if (userRoleIds.length === 0) {
      this.error = 'Select at least one role to assign.';
      return;
    }
    this.error = '';
    this.assigning = true;
    this.usersService.assignUserRolesToUserGroup(this.data.userGroupId, userRoleIds).subscribe({
      next: () => {
        this.assigning = false;
        this.snackBar.open('Roles assigned to the group.', 'Close', {
          duration: 5000,
          panelClass: ['app-snackbar-success'],
        });
        this.dialogRef.close(true);
      },
      error: () => {
        this.assigning = false;
        this.error = 'Could not assign roles. Check the selection and try again.';
      },
    });
  }

  close(): void {
    this.dialogRef.close(false);
  }
}
