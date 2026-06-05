import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { map, Observable, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { StorageService } from '../services/storage.service';
import { currentUserFromJwt } from '../utils/jwt.util';
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
    if (this.authState.currentUser) {
      return this.router.createUrlTree(portalHomeRoute(this.authState.currentUser));
    }
    const jwtUser = currentUserFromJwt(token);
    if (jwtUser?.orgClassification) {
      this.authState.setCurrentUser(jwtUser);
      this.authService.bootstrapFromStorage();
      return this.router.createUrlTree(portalHomeRoute(jwtUser));
    }
    return this.authService.initializeSession().pipe(
      map(() =>
        this.authState.currentUser
          ? this.router.createUrlTree(portalHomeRoute(this.authState.currentUser))
          : true,
      ),
    );
  }
}
