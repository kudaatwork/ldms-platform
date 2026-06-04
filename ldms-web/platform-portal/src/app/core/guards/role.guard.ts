import { Injectable } from '@angular/core';
import { CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';
import { StorageService } from '../services/storage.service';
import { canAccessPath, normalizeRoleCodes } from '../utils/nav-access.util';

const BOOTSTRAP_PATHS = ['/dashboard', '/account', '/help', '/users/groups', '/users/roles'] as const;

/** Organisation workspace modules — allow navigation when signed in; APIs enforce fine-grained roles. */
const ORG_WORKSPACE_PREFIXES = ['/users', '/activity', '/documents', '/settings'] as const;

function isBootstrapPath(path: string): boolean {
  return BOOTSTRAP_PATHS.some((p) => path === p || path.startsWith(`${p}/`));
}

function isOrgWorkspacePath(path: string): boolean {
  return ORG_WORKSPACE_PREFIXES.some((p) => path === p || path.startsWith(`${p}/`));
}

/** Enforces workspace navigation from JWT user-group roles. */
@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly router: Router,
  ) {}

  canActivate(_route: unknown, state: RouterStateSnapshot): boolean | UrlTree {
    const path = state.url.split('?')[0].split('#')[0];
    const roles = this.authState.currentUser?.roles ?? this.storage.getRoles();
    const codes = normalizeRoleCodes(roles);
    const token = this.storage.getToken();
    const signedIn = !!token && !token.startsWith('mock-token-') && !token.startsWith('mock.');

    if (signedIn && isOrgWorkspacePath(path)) {
      return true;
    }

    if (codes.length === 0 && isBootstrapPath(path)) {
      return true;
    }

    if (canAccessPath(path, roles)) {
      return true;
    }

    if (path === '/dashboard' || path.startsWith('/dashboard/')) {
      return true;
    }

    if (canAccessPath('/dashboard', roles)) {
      return this.router.createUrlTree(['/dashboard']);
    }
    return this.router.createUrlTree(['/account']);
  }
}
