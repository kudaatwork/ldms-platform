import { Injectable } from '@angular/core';
import { canAccessPath, normalizeRoleCodes } from '../utils/nav-access.util';
import { StorageService } from './storage.service';

/** Minimal nav shape used for RBAC filtering (matches app shell nav). */
export interface NavAccessItem {
  label: string;
  icon: string;
  route: string;
  badge?: number | null;
  children?: NavAccessGroupEntry[];
}

export type NavAccessGroupEntry =
  | NavAccessChild
  | { type: 'heading'; label: string };

export interface NavAccessChild {
  label: string;
  icon: string;
  route: string;
  children?: NavAccessSubEntry[];
}

export type NavAccessSubEntry =
  | { type?: 'link'; label: string; icon: string; route: string }
  | { type: 'heading'; label: string };

@Injectable({ providedIn: 'root' })
export class NavAccessService {
  constructor(private readonly storage: StorageService) {}

  currentRoles(): string[] {
    return normalizeRoleCodes(this.storage.getRoles());
  }

  canAccessRoute(route: string): boolean {
    const user = this.storage.getUser();
    return canAccessPath(route, this.currentRoles(), {
      organizationKycApprover: user?.organizationKycApprover,
      operationalIssueHandler: user?.operationalIssueHandler,
    });
  }

  /** Filters top-level and nested menu entries by user-group roles. */
  filterNavItems<T extends NavAccessItem>(items: T[]): T[] {
    const roles = this.currentRoles();
    return items
      .map((item) => this.filterNavItem(item, roles))
      .filter((item): item is T => item != null);
  }

  private filterNavItem<T extends NavAccessItem>(item: T, roles: string[]): T | null {
    if (!item.children?.length) {
      return this.canAccessRoute(item.route) ? item : null;
    }

    const children = item.children
      .map((entry) => this.filterGroupEntry(entry, roles))
      .filter((entry): entry is NavAccessGroupEntry => entry != null);

    if (children.length === 0) {
      return null;
    }

    const selfOk = this.canAccessRoute(item.route);
    const childLinkOk = children.some((c) => this.groupEntryHasLink(c));
    if (!selfOk && !childLinkOk) {
      return null;
    }

    return { ...item, children } as T;
  }

  private filterGroupEntry(entry: NavAccessGroupEntry, roles: string[]): NavAccessGroupEntry | null {
    if ('type' in entry && entry.type === 'heading') {
      return entry;
    }
    const child = entry as NavAccessChild;
    if (child.children?.length) {
      const sub = child.children
        .map((subEntry) => {
          if (subEntry.type === 'heading') {
            return subEntry;
          }
          return this.canAccessRoute(subEntry.route) ? subEntry : null;
        })
        .filter((e): e is NavAccessSubEntry => e != null);
      const links = sub.filter((e) => e.type !== 'heading');
      if (links.length === 0 && !this.canAccessRoute(child.route)) {
        return null;
      }
      return { ...child, children: sub };
    }
    return this.canAccessRoute(child.route) ? child : null;
  }

  private groupEntryHasLink(entry: NavAccessGroupEntry): boolean {
    if ('type' in entry && entry.type === 'heading') {
      return false;
    }
    const child = entry as NavAccessChild;
    if (this.canAccessRoute(child.route)) {
      return true;
    }
    return (child.children ?? []).some(
      (sub) => sub.type !== 'heading' && this.canAccessRoute(sub.route),
    );
  }
}
