import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { StorageService } from '../services/storage.service';
import { TokenRefreshService } from '../services/token-refresh.service';
import { currentUserFromJwt, decodeJwtPayload, isJwtExpired } from '../utils/jwt.util';

/**
 * Protects driver workspace and trip-detail routes.
 * Redirects unauthenticated users to driver sign-in.
 */
@Injectable({ providedIn: 'root' })
export class DriverAuthGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly authService: AuthService,
    private readonly tokenRefresh: TokenRefreshService,
    private readonly router: Router,
  ) {}

  private loginTree(reason?: string): UrlTree {
    return this.router.createUrlTree(['/auth/login'], {
      queryParams: {
        portal: 'driver',
        ...(reason ? { reason } : {}),
      },
    });
  }

  canActivate(): boolean | UrlTree | Observable<boolean | UrlTree> {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      return this.loginTree();
    }

    if (isJwtExpired(token)) {
      if (!this.storage.getRefreshToken()) {
        this.authService.logout();
        return this.loginTree('session_expired');
      }
      return this.tokenRefresh.refreshAccessToken().pipe(
        map(() => this.resolveAfterTokenValid(this.storage.getToken() ?? token)),
        catchError(() => {
          this.authService.logout();
          return of(this.loginTree('session_expired'));
        }),
      );
    }

    return this.resolveAfterTokenValid(token);
  }

  private resolveAfterTokenValid(token: string): boolean | UrlTree {
    const current = this.storage.getToken() ?? token;
    if (!this.authState.currentUser) {
      const jwtUser = currentUserFromJwt(current);
      if (jwtUser) {
        this.authState.setCurrentUser(jwtUser);
      }
    }
    // If the user must change their credentials, redirect to setup page with driver context.
    const mustChange =
      this.authState.currentUser?.mustChangeCredentials ??
      decodeJwtPayload(current)?.mustChangeCredentials === true;
    if (mustChange) {
      return this.router.createUrlTree(['/auth/setup-credentials'], {
        queryParams: { portal: 'driver' },
      });
    }
    return true;
  }
}
