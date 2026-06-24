import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, finalize, forkJoin, takeUntil } from 'rxjs';
import {
  LdmsRoleModuleGroup,
  groupRolesByModule,
  moduleSectionFromApi,
} from '@shared/utils/ldms-role-module.util';
import { UsersAdminService } from '../../../users/services/users-admin.service';

interface GroupCard {
  id: number;
  name: string;
  description: string;
  users: number;
  roles: number;
  systemGroup: boolean;
  organizationId: number | null;
  organizationClassification: string | null;
}

interface RoleToggleRow {
  id: number;
  role: string;
  description: string;
  moduleKey: string;
  moduleLabel: string;
  assigned: boolean;
  busy: boolean;
  locked: boolean;
  /** Organization classifications this role applies to. Empty/null = platform-only. */
  organizationClassifications: string[] | null;
  /** True when this role may be assigned to an organisation classification. */
  portalRole: boolean;
}

/** A role row within a single classification editor — "member" = role belongs to the classification. */
interface ClassificationRoleRow {
  id: number;
  role: string;
  description: string;
  moduleKey: string;
  moduleLabel: string;
  member: boolean;
  busy: boolean;
}

interface ClassificationView {
  key: string;
  label: string;
  roleRows: ClassificationRoleRow[];
  moduleGroups: LdmsRoleModuleGroup<ClassificationRoleRow>[];
  memberCount: number;
}

type RightPanelMode = 'none' | 'regular-group' | 'admin-classifications';

const ALL_CLASSIFICATIONS: string[] = [
  'SUPPLIER',
  'CUSTOMER',
  'TRANSPORT_COMPANY',
  'CLEARING_AGENT',
  'SERVICE_STATION',
  'ROADSIDE_SUPPORT_SERVICE',
  'GOVERNMENT_AGENCY',
];

function classificationLabel(key: string): string {
  const labels: Record<string, string> = {
    SUPPLIER: 'Supplier',
    CUSTOMER: 'Customer',
    TRANSPORT_COMPANY: 'Transport Company',
    CLEARING_AGENT: 'Clearing Agent',
    SERVICE_STATION: 'Service Station',
    ROADSIDE_SUPPORT_SERVICE: 'Roadside Support',
    GOVERNMENT_AGENCY: 'Government Agency',
    ADMIN_PORTAL: 'Admin Portal',
  };
  return labels[key] ?? key;
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
  /** Classification drill-down views (populated when Administrator group is selected). */
  classificationViews: ClassificationView[] = [];
  /** Which classification panel is expanded in the drill-down view. */
  expandedClassification: string | null = null;
  groupSearch = '';
  roleSearch = '';
  error = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly usersService: UsersAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

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

  get isAdminGroupSelected(): boolean {
    const g = this.selectedGroup;
    return g != null && g.systemGroup && g.name.toLowerCase() === 'administrator';
  }

  get rightPanelMode(): RightPanelMode {
    if (!this.selectedGroupId) {
      return 'none';
    }
    if (this.isAdminGroupSelected) {
      return 'admin-classifications';
    }
    return 'regular-group';
  }

  get assignedCount(): number {
    return this.roleRows.filter((r) => r.assigned).length;
  }

  toggleClassification(key: string): void {
    this.expandedClassification = this.expandedClassification === key ? null : key;
    this.cdr.markForCheck();
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
    this.roleRows = [];
    this.roleModuleGroups = [];
    this.loadRolesForGroup(group.id);
  }

  toggleRole(row: RoleToggleRow): void {
    if (!this.selectedGroupId || row.busy || this.savingRoleId !== null || row.locked) {
      return;
    }
    const targetGroupId = this.selectedGroupId;
    this.savingRoleId = row.id;
    row.busy = true;
    this.cdr.markForCheck();

    const req$ = row.assigned
      ? this.usersService.removeUserRolesFromUserGroup(targetGroupId, [row.id])
      : this.usersService.assignUserRolesToUserGroup(targetGroupId, [row.id]);

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
        this.syncGroupRoleCount(targetGroupId);
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
    this.usersService
      .queryUserGroups({ page: 0, size: 500, searchQuery: '', columnFilters: { name: '', description: '' } })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loadingGroups = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: ({ rows }) => {
          const allGroups = rows.map((dto) => this.mapGroup(dto));

          const adminGroups = allGroups.filter(
            (g) => g.name.toLowerCase() === 'administrator',
          );
          const regularGroups = allGroups.filter(
            (g) => g.name.toLowerCase() !== 'administrator',
          );

          const merged: GroupCard[] = [...regularGroups];

          if (adminGroups.length > 0) {
            const platformAdmin = adminGroups.find((g) => g.organizationId == null || g.organizationId === 0);
            const totalUsers = adminGroups.reduce((sum, g) => sum + g.users, 0);
            const totalRoles = adminGroups.reduce((sum, g) => sum + g.roles, 0);
            merged.unshift({
              id: platformAdmin?.id ?? adminGroups[0].id,
              name: 'Administrator',
              description: 'Default system group — platform and all organisation workspaces',
              users: totalUsers,
              roles: totalRoles,
              systemGroup: true,
              organizationId: null,
              organizationClassification: null,
            });
          }

          this.groups = merged.sort((a, b) => {
            if (a.systemGroup && a.name.toLowerCase() === 'administrator') return -1;
            if (b.systemGroup && b.name.toLowerCase() === 'administrator') return 1;
            return a.name.localeCompare(b.name);
          });
          this.filteredGroups = [...this.groups];
          if (this.groups.length && !this.selectedGroupId) {
            this.selectGroup(this.groups[0]);
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
    this.classificationViews = [];
    this.expandedClassification = null;
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
        next: ({ catalog, group }) => {
          const assignedIds = this.extractAssignedRoleIds(group);
          const isSystemAdmin = Boolean(
            group?.['systemGroup'] ?? group?.['system_group'] ?? false,
          );
          const lockedIds = isSystemAdmin
            ? this.extractDefaultRoleIds(group)
            : new Set<number>();

          this.roleRows = catalog.rows
            .map((r) => {
              const id = Number(r['id'] ?? 0);
              const role = String(r['role'] ?? '');
              const module = moduleSectionFromApi(
                role,
                String(r['moduleKey'] ?? r['module_key'] ?? ''),
                String(r['moduleLabel'] ?? r['module_label'] ?? ''),
              );
              const classifications = this.extractRoleClassifications(r);
              return {
                id,
                role,
                description: String(r['description'] ?? '—'),
                moduleKey: module.key,
                moduleLabel: module.label,
                assigned: assignedIds.has(id),
                busy: false,
                locked: lockedIds.has(id),
                organizationClassifications: classifications,
                portalRole: this.extractPortalRoleFlag(r),
              };
            })
            .filter((r) => Number.isFinite(r.id) && r.id > 0)
            .sort((a, b) => {
              const moduleCmp = a.moduleLabel.localeCompare(b.moduleLabel);
              return moduleCmp !== 0 ? moduleCmp : a.role.localeCompare(b.role);
            });

          if (this.isAdminGroupSelected) {
            this.buildClassificationViews();
          }
          this.rebuildRoleView();
        },
        error: () => {
          this.snackBar.open('Could not load roles for this group.', 'Close', { duration: 5000 });
        },
      });
  }

  private buildClassificationViews(): void {
    // Only organisation-portal-eligible roles can belong to a classification.
    const eligible = this.roleRows.filter((r) => r.portalRole);
    const views: ClassificationView[] = [];
    for (const classification of ALL_CLASSIFICATIONS) {
      const rows: ClassificationRoleRow[] = eligible.map((r) => ({
        id: r.id,
        role: r.role,
        description: r.description,
        moduleKey: r.moduleKey,
        moduleLabel: r.moduleLabel,
        member: r.organizationClassifications?.includes(classification) ?? false,
        busy: false,
      }));
      const moduleGroups = groupRolesByModule(rows, (row) => row.moduleKey as never);
      views.push({
        key: classification,
        label: classificationLabel(classification),
        roleRows: rows,
        moduleGroups,
        memberCount: rows.filter((row) => row.member).length,
      });
    }
    this.classificationViews = views;
  }

  classificationMemberCount(group: LdmsRoleModuleGroup<ClassificationRoleRow>): number {
    return group.roles.filter((r) => r.member).length;
  }

  trackClassificationRole(_index: number, row: ClassificationRoleRow): number {
    return row.id;
  }

  /** Adds/removes a role for an organisation classification (editable source of truth). */
  toggleClassificationRole(view: ClassificationView, row: ClassificationRoleRow): void {
    if (row.busy || this.savingRoleId !== null) {
      return;
    }
    this.savingRoleId = row.id;
    row.busy = true;
    this.cdr.markForCheck();

    const req$ = row.member
      ? this.usersService.removeRolesFromClassification(view.key, [row.id])
      : this.usersService.assignRolesToClassification(view.key, [row.id]);

    req$
      .pipe(
        finalize(() => {
          row.busy = false;
          this.savingRoleId = null;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (resp) => {
          if (this.usersService.isUserMutationFailure(resp)) {
            this.snackBar.open(
              this.usersService.formatUserMutationMessage(resp, 'Could not update classification roles.'),
              'Close',
              { duration: 6000 },
            );
            return;
          }
          row.member = !row.member;
          view.memberCount = view.roleRows.filter((r) => r.member).length;
          this.syncCatalogClassification(view.key, row.id, row.member);
          this.snackBar.open(
            row.member
              ? `${row.role} added to ${view.label}.`
              : `${row.role} removed from ${view.label}.`,
            'Close',
            { duration: 3500, panelClass: ['app-snackbar-success'] },
          );
        },
        error: (err: { error?: unknown }) => {
          this.snackBar.open(
            this.usersService.formatUserMutationMessage(err.error, 'Could not update classification roles.'),
            'Close',
            { duration: 6000 },
          );
        },
      });
  }

  /** Keeps the underlying catalog row's classification set in sync so view rebuilds stay accurate. */
  private syncCatalogClassification(classification: string, roleId: number, member: boolean): void {
    const catalogRow = this.roleRows.find((r) => r.id === roleId);
    if (!catalogRow) {
      return;
    }
    const set = new Set(catalogRow.organizationClassifications ?? []);
    if (member) {
      set.add(classification);
    } else {
      set.delete(classification);
    }
    catalogRow.organizationClassifications = Array.from(set);
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
    const orgId = src['organizationId'] ?? src['organization_id'];
    return {
      id: Number(src['id'] ?? dto['id'] ?? 0),
      name: String(src['name'] ?? '—'),
      description: String(src['description'] ?? '—'),
      users: this.resolveMemberCount(
        src['userMemberCount'] ?? src['user_member_count'],
        Array.isArray(usersRaw) ? usersRaw.length : null,
      ),
      roles: this.resolveMemberCount(
        src['userRoleMemberCount'] ?? src['user_role_member_count'],
        Array.isArray(rolesRaw) ? rolesRaw.length : null,
      ),
      systemGroup: Boolean(src['systemGroup'] ?? src['system_group'] ?? false),
      organizationId: orgId == null ? null : Number(orgId),
      organizationClassification: (src['organizationClassification'] ?? src['organization_classification']) as string | null,
    };
  }

  private resolveUserGroupPayload(dto: Record<string, unknown>): Record<string, unknown> {
    const nested = dto['userGroupDto'];
    if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
      return nested as Record<string, unknown>;
    }
    return dto;
  }

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
      (group['userRoleDtoSet'] ??
        group['user_role_dto_set'] ??
        group['userRoleDtoList'] ??
        group['user_role_dto_list']) as unknown;
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

  private extractRoleClassifications(roleDto: Record<string, unknown>): string[] | null {
    const raw = roleDto['organizationClassifications'] ?? roleDto['organization_classifications'];
    if (Array.isArray(raw)) {
      return raw.filter((v): v is string => typeof v === 'string' && v.length > 0);
    }
    return null;
  }

  private extractPortalRoleFlag(roleDto: Record<string, unknown>): boolean {
    const raw = roleDto['organizationPortalRole'] ?? roleDto['organization_portal_role'];
    return raw === true || raw === 'true';
  }
}
