import { Injectable } from '@angular/core';
import { CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { StorageService } from '../services/storage.service';
import { isReadableAccessToken, isStoredSessionToken } from '../utils/jwt.util';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly router: Router,
  ) {}

  canActivate(_route: unknown, state: RouterStateSnapshot): boolean | UrlTree {
    const token = this.storage.getToken();
    if (isReadableAccessToken(token) || isStoredSessionToken(token)) {
      return true;
    }
    if (token) {
      this.storage.clearSession();
    }
    return this.router.createUrlTree(['/auth/login'], {
      queryParams: { returnUrl: state.url },
    });
  }
}
