import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';
import { StorageService } from '../services/storage.service';
import { decodeJwtPayload } from '../utils/jwt.util';

/** Requires a session that still needs permanent username/password setup. */
@Injectable({ providedIn: 'root' })
export class SetupCredentialsGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      return this.router.createUrlTree(['/auth/login']);
    }
    const mustChange = this.readMustChangeCredentials(token);
    if (!mustChange) {
      return this.router.createUrlTree(['/dashboard']);
    }
    return true;
  }

  private readMustChangeCredentials(token: string): boolean {
    const user = this.authState.currentUser;
    if (user?.mustChangeCredentials) {
      return true;
    }
    const payload = decodeJwtPayload(token);
    return payload?.mustChangeCredentials === true;
  }
}
