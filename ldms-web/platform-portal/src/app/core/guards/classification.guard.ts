import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class ClassificationGuard implements CanActivate {
  constructor(
    private readonly authState: AuthStateService,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree {
    if (!this.authState.currentUser) {
      this.authService.bootstrapFromStorage();
    }
    const user = this.authState.currentUser;
    if (!user?.orgClassification) {
      return this.router.createUrlTree(['/welcome']);
    }
    return true;
  }
}
