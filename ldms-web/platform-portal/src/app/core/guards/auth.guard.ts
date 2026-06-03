import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { StorageService } from '../services/storage.service';
import { decodeJwtPayload } from '../utils/jwt.util';

/** Requires a stored access token (real JWT or valid session). */
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      return this.router.createUrlTree(['/auth/login']);
    }
    this.authService.bootstrapFromStorage();
    if (this.mustChangeCredentials(token)) {
      return this.router.createUrlTree(['/auth/setup-credentials']);
    }
    return true;
  }

  private mustChangeCredentials(token: string): boolean {
    const user = this.authState.currentUser;
    if (user?.mustChangeCredentials) {
      return true;
    }
    const payload = decodeJwtPayload(token);
    return payload?.mustChangeCredentials === true;
  }
}
