import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { map, Observable, of } from 'rxjs';
import type { CurrentUser } from '../models/auth.model';
import { AuthStateService } from '../services/auth-state.service';
import { AuthService } from '../services/auth.service';
import { StorageService } from '../services/storage.service';

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
        if (user?.orgClassification) {
          return true;
        }
        const token = this.storage.getToken();
        if (token && !token.startsWith('mock.')) {
          // Signed-in workspace — allow entry while org profile loads; do not bounce to login.
          return true;
        }
        return this.router.createUrlTree(['/welcome']);
      }),
    );
  }

  private ensureUserLoaded(): Observable<CurrentUser | null> {
    const existing = this.authState.currentUser;
    if (existing?.orgClassification) {
      return of(existing);
    }
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      return of(null);
    }
    return this.authService.initializeSession().pipe(map(() => this.authState.currentUser));
  }
}
