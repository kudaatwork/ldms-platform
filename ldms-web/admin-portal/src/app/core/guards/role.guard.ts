import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { StorageService } from '../services/storage.service';

/** Expects `route.data['roles']` as `string[]` — user must have at least one. */
@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly router: Router,
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    const required = route.data['roles'] as string[] | undefined;
    if (!required?.length) {
      return true;
    }
    const userRoles = this.storage.getRoles();
    const allowed = required.some((r) => userRoles.includes(r));
    if (allowed) {
      return true;
    }
    return this.router.createUrlTree(['/dashboard']);
  }
}
