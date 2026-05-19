import { HttpErrorResponse } from '@angular/common/http';
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { UsersAdminService } from '../../services/users-admin.service';

export interface AssignRolesDialogData {
  userGroupId: number;
  /** Shown in the dialog title for context */
  groupLabel?: string;
}

export interface AssignRoleRow {
  id: number;
  role: string;
  description: string;
  selected: boolean;
  /** Role is already linked to this user group (from server). */
  alreadyAssigned: boolean;
}

export type AssignRolesFilter = 'all' | 'on-group' | 'available';

@Component({
  selector: 'app-assign-roles-dialog',
  templateUrl: './assign-roles-dialog.component.html',
  styleUrl: './assign-roles-dialog.component.scss',
  standalone: false,
})
export class AssignRolesDialogComponent implements OnInit {
  private static readonly CATALOG_PAGE_SIZE = 1000;
  /** Row height for CDK virtual scroll (px). */
  readonly virtualRowHeight = 52;

  loading = false;
  assigning = false;
  removing = false;
  roles: AssignRoleRow[] = [];
  error = '';
  searchQuery = '';
  activeFilter: AssignRolesFilter = 'all';

  readonly title: string;

  get filteredRoles(): AssignRoleRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    return this.roles.filter((r) => {
      if (this.activeFilter === 'on-group' && !r.alreadyAssigned) {
        return false;
      }
      if (this.activeFilter === 'available' && r.alreadyAssigned) {
        return false;
      }
      if (!q) {
        return true;
      }
      return r.role.toLowerCase().includes(q) || r.description.toLowerCase().includes(q);
    });
  }

  get onGroupCount(): number {
    return this.roles.filter((r) => r.alreadyAssigned).length;
  }

  get availableCount(): number {
    return this.roles.filter((r) => !r.alreadyAssigned).length;
  }

  get assignSelectedCount(): number {
    return this.roles.filter((r) => r.selected && !r.alreadyAssigned).length;
  }

  get removeSelectedCount(): number {
    return this.roles.filter((r) => r.selected && r.alreadyAssigned).length;
  }

  get allCatalogRolesAlreadyOnGroup(): boolean {
    return this.roles.length > 0 && this.roles.every((r) => r.alreadyAssigned);
  }

  get canAssignSelected(): boolean {
    return this.assignSelectedCount > 0;
  }

  get canRemoveSelected(): boolean {
    return this.removeSelectedCount > 0;
  }

  get allFilteredSelected(): boolean {
    const visible = this.filteredRoles;
    return visible.length > 0 && visible.every((r) => r.selected);
  }

  constructor(
    private readonly dialogRef: MatDialogRef<AssignRolesDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AssignRolesDialogData,
    private readonly usersService: UsersAdminService,
    private readonly snackBar: MatSnackBar,
  ) {
    this.title = data.groupLabel?.trim()
      ? `Manage group roles — ${data.groupLabel.trim()}`
      : `Manage group roles — group #${data.userGroupId}`;
  }

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      catalog: this.usersService.queryUserRoles({
        page: 0,
        size: AssignRolesDialogComponent.CATALOG_PAGE_SIZE,
        searchQuery: '',
        columnFilters: { role: '', description: '' },
      }),
      group: this.usersService.getUserGroupById(this.data.userGroupId),
    }).subscribe({
      next: ({ catalog, group }) => {
        const assignedIds = this.extractAssignedRoleIds(group);
        const mapped = catalog.rows
          .map((r) => {
            const id = Number(r['id'] ?? 0);
            return {
              id,
              role: String(r['role'] ?? ''),
              description: String(r['description'] ?? '—'),
              selected: false,
              alreadyAssigned: assignedIds.has(id),
            };
          })
          .filter((r) => Number.isFinite(r.id) && r.id > 0);
        mapped.sort((a, b) => {
          if (a.alreadyAssigned !== b.alreadyAssigned) {
            return a.alreadyAssigned ? -1 : 1;
          }
          return a.role.localeCompare(b.role, undefined, { sensitivity: 'base' });
        });
        this.roles = mapped;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Failed to load roles from the server.';
      },
    });
  }

  setFilter(filter: AssignRolesFilter): void {
    this.activeFilter = filter;
  }

  toggleFilteredSelection(): void {
    const select = !this.allFilteredSelected;
    const visibleIds = new Set(this.filteredRoles.map((r) => r.id));
    this.roles = this.roles.map((r) => (visibleIds.has(r.id) ? { ...r, selected: select } : r));
  }

  clearSelection(): void {
    this.roles = this.roles.map((r) => ({ ...r, selected: false }));
  }

  toggleRow(row: AssignRoleRow, event: Event): void {
    if (this.assigning || this.removing) {
      return;
    }
    const target = event.target as HTMLElement;
    if (target.closest('mat-checkbox, .mdc-checkbox, .mdc-form-field')) {
      return;
    }
    const idx = this.roles.findIndex((r) => r.id === row.id);
    if (idx < 0) {
      return;
    }
    const next = [...this.roles];
    next[idx] = { ...next[idx], selected: !next[idx].selected };
    this.roles = next;
  }

  trackRole(_index: number, row: AssignRoleRow): number {
    return row.id;
  }

  roleBreakPoints(role: string): string {
    return role.replace(/_/g, '_\u200b');
  }

  rowTitle(row: AssignRoleRow): string {
    if (row.description && row.description !== '—') {
      return `${row.role}\n${row.description}`;
    }
    return row.role;
  }

  assign(): void {
    const userRoleIds = this.roles.filter((r) => r.selected && !r.alreadyAssigned).map((r) => r.id);
    if (userRoleIds.length === 0) {
      this.error = 'Select at least one role that is not already on this group.';
      return;
    }
    this.error = '';
    this.assigning = true;
    this.usersService.assignUserRolesToUserGroup(this.data.userGroupId, userRoleIds).subscribe({
      next: (resp) => {
        this.assigning = false;
        if (this.usersService.isUserMutationFailure(resp)) {
          this.error = this.usersService.formatUserMutationMessage(
            resp,
            'Could not assign roles. The selected role(s) may already be on this group.',
          );
          return;
        }
        const msg = this.usersService.formatUserMutationMessage(resp, 'Roles assigned to the group.');
        this.snackBar.open(msg, 'Close', { duration: 6000, panelClass: ['app-snackbar-success'] });
        this.dialogRef.close(true);
      },
      error: (err: HttpErrorResponse) => {
        this.assigning = false;
        this.error = this.usersService.formatUserMutationMessage(
          err.error,
          'Could not assign roles. The selected role(s) may already be on this group.',
        );
      },
    });
  }

  remove(): void {
    const userRoleIds = this.roles.filter((r) => r.selected && r.alreadyAssigned).map((r) => r.id);
    if (userRoleIds.length === 0) {
      this.error = 'Select at least one role that is already on this group to remove it.';
      return;
    }
    this.error = '';
    this.removing = true;
    this.usersService.removeUserRolesFromUserGroup(this.data.userGroupId, userRoleIds).subscribe({
      next: (resp) => {
        this.removing = false;
        if (this.usersService.isUserMutationFailure(resp)) {
          this.error = this.usersService.formatUserMutationMessage(
            resp,
            'Could not remove roles. Check the selection and try again.',
          );
          return;
        }
        const msg = this.usersService.formatUserMutationMessage(
          resp,
          'Roles removed from this group. Other groups are unchanged.',
        );
        this.snackBar.open(msg, 'Close', { duration: 6000, panelClass: ['app-snackbar-success'] });
        this.dialogRef.close(true);
      },
      error: (err: HttpErrorResponse) => {
        this.removing = false;
        this.error = this.usersService.formatUserMutationMessage(
          err.error,
          'Could not remove roles. Check the selection and try again.',
        );
      },
    });
  }

  close(): void {
    this.dialogRef.close(false);
  }

  private extractAssignedRoleIds(group: Record<string, unknown> | null): Set<number> {
    if (!group) {
      return new Set();
    }
    const raw =
      (group['userRoleDtoSet'] ?? group['user_role_dto_set'] ?? group['userRoleDtoList'] ?? group['user_role_dto_list']) as unknown;
    if (!Array.isArray(raw)) {
      return new Set();
    }
    const ids = new Set<number>();
    for (const item of raw) {
      if (item === null || typeof item !== 'object' || Array.isArray(item)) {
        continue;
      }
      const o = item as Record<string, unknown>;
      const id = Number(o['id'] ?? 0);
      if (Number.isFinite(id) && id > 0) {
        ids.add(id);
      }
    }
    return ids;
  }
}
