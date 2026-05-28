import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { StorageService } from '../services/storage.service';

@Injectable({ providedIn: 'root' })
export class GuestGuard implements CanActivate {
  constructor(
    private readonly storage: StorageService,
    private readonly router: Router,
  ) {}

  canActivate(): boolean | UrlTree {
    if (this.storage.getToken()) {
      return this.router.createUrlTree(['/dashboard']);
    }
    return true;
  }
}
