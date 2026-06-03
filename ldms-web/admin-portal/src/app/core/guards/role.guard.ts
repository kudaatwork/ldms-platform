import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { StorageService } from '../services/storage.service';
import { canAccessPath } from '../utils/nav-access.util';

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

  canActivate(): boolean | UrlTree {
    const path = this.router.url.split('?')[0].split('#')[0];
    const roles = this.storage.getRoles();

    if (canAccessPath(path, roles)) {
      return true;
    }

    if (canAccessPath('/dashboard', roles)) {
      return this.router.createUrlTree(['/dashboard']);
    }
    return this.router.createUrlTree(['/account']);
  }
}
