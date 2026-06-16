import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { StorageService } from '../services/storage.service';
import { TokenRefreshService } from '../services/token-refresh.service';
import { currentUserFromJwt, isJwtExpired } from '../utils/jwt.util';
import type { CurrentUser } from '../models/auth.model';
import { portalHomeRoute } from '../utils/portal-navigation.util';

@Injectable({ providedIn: 'root' })
export class GuestGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly authService: AuthService,
    private readonly authState: AuthStateService,
    private readonly tokenRefresh: TokenRefreshService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree | Observable<boolean | UrlTree> {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      return true;
    }
    if (isJwtExpired(token)) {
      if (!this.storage.getRefreshToken()) {
        this.authService.logout();
        return true;
      }
      return this.tokenRefresh.refreshAccessToken().pipe(
        map(() => true),
        catchError(() => {
          this.authService.logout();
          return of(true);
        }),
      );
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
