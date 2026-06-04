import { Injectable } from '@angular/core';
import { CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { StorageService } from '../services/storage.service';
import { canAccessPath, normalizeRoleCodes } from '../utils/nav-access.util';

/** Routes reachable while the user group has no permission roles yet (self-service setup). */
const BOOTSTRAP_PATHS = ['/dashboard', '/account', '/help', '/users/groups', '/users/roles'] as const;

function isBootstrapPath(path: string): boolean {
  return BOOTSTRAP_PATHS.some((p) => path === p || path.startsWith(`${p}/`));
}

/**
 * Enforces admin-portal navigation access from the signed-in user's user-group roles.
 * Optional {@code route.data['roles']} still supports explicit any-of role checks per route.
 */
@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly router: Router,
  ) {}

  canActivate(_route: unknown, state: RouterStateSnapshot): boolean | UrlTree {
    const path = state.url.split('?')[0].split('#')[0];
    const roles = this.storage.getRoles();
    const codes = normalizeRoleCodes(roles);

    if (codes.length === 0 && isBootstrapPath(path)) {
      return true;
    }

    if (canAccessPath(path, roles, {
      organizationKycApprover: this.storage.getUser()?.organizationKycApprover,
      operationalIssueHandler: this.storage.getUser()?.operationalIssueHandler,
    })) {
      return true;
    }

    if (path === '/dashboard' || path.startsWith('/dashboard/')) {
      return true;
    }

    if (canAccessPath('/dashboard', roles, {
      organizationKycApprover: this.storage.getUser()?.organizationKycApprover,
      operationalIssueHandler: this.storage.getUser()?.operationalIssueHandler,
    })) {
      return this.router.createUrlTree(['/dashboard']);
    }
    return this.router.createUrlTree(['/account']);
  }
}
