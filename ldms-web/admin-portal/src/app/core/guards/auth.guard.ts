import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { StorageService } from '../services/storage.service';
import { isStoredSessionToken } from '../utils/jwt.util';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree {
    const token = this.storage.getToken();
    if (isStoredSessionToken(token)) {
      return true;
    }
    if (token) {
      this.storage.clearSession();
    }
    return this.router.createUrlTree(['/auth/login'], {
      queryParams: { returnUrl: this.router.url },
    });
  }
}
