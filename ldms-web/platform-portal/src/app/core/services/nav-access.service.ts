import { Injectable } from '@angular/core';
import { canAccessPath, normalizeRoleCodes } from '../utils/nav-access.util';
import { AuthStateService } from './auth-state.service';
import { StorageService } from './storage.service';
import type { NavChild, NavItem } from '../../layout/sidebar/sidebar.config';

@Injectable({ providedIn: 'root' })
export class NavAccessService {
  constructor(
    private readonly authState: AuthStateService,
    private readonly storage: StorageService,
  ) {}

  currentRoles(): string[] {
    const fromUser = this.authState.currentUser?.roles;
    if (fromUser?.length) {
      return normalizeRoleCodes(fromUser);
    }
    return this.storage.getRoles();
  }

  canAccessRoute(route: string): boolean {
    return canAccessPath(route, this.currentRoles());
  }

  filterNavItems(items: NavItem[]): NavItem[] {
    const roles = this.currentRoles();
    return items
      .map((item) => this.filterNavItem(item, roles))
      .filter((item): item is NavItem => item != null);
  }

  private filterNavItem(item: NavItem, roles: string[]): NavItem | null {
    if (!item.children?.length) {
      return this.canAccessRoute(item.route) ? item : null;
    }

    const children = item.children
      .map((child) => (this.canAccessRoute(child.route) ? child : null))
      .filter((child): child is NavChild => child != null);

    if (children.length === 0) {
      return null;
    }

    const selfOk = this.canAccessRoute(item.route);
    const childOk = children.length > 0;
    if (!selfOk && !childOk) {
      return null;
    }

    return { ...item, children };
  }
}
