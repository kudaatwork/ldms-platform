import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, finalize, forkJoin, takeUntil } from 'rxjs';
import { OrgContextService } from '../../../../core/services/org-context.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import {
  LdmsRoleModuleGroup,
  groupRolesByModule,
  moduleSectionFromApi,
} from '@shared/utils/ldms-role-module.util';
import { UsersPortalService } from '../../../users/services/users-portal.service';

interface GroupCard {
  id: number;
  name: string;
  description: string;
  organizationClassification: string;
  users: number;
  roles: number;
  systemGroup: boolean;
}

interface RoleToggleRow {
  id: number;
  role: string;
  description: string;
  moduleKey: string;
  moduleLabel: string;
  assigned: boolean;
  busy: boolean;
  /** True when the Administrator (default) group is selected — toggling is disabled. */
  locked: boolean;
}

@Component({
  selector: 'app-settings-group-roles',
  templateUrl: './settings-group-roles.component.html',
  styleUrl: './settings-group-roles.component.scss',
  standalone: false,
})
export class SettingsGroupRolesComponent implements OnInit, OnDestroy {
  loadingGroups = true;
  loadingRoles = false;
  savingRoleId: number | null = null;
  groups: GroupCard[] = [];
  filteredGroups: GroupCard[] = [];
  selectedGroupId: number | null = null;
  roleRows: RoleToggleRow[] = [];
  roleModuleGroups: LdmsRoleModuleGroup<RoleToggleRow>[] = [];
  groupSearch = '';
  roleSearch = '';
  error = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly usersService: UsersPortalService,
    private readonly orgContext: OrgContextService,
    private readonly authState: AuthStateService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  get orgClassificationLabel(): string {
    const raw = this.orgClassification;
    return raw ? raw.replace(/_/g, ' ') : '';
  }

  /** Raw organisation classification (uppercase) of the signed-in workspace, e.g. SUPPLIER. */
  get orgClassification(): string {
    return String(this.authState.currentUser?.orgClassification ?? '').trim().toUpperCase();
  }

  /** True when the selected group is the locked, default Administrator group. */
  get isAdminGroupSelected(): boolean {
    const g = this.selectedGroup;
    return g != null && g.systemGroup && g.name.toLowerCase() === 'administrator';
  }

  ngOnInit(): void {
    this.loadGroups();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get selectedGroup(): GroupCard | null {
    return this.groups.find((g) => g.id === this.selectedGroupId) ?? null;
  }

  get assignedCount(): number {
    return this.roleRows.filter((r) => r.assigned).length;
  }

  onGroupSearchChange(): void {
    const q = this.groupSearch.trim().toLowerCase();
    this.filteredGroups = !q
      ? [...this.groups]
      : this.groups.filter(
          (g) => g.name.toLowerCase().includes(q) || g.description.toLowerCase().includes(q),
        );
    this.cdr.markForCheck();
  }

  onRoleSearchChange(): void {
    this.rebuildRoleView();
  }

  selectGroup(group: GroupCard): void {
    if (this.selectedGroupId === group.id) {
      return;
    }
    this.selectedGroupId = group.id;
    this.loadRolesForGroup(group.id);
  }

  toggleRole(row: RoleToggleRow): void {
    if (!this.selectedGroupId || row.busy || this.savingRoleId !== null || row.locked) {
      return;
    }
    this.savingRoleId = row.id;
    row.busy = true;
    this.cdr.markForCheck();

    const req$ = row.assigned
      ? this.usersService.removeUserRolesFromUserGroup(this.selectedGroupId, [row.id])
      : this.usersService.assignUserRolesToUserGroup(this.selectedGroupId, [row.id]);

    req$.pipe(finalize(() => {
      row.busy = false;
      this.savingRoleId = null;
      this.cdr.markForCheck();
    })).subscribe({
      next: (resp) => {
        if (this.usersService.isUserMutationFailure(resp)) {
          this.snackBar.open(
            this.usersService.formatUserMutationMessage(resp, 'Could not update group roles.'),
            'Close',
            { duration: 6000 },
          );
          return;
        }
        row.assigned = !row.assigned;
        this.syncGroupRoleCount(this.selectedGroupId!);
        this.rebuildRoleView();
        this.snackBar.open(
          row.assigned ? `${row.role} added to group.` : `${row.role} removed from group.`,
          'Close',
          { duration: 3500, panelClass: ['app-snackbar-success'] },
        );
      },
      error: (err: { error?: unknown }) => {
        this.snackBar.open(
          this.usersService.formatUserMutationMessage(err.error, 'Could not update group roles.'),
          'Close',
          { duration: 6000 },
        );
      },
    });
  }

  trackGroup(_index: number, group: GroupCard): number {
    return group.id;
  }

  trackRole(_index: number, row: RoleToggleRow): number {
    return row.id;
  }

  moduleAssignedCount(group: LdmsRoleModuleGroup<RoleToggleRow>): number {
    return group.roles.filter((r) => r.assigned).length;
  }

  private loadGroups(): void {
    this.loadingGroups = true;
    this.error = '';
    const orgId = this.orgContext.organizationId;
    if (!orgId) {
      this.loadingGroups = false;
      this.error = 'Sign in with an organisation account to manage group roles.';
      this.cdr.markForCheck();
      return;
    }

    this.usersService
      .queryUserGroups({
        page: 0,
        size: 500,
        searchQuery: '',
        columnFilters: { name: '', description: '' },
        organizationId: orgId,
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loadingGroups = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: ({ rows }) => {
          const mapped = rows.map((dto) => this.mapGroup(dto));
          this.groups = mapped.sort((a, b) => {
            const adminA = a.name.toLowerCase() === 'administrator';
            const adminB = b.name.toLowerCase() === 'administrator';
            if (adminA !== adminB) {
              return adminA ? -1 : 1;
            }
            return a.name.localeCompare(b.name);
          });
          this.filteredGroups = [...this.groups];
          const administrator = this.groups.find((g) => g.name.toLowerCase() === 'administrator');
          if (administrator && !this.selectedGroupId) {
            this.selectGroup(administrator);
          } else if (this.groups.length && !this.selectedGroupId) {
            this.selectGroup(this.groups[0]);
          } else if (!this.groups.length) {
            this.error = 'No workspace groups found for your organisation yet.';
          }
        },
        error: () => {
          this.error = 'Could not load user groups.';
        },
      });
  }

  private loadRolesForGroup(groupId: number): void {
    this.loadingRoles = true;
    this.roleRows = [];
    this.roleModuleGroups = [];
    this.cdr.markForCheck();

    forkJoin({
      catalog: this.usersService.queryUserRoles({
        page: 0,
        size: 1000,
        searchQuery: '',
        columnFilters: { role: '', description: '' },
      }),
      group: this.usersService.getUserGroupById(groupId),
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loadingRoles = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (result: {
          catalog: { rows: Record<string, unknown>[] };
          group: Record<string, unknown> | null;
        }) => {
          const assignedIds = this.extractAssignedRoleIds(result.group);
          const orgClass = this.orgClassification;
          const locked = this.isAdminGroupSelected;
          const rows: RoleToggleRow[] = [];
          for (const raw of result.catalog.rows) {
            const id = Number(raw['id'] ?? 0);
            if (!Number.isFinite(id) || id <= 0) {
              continue;
            }
            const role = String(raw['role'] ?? '');
            // The locked Administrator group shows only its assigned roles. Editable custom groups show
            // the classification-applicable catalog (plus anything already assigned) so role assignments
            // stay isolated per organisation classification.
            const keep = locked
              ? assignedIds.has(id)
              : assignedIds.has(id) || this.isRoleApplicableToClassification(raw, orgClass);
            if (!keep) {
              continue;
            }
            const module = moduleSectionFromApi(
              role,
              String(raw['moduleKey'] ?? raw['module_key'] ?? ''),
              String(raw['moduleLabel'] ?? raw['module_label'] ?? ''),
            );
            rows.push({
              id,
              role,
              description: String(raw['description'] ?? '—'),
              moduleKey: module.key,
              moduleLabel: module.label,
              assigned: assignedIds.has(id),
              busy: false,
              locked,
            });
          }
          rows.sort((a, b) => {
            const moduleCmp = a.moduleLabel.localeCompare(b.moduleLabel);
            return moduleCmp !== 0 ? moduleCmp : a.role.localeCompare(b.role);
          });
          this.roleRows = rows;
          this.rebuildRoleView();
        },
        error: () => {
          this.snackBar.open('Could not load roles for this group.', 'Close', { duration: 5000 });
        },
      });
  }

  private rebuildRoleView(): void {
    const q = this.roleSearch.trim().toLowerCase();
    const filtered = !q
      ? this.roleRows
      : this.roleRows.filter(
          (r) =>
            r.role.toLowerCase().includes(q) ||
            r.description.toLowerCase().includes(q) ||
            r.moduleLabel.toLowerCase().includes(q),
        );
    this.roleModuleGroups = groupRolesByModule(filtered, (row) => row.moduleKey as never);
    this.cdr.markForCheck();
  }

  private syncGroupRoleCount(groupId: number): void {
    const count = this.roleRows.filter((r) => r.assigned).length;
    this.groups = this.groups.map((g) => (g.id === groupId ? { ...g, roles: count } : g));
    this.onGroupSearchChange();
  }

  private mapGroup(dto: Record<string, unknown>): GroupCard {
    const src = this.resolveUserGroupPayload(dto);
    const usersRaw = src['users'] ?? src['userDtoSet'] ?? src['user_dto_set'];
    const rolesRaw = src['userRoleDtoSet'] ?? src['user_role_dto_set'];
    return {
      id: Number(src['id'] ?? dto['id'] ?? 0),
      name: String(src['name'] ?? '—'),
      description: String(src['description'] ?? '—'),
      organizationClassification: String(
        src['organizationClassification'] ?? src['organization_classification'] ?? '',
      ).trim(),
      users: this.resolveMemberCount(
        src['userMemberCount'] ?? src['user_member_count'],
        Array.isArray(usersRaw) ? usersRaw.length : null,
      ),
      roles: this.resolveMemberCount(
        src['userRoleMemberCount'] ?? src['user_role_member_count'],
        Array.isArray(rolesRaw) ? rolesRaw.length : null,
      ),
      systemGroup: Boolean(src['systemGroup'] ?? src['system_group'] ?? false),
    };
  }

  /** A role belongs to a classification when its mapping includes it (platform-only roles never do). */
  private isRoleApplicableToClassification(roleDto: Record<string, unknown>, orgClassification: string): boolean {
    if (!orgClassification) {
      return false;
    }
    const raw = roleDto['organizationClassifications'] ?? roleDto['organization_classifications'];
    if (!Array.isArray(raw)) {
      return false;
    }
    return raw.some((v) => typeof v === 'string' && v.trim().toUpperCase() === orgClassification);
  }

  /** API rows are usually flat `UserGroupDto`; some gateways wrap as `userGroupDto`. */
  private resolveUserGroupPayload(dto: Record<string, unknown>): Record<string, unknown> {
    const nested = dto['userGroupDto'];
    if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
      return nested as Record<string, unknown>;
    }
    return dto;
  }

  /** List endpoints expose counts (`userRoleMemberCount`) instead of full role arrays. */
  private resolveMemberCount(rawCount: unknown, legacyListLength: number | null): number {
    if (rawCount !== null && rawCount !== undefined && rawCount !== '') {
      const parsed =
        typeof rawCount === 'number' && Number.isFinite(rawCount) ? Math.trunc(rawCount) : Number(rawCount);
      if (Number.isFinite(parsed) && parsed >= 0) {
        return parsed;
      }
    }
    return legacyListLength != null ? legacyListLength : 0;
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
      if (item && typeof item === 'object' && !Array.isArray(item)) {
        const id = Number((item as Record<string, unknown>)['id'] ?? 0);
        if (Number.isFinite(id) && id > 0) {
          ids.add(id);
        }
      }
    }
    return ids;
  }
}
