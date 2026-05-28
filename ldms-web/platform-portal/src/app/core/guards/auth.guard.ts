import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { StorageService } from '../services/storage.service';

/** Requires a stored access token (real JWT or valid session). */
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      return this.router.createUrlTree(['/auth/login']);
    }
    this.authService.bootstrapFromStorage();
    return true;
  }
}
