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
        if (!user?.orgClassification) {
          const token = this.storage.getToken();
          if (token && !token.startsWith('mock.')) {
            return this.router.createUrlTree(['/auth/login']);
          }
          return this.router.createUrlTree(['/welcome']);
        }
        return true;
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
