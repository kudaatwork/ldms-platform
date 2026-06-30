import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { StorageService } from '../../../core/services/storage.service';
import { TokenRefreshService } from '../../../core/services/token-refresh.service';
import { currentUserFromJwt, decodeJwtPayload, isJwtExpired } from '../../../core/utils/jwt.util';

/**
 * Protects clerk workspace and stock-receive routes.
 * Redirects unauthenticated users to clerk sign-in.
 */
@Injectable({ providedIn: 'root' })
export class ClerkAuthGuard implements CanActivate {
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
        portal: 'clerk',
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
    const mustChange =
      this.authState.currentUser?.mustChangeCredentials ??
      decodeJwtPayload(current)?.mustChangeCredentials === true;
    if (mustChange) {
      return this.router.createUrlTree(['/auth/setup-credentials'], {
        queryParams: { portal: 'clerk' },
      });
    }
    return true;
  }
}
