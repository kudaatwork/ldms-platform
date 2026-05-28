import { HttpErrorResponse } from '@angular/common/http';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { UsersAdminService, UserListRow } from '../../services/users-admin.service';

export interface UserGroupMembersDialogResult {
  changed: boolean;
  memberCounts: Record<number, { users: number; roles: number }>;
}

export interface UserGroupMembersDialogData {
  userGroupId: number;
  groupLabel?: string;
}

export type ManageUsersPanel = 'members' | 'add';

interface SelectableMemberRow extends UserListRow {
  selected: boolean;
}

interface SelectableCatalogRow extends UserListRow {
  selected: boolean;
}

@Component({
  selector: 'app-user-group-members-dialog',
  templateUrl: './user-group-members-dialog.component.html',
  styleUrl: './user-group-members-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ScrollingModule,
    RouterModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressBarModule,
  ],
})
export class UserGroupMembersDialogComponent implements OnInit, OnDestroy {
  private static readonly PAGE_SIZE = 500;
  private static readonly MAX_LOADED = 2000;

  readonly virtualRowHeight = 68;

  loading = false;
  loadingCatalog = false;
  mutating = false;
  error = '';
  members: SelectableMemberRow[] = [];
  catalog: SelectableCatalogRow[] = [];
  totalElements = 0;
  loadTruncated = false;
  catalogTruncated = false;
  searchQuery = '';
  activePanel: ManageUsersPanel = 'members';
  private readonly searchReload$ = new Subject<void>();
  private membersChanged = false;
  private memberCounts: Record<number, { users: number; roles: number }> = {};
  private memberIds = new Set<number>();

  readonly title: string;

  get filteredMembers(): SelectableMemberRow[] {
    return this.members;
  }

  get filteredCatalog(): SelectableCatalogRow[] {
    return this.catalog;
  }

  get selectedMemberCount(): number {
    return this.members.filter((m) => m.selected).length;
  }

  get selectedAddCount(): number {
    return this.catalog.filter((m) => m.selected).length;
  }

  get activeCount(): number {
    return this.members.filter((m) => m.status === 'active').length;
  }

  constructor(
    private readonly dialogRef: MatDialogRef<UserGroupMembersDialogComponent, UserGroupMembersDialogResult | false>,
    @Inject(MAT_DIALOG_DATA) public readonly data: UserGroupMembersDialogData,
    private readonly usersService: UsersAdminService,
    private readonly snackBar: MatSnackBar,
  ) {
    const label = data.groupLabel?.trim();
    this.title = label ? `Manage users — ${label}` : `Manage users — group #${data.userGroupId}`;
  }

  ngOnInit(): void {
    this.searchReload$.pipe(debounceTime(300)).subscribe(() => {
      if (this.activePanel === 'members') {
        this.loadMembers();
      } else {
        this.loadCatalog();
      }
    });
    this.loadMembers();
    this.loadCatalog();
  }

  ngOnDestroy(): void {
    this.searchReload$.complete();
  }

  onSearchQueryChange(): void {
    this.searchReload$.next();
  }

  setPanel(panel: ManageUsersPanel): void {
    this.activePanel = panel;
    this.searchQuery = '';
    this.error = '';
    if (panel === 'members') {
      this.loadMembers();
    } else {
      this.loadCatalog();
    }
  }

  loadMembers(): void {
    this.loading = true;
    this.error = '';
    this.loadTruncated = false;
    this.memberIds = new Set();
    this.fetchMemberPage(0, []);
  }

  loadCatalog(): void {
    this.loadingCatalog = true;
    this.catalogTruncated = false;
    this.fetchCatalogPage(0, []);
  }

  refreshAll(): void {
    this.clearMemberSelection();
    this.clearCatalogSelection();
    this.loadMembers();
    this.loadCatalog();
  }

  trackMember(_index: number, row: UserListRow): number {
    return row.id;
  }

  userInitials(row: UserListRow): string {
    const name = row.name.trim();
    const parts = name.split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return `${parts[0][0] ?? ''}${parts[1][0] ?? ''}`.toUpperCase();
    }
    const seed = name || row.username || row.email || '?';
    return seed.slice(0, 2).toUpperCase();
  }

  profileLink(row: UserListRow): string[] {
    return ['/users', String(row.id), 'profile'];
  }

  toggleMemberSelection(row: SelectableMemberRow, event: Event): void {
    if (this.mutating) {
      return;
    }
    if ((event.target as HTMLElement).closest('mat-checkbox, .mdc-checkbox, a, button')) {
      return;
    }
    row.selected = !row.selected;
  }

  toggleCatalogSelection(row: SelectableCatalogRow, event: Event): void {
    if (this.mutating) {
      return;
    }
    if ((event.target as HTMLElement).closest('mat-checkbox, .mdc-checkbox')) {
      return;
    }
    row.selected = !row.selected;
  }

  toggleVisibleMemberSelection(): void {
    const visible = this.filteredMembers;
    const allSelected = visible.length > 0 && visible.every((m) => m.selected);
    const ids = new Set(visible.map((m) => m.id));
    this.members = this.members.map((m) => (ids.has(m.id) ? { ...m, selected: !allSelected } : m));
  }

  toggleVisibleCatalogSelection(): void {
    const visible = this.filteredCatalog;
    const allSelected = visible.length > 0 && visible.every((m) => m.selected);
    const ids = new Set(visible.map((m) => m.id));
    this.catalog = this.catalog.map((m) => (ids.has(m.id) ? { ...m, selected: !allSelected } : m));
  }

  clearMemberSelection(): void {
    this.members = this.members.map((m) => ({ ...m, selected: false }));
  }

  clearCatalogSelection(): void {
    this.catalog = this.catalog.map((m) => ({ ...m, selected: false }));
  }

  removeSelected(): void {
    const userIds = this.members.filter((m) => m.selected).map((m) => m.id);
    if (userIds.length === 0) {
      this.error = 'Select at least one member to remove from this group.';
      return;
    }
    this.error = '';
    this.mutating = true;
    this.usersService.removeUsersFromUserGroup(this.data.userGroupId, userIds).subscribe({
      next: (resp) => {
        this.mutating = false;
        if (this.isMutationFailure(resp)) {
          this.error = this.formatMutationError(
            resp,
            'Could not remove the selected user(s). Check they belong to this group and try again.',
          );
          return;
        }
        this.membersChanged = true;
        this.mergeMemberCountsFromResponse(resp);
        this.snackBar.open(
          userIds.length === 1 ? 'User removed from the group.' : `${userIds.length} users removed from the group.`,
          'Close',
          { duration: 5000, panelClass: ['app-snackbar-success'] },
        );
        this.refreshAll();
      },
      error: () => {
        this.mutating = false;
        this.error = 'Could not remove the selected user(s). Check they belong to this group and try again.';
      },
    });
  }

  addSelected(): void {
    const selected = this.catalog.filter((m) => m.selected);
    if (selected.length === 0) {
      this.error = 'Select at least one user to add to this group.';
      return;
    }
    const gid = this.data.userGroupId;
    const alreadyInGroup = selected.filter(
      (m) => this.memberIds.has(m.id) || m.userGroupId === gid,
    );
    const toAdd = selected.filter((m) => !this.memberIds.has(m.id) && m.userGroupId !== gid);
    if (toAdd.length === 0) {
      this.error =
        alreadyInGroup.length === 1
          ? 'This user is already in this group.'
          : `All ${alreadyInGroup.length} selected user(s) are already in this group.`;
      return;
    }
    if (alreadyInGroup.length > 0) {
      this.snackBar.open(
        `${alreadyInGroup.length} selected user(s) are already in this group and will be skipped.`,
        'Close',
        { duration: 6000, panelClass: ['app-snackbar-success'] },
      );
    }
    const userIds = toAdd.map((m) => m.id);
    this.error = '';
    this.mutating = true;
    const movingFromOtherGroup = toAdd.some(
      (m) => m.userGroupId != null && m.userGroupId !== gid,
    );
    if (movingFromOtherGroup && !window.confirm(
      'One or more selected users belong to another group. Each user can only have one primary group — they will be moved to this group. Continue?',
    )) {
      this.mutating = false;
      return;
    }

    this.usersService.addUsersToUserGroup(this.data.userGroupId, userIds).subscribe({
      next: (resp) => {
        this.mutating = false;
        if (this.isMutationFailure(resp)) {
          this.error = this.formatMutationError(
            resp,
            'Could not add one or more users. They may already be in this group or the request failed.',
          );
          return;
        }
        this.membersChanged = true;
        this.mergeMemberCountsFromResponse(resp);
        const msg = this.usersService.formatUserMutationMessage(
          resp,
          userIds.length === 1 ? 'User added to the group.' : `${userIds.length} users added to the group.`,
        );
        this.snackBar.open(msg, 'Close', { duration: 6000, panelClass: ['app-snackbar-success'] });
        this.setPanel('members');
        this.refreshAll();
      },
      error: (err: HttpErrorResponse) => {
        this.mutating = false;
        this.error = this.usersService.formatUserMutationMessage(
          err.error,
          'Could not add one or more users. They may already be in this group or the request failed.',
        );
      },
    });
  }

  close(): void {
    if (!this.membersChanged) {
      this.dialogRef.close(false);
      return;
    }
    this.dialogRef.close({ changed: true, memberCounts: { ...this.memberCounts } });
  }

  currentGroupLabel(row: UserListRow): string {
    if (row.userGroupId == null) {
      return 'Unassigned';
    }
    if (row.userGroupId === this.data.userGroupId) {
      return 'In this group';
    }
    const name = String(row.role ?? '').trim();
    return name && name !== '—' ? name : `Group #${row.userGroupId}`;
  }

  private mergeMemberCountsFromResponse(resp: unknown): void {
    const extracted = this.usersService.extractUserGroupMemberCounts(resp);
    this.memberCounts = { ...this.memberCounts, ...extracted };
  }

  private isMutationFailure(resp: unknown): boolean {
    if (resp === null || typeof resp !== 'object') {
      return false;
    }
    const r = resp as Record<string, unknown>;
    if (r['success'] === false || r['isSuccess'] === false) {
      return true;
    }
    const statusCode = r['statusCode'];
    return typeof statusCode === 'number' && statusCode >= 400;
  }

  private formatMutationError(resp: unknown, fallback: string): string {
    if (resp !== null && typeof resp === 'object') {
      const r = resp as Record<string, unknown>;
      const messages = r['errorMessages'];
      if (Array.isArray(messages) && messages.length > 0) {
        return messages.map((m) => String(m)).join(' ');
      }
      if (typeof r['message'] === 'string' && r['message'].trim()) {
        return r['message'].trim();
      }
    }
    return fallback;
  }

  /** Drop users already in this group (by loaded member list and primary group id). */
  private reconcileCatalogWithMembers(): void {
    const gid = this.data.userGroupId;
    this.catalog = this.catalog.filter((r) => !this.memberIds.has(r.id) && r.userGroupId !== gid);
  }

  private fetchMemberPage(page: number, accumulated: UserListRow[]): void {
    this.usersService
      .queryUsers({
        page,
        size: UserGroupMembersDialogComponent.PAGE_SIZE,
        searchQuery: this.searchQuery.trim(),
        userGroupId: this.data.userGroupId,
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
        next: ({ rows, totalElements }) => {
          const merged = accumulated.concat(rows);
          this.totalElements = totalElements;
          const hasMore =
            merged.length < totalElements &&
            rows.length === UserGroupMembersDialogComponent.PAGE_SIZE &&
            merged.length < UserGroupMembersDialogComponent.MAX_LOADED;
          if (hasMore) {
            this.fetchMemberPage(page + 1, merged);
            return;
          }
          this.members = merged.map((r) => ({ ...r, selected: false }));
          this.memberIds = new Set(this.members.map((m) => m.id));
          this.reconcileCatalogWithMembers();
          this.loadTruncated = totalElements > merged.length;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error = 'Could not load users for this group.';
        },
      });
  }

  private fetchCatalogPage(page: number, accumulated: UserListRow[]): void {
    this.usersService
      .queryUsers({
        page,
        size: UserGroupMembersDialogComponent.PAGE_SIZE,
        searchQuery: this.searchQuery.trim(),
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
        next: ({ rows, totalElements }) => {
          const merged = accumulated.concat(rows);
          const hasMore =
            merged.length < totalElements &&
            rows.length === UserGroupMembersDialogComponent.PAGE_SIZE &&
            merged.length < UserGroupMembersDialogComponent.MAX_LOADED;
          if (hasMore) {
            this.fetchCatalogPage(page + 1, merged);
            return;
          }
          this.catalog = merged.map((r) => ({ ...r, selected: false }));
          this.reconcileCatalogWithMembers();
          this.catalogTruncated = totalElements > merged.length;
          this.loadingCatalog = false;
        },
        error: () => {
          this.loadingCatalog = false;
        },
      });
  }
}
