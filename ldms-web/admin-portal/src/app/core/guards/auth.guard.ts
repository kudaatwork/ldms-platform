import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { StorageService } from '../services/storage.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree {
    if (this.storage.getToken()) {
      return true;
    }
    return this.router.createUrlTree(['/auth/login'], {
      queryParams: { returnUrl: this.router.url },
    });
  }
}
