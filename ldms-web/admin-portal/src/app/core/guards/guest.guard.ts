import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { StorageService } from '../services/storage.service';
import { isReadableAccessToken } from '../utils/jwt.util';

/** Redirects authenticated users away from sign-in pages to the dashboard. */
@Injectable({ providedIn: 'root' })
export class GuestGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree {
    const token = this.storage.getToken();
    if (isReadableAccessToken(token)) {
      return this.router.createUrlTree(['/dashboard']);
    }
    if (token) {
      this.storage.clearSession();
    }
    return true;
  }
}
