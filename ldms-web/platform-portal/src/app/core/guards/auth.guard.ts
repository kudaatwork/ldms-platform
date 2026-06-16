import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { StorageService } from '../services/storage.service';
import { TokenRefreshService } from '../services/token-refresh.service';
import { currentUserFromJwt, decodeJwtPayload, isJwtExpired } from '../utils/jwt.util';

/** Requires a stored access token (real JWT or valid session). */
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly authService: AuthService,
    private readonly tokenRefresh: TokenRefreshService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree | Observable<boolean | UrlTree> {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      return this.router.createUrlTree(['/auth/login']);
    }

    if (isJwtExpired(token)) {
      if (!this.storage.getRefreshToken()) {
        this.authService.logout();
        return this.router.createUrlTree(['/auth/login'], { queryParams: { reason: 'session_expired' } });
      }
      return this.tokenRefresh.refreshAccessToken().pipe(
        map(() => this.resolveAfterTokenValid(this.storage.getToken() ?? token)),
        catchError(() => {
          this.authService.logout();
          return of(this.router.createUrlTree(['/auth/login'], { queryParams: { reason: 'session_expired' } }));
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
    if (this.mustChangeCredentials(current)) {
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
