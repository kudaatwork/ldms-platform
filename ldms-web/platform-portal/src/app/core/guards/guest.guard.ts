import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { map, Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { StorageService } from '../services/storage.service';
import { currentUserFromJwt, isJwtExpired } from '../utils/jwt.util';
import type { CurrentUser } from '../models/auth.model';
import { portalHomeRoute } from '../utils/portal-navigation.util';

@Injectable({ providedIn: 'root' })
export class GuestGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly authService: AuthService,
    private readonly authState: AuthStateService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree | Observable<boolean | UrlTree> {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      return true;
    }
    if (isJwtExpired(token)) {
      this.authService.logout();
      return true;
    }

    const existing = this.authState.currentUser;
    if (existing?.orgClassification) {
      return this.homeOrCredentialsRoute(existing);
    }

    const jwtUser = currentUserFromJwt(token);
    if (jwtUser?.orgClassification) {
      this.authState.setCurrentUser(jwtUser);
      this.authService.bootstrapFromStorage();
      return this.homeOrCredentialsRoute(jwtUser);
    }

    return this.authService.initializeSession().pipe(
      map(() => {
        const resolved = this.authState.currentUser;
        if (resolved?.orgClassification) {
          return this.homeOrCredentialsRoute(resolved);
        }
        // Stale or partial session — allow the login page instead of bouncing to /welcome.
        return true;
      }),
    );
  }

  private homeOrCredentialsRoute(user: CurrentUser): UrlTree {
    if (user.mustChangeCredentials) {
      return this.router.createUrlTree(['/auth/setup-credentials']);
    }
    return this.router.createUrlTree(portalHomeRoute(user));
  }
}
