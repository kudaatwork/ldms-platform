import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { map, Observable, of } from 'rxjs';
import type { CurrentUser } from '../models/auth.model';
import { AuthStateService } from '../services/auth-state.service';
import { AuthService } from '../services/auth.service';
import { StorageService } from '../services/storage.service';
import { currentUserFromJwt } from '../utils/jwt.util';

@Injectable({ providedIn: 'root' })
export class ClassificationGuard implements CanActivate {
  constructor(
    private readonly authState: AuthStateService,
    private readonly authService: AuthService,
    private readonly storage: StorageService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree | Observable<boolean | UrlTree> {
    return this.ensureUserLoaded().pipe(
      map((user) => {
        if (!user?.orgClassification) {
          return this.router.createUrlTree(['/welcome']);
        }
        return true;
      }),
    );
  }

  private ensureUserLoaded(): Observable<CurrentUser | null> {
    if (this.authState.currentUser) {
      return of(this.authState.currentUser);
    }
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      return of(null);
    }
    const jwtUser = currentUserFromJwt(token);
    if (jwtUser?.orgClassification) {
      this.authState.setCurrentUser(jwtUser);
      this.authService.bootstrapFromStorage();
      return of(jwtUser);
    }
    return this.authService.initializeSession().pipe(map(() => this.authState.currentUser));
  }
}
