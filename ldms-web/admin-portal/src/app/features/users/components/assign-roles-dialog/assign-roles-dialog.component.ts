import { HttpErrorResponse } from '@angular/common/http';
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, of, timeout } from 'rxjs';
import {
  LdmsRoleModuleGroup,
  groupRolesByModule,
  moduleSectionFromApi,
} from '@shared/utils/ldms-role-module.util';
import { UsersAdminService } from '../../services/users-admin.service';

export interface AssignRolesDialogData {
  userGroupId: number;
  /** Shown in the dialog title for context */
  groupLabel?: string;
  /** Whether the group is a system group (e.g. Administrator) with locked default roles. */
  isSystemGroup?: boolean;
}

export interface AssignRoleRow {
  id: number;
  role: string;
  description: string;
  moduleKey: string;
  moduleLabel: string;
  selected: boolean;
  /** Role is already linked to this user group (from server). */
  alreadyAssigned: boolean;
  /** Role is a locked default role on a system group and cannot be removed. */
  locked: boolean;
}

export type AssignRolesFilter = 'all' | 'on-group' | 'available';

@Component({
  selector: 'app-assign-roles-dialog',
  templateUrl: './assign-roles-dialog.component.html',
  styleUrl: './assign-roles-dialog.component.scss',
  standalone: false,
})
export class AssignRolesDialogComponent implements OnInit, OnDestroy {
  private static readonly CATALOG_PAGE_SIZE = 1000;
  private static readonly LOAD_TIMEOUT_MS = 12000;
  /** Row height for CDK virtual scroll (px). */
  readonly virtualRowHeight = 52;

  loading = false;
  assigning = false;
  removing = false;
  roles: AssignRoleRow[] = [];
  filteredRoles: AssignRoleRow[] = [];
  pagedFilteredRoles: AssignRoleRow[] = [];
  roleModuleGroups: LdmsRoleModuleGroup<AssignRoleRow>[] = [];
  totalPages = 1;
  allFilteredSelected = false;
  /** Module section keys expanded in the accordion (all expanded by default after load). */
  expandedModuleKeys = new Set<string>();
  error = '';
  searchQuery = '';
  activeFilter: AssignRolesFilter = 'all';
  pageIndex = 0;
  pageSize = 25;
  readonly pageSizeOptions = [10, 25, 50, 100];

  readonly title: string;

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

  ngOnDestroy(): void {}

  onSearchQueryChange(): void {
    // Client-side filter only; avoid re-fetching and freezing the dialog on each keypress.
    this.pageIndex = 0;
    this.recalculateViewModel();
  }

  loadRoles(): void {
    this.loading = true;
    this.error = '';
    this.usersService
      .queryUserRoles({
        page: 0,
        size: AssignRolesDialogComponent.CATALOG_PAGE_SIZE,
        searchQuery: '',
        columnFilters: { role: '', description: '' },
      })
      .pipe(timeout(AssignRolesDialogComponent.LOAD_TIMEOUT_MS))
      .subscribe({
        next: (catalog) => {
          const mapped = catalog.rows
            .map((r) => {
              const id = Number(r['id'] ?? 0);
              const role = String(r['role'] ?? '');
              const module = moduleSectionFromApi(
                role,
                String(r['moduleKey'] ?? r['module_key'] ?? ''),
                String(r['moduleLabel'] ?? r['module_label'] ?? ''),
              );
              return {
                id,
                role,
                description: String(r['description'] ?? '—'),
                moduleKey: module.key,
                moduleLabel: module.label,
                selected: false,
                alreadyAssigned: false,
                locked: false,
              };
            })
            .filter((r) => Number.isFinite(r.id) && r.id > 0);
          mapped.sort((a, b) => {
            const moduleCmp = a.moduleLabel.localeCompare(b.moduleLabel, undefined, { sensitivity: 'base' });
            if (moduleCmp !== 0) {
              return moduleCmp;
            }
            return a.role.localeCompare(b.role, undefined, { sensitivity: 'base' });
          });
          this.roles = mapped;
          this.pageIndex = 0;
          this.recalculateViewModel();
          this.loading = false;
          this.loadAssignedRoleState();
        },
        error: () => {
          this.loading = false;
          this.error = 'Failed to load role catalog from the server.';
        },
      });
  }

  private loadAssignedRoleState(): void {
    this.usersService
      .getUserGroupById(this.data.userGroupId)
      .pipe(timeout(AssignRolesDialogComponent.LOAD_TIMEOUT_MS), catchError(() => of(null)))
      .subscribe((group) => {
        const assignedIds = this.extractAssignedRoleIds(group);
        const isSystem = Boolean(group?.['systemGroup'] ?? group?.['system_group'] ?? this.data.isSystemGroup);
        const lockedIds = isSystem ? this.extractDefaultRoleIds(group) : new Set<number>();
        if (!assignedIds.size || this.roles.length === 0) {
          return;
        }
        this.roles = this.roles
          .map((r) => {
            return {
              ...r,
              alreadyAssigned: assignedIds.has(r.id),
              locked: lockedIds.has(r.id),
            };
          })
          .sort((a, b) => {
            if (a.alreadyAssigned !== b.alreadyAssigned) {
              return a.alreadyAssigned ? -1 : 1;
            }
            const moduleCmp = a.moduleLabel.localeCompare(b.moduleLabel, undefined, { sensitivity: 'base' });
            if (moduleCmp !== 0) {
              return moduleCmp;
            }
            return a.role.localeCompare(b.role, undefined, { sensitivity: 'base' });
          });
        this.recalculateViewModel();
      });
  }

  setFilter(filter: AssignRolesFilter): void {
    this.activeFilter = filter;
    this.pageIndex = 0;
    this.recalculateViewModel();
  }

  onPageSizeChange(raw: number | string): void {
    const next = Number(raw);
    if (!Number.isFinite(next) || next <= 0 || next === this.pageSize) {
      return;
    }
    this.pageSize = next;
    this.pageIndex = 0;
    this.recalculateViewModel();
  }

  previousPage(): void {
    if (this.pageIndex <= 0) {
      return;
    }
    this.pageIndex -= 1;
    this.recalculateViewModel();
  }

  nextPage(): void {
    if ((this.pageIndex + 1) * this.pageSize >= this.filteredRoles.length) {
      return;
    }
    this.pageIndex += 1;
    this.recalculateViewModel();
  }

  isModuleExpanded(moduleKey: string): boolean {
    return this.expandedModuleKeys.has(moduleKey);
  }

  toggleModuleExpanded(moduleKey: string, expanded: boolean): void {
    const next = new Set(this.expandedModuleKeys);
    if (expanded) {
      next.add(moduleKey);
    } else {
      next.delete(moduleKey);
    }
    this.expandedModuleKeys = next;
  }

  expandAllModules(): void {
    this.expandedModuleKeys = new Set(this.roleModuleGroups.map((g) => g.section.key));
  }

  collapseAllModules(): void {
    this.expandedModuleKeys = new Set();
  }

  moduleOnGroupCount(group: LdmsRoleModuleGroup<AssignRoleRow>): number {
    return group.roles.filter((r) => r.alreadyAssigned).length;
  }

  moduleSelectedAssignCount(group: LdmsRoleModuleGroup<AssignRoleRow>): number {
    return group.roles.filter((r) => r.selected && !r.alreadyAssigned).length;
  }

  moduleSelectedRemoveCount(group: LdmsRoleModuleGroup<AssignRoleRow>): number {
    return group.roles.filter((r) => r.selected && r.alreadyAssigned).length;
  }

  toggleModuleSelection(group: LdmsRoleModuleGroup<AssignRoleRow>, event: Event): void {
    event.stopPropagation();
    const ids = new Set(group.roles.map((r) => r.id));
    const allSelected = group.roles.every((r) => r.selected);
    this.roles = this.roles.map((r) => (ids.has(r.id) ? { ...r, selected: !allSelected } : r));
    this.recalculateViewModel();
  }

  toggleFilteredSelection(): void {
    const select = !this.allFilteredSelected;
    const visibleIds = new Set(this.pagedFilteredRoles.map((r) => r.id));
    this.roles = this.roles.map((r) => (visibleIds.has(r.id) ? { ...r, selected: select } : r));
    this.recalculateViewModel();
  }

  clearSelection(): void {
    this.roles = this.roles.map((r) => ({ ...r, selected: false }));
    this.recalculateViewModel();
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
    this.recalculateViewModel();
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
    const selectedToRemove = this.roles.filter((r) => r.selected && r.alreadyAssigned);
    const lockedSelected = selectedToRemove.filter((r) => r.locked);
    if (lockedSelected.length > 0) {
      this.error = `${lockedSelected.length} selected role(s) are locked default roles on the Administrator group and cannot be removed.`;
      return;
    }
    const userRoleIds = selectedToRemove.map((r) => r.id);
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

  private extractDefaultRoleIds(group: Record<string, unknown> | null): Set<number> {
    if (!group) {
      return new Set();
    }
    const raw = (group['defaultRoleIds'] ?? group['default_role_ids']) as unknown;
    if (Array.isArray(raw)) {
      return new Set(raw.filter((id): id is number => typeof id === 'number' && id > 0));
    }
    return new Set();
  }

  private recalculateViewModel(): void {
    const q = this.searchQuery.trim().toLowerCase();
    this.filteredRoles = this.roles.filter((r) => {
      if (this.activeFilter === 'on-group' && !r.alreadyAssigned) {
        return false;
      }
      if (this.activeFilter === 'available' && r.alreadyAssigned) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        r.role.toLowerCase().includes(q) ||
        r.description.toLowerCase().includes(q) ||
        r.moduleLabel.toLowerCase().includes(q)
      );
    });

    const pages = Math.ceil(this.filteredRoles.length / this.pageSize);
    this.totalPages = pages > 0 ? pages : 1;
    if (this.pageIndex > this.totalPages - 1) {
      this.pageIndex = Math.max(0, this.totalPages - 1);
    }

    const start = this.pageIndex * this.pageSize;
    this.pagedFilteredRoles = this.filteredRoles.slice(start, start + this.pageSize);
    this.allFilteredSelected =
      this.pagedFilteredRoles.length > 0 && this.pagedFilteredRoles.every((r) => r.selected);
    this.roleModuleGroups = groupRolesByModule(this.pagedFilteredRoles, (row) => row.moduleKey as never);
    this.expandedModuleKeys = new Set(this.roleModuleGroups.map((g) => g.section.key));
  }
}
