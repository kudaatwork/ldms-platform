import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';

const CROSS_DOCK_ALLOWED_TABS = new Set(['integration-setup']);

@Injectable({ providedIn: 'root' })
export class InventoryOperationalModeGuard implements CanActivate {
  constructor(
    private readonly authState: AuthStateService,
    private readonly router: Router,
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    const user = this.authState.currentUser;
    if (!user) {
      return true;
    }

    const path = route.routeConfig?.path ?? '';
    const tab = String(route.params['tab'] ?? '');

    // API documentation is always reachable — suppliers use it to plan ERP integration.
    if (path === 'integration-api') {
      return true;
    }

    const inventoryMgmtEnabled = user.inventoryManagementEnabled !== false;
    const crossDockOnly = !!user.crossDockingEnabled && user.inventoryManagementEnabled === false;

    if (crossDockOnly) {
      if (CROSS_DOCK_ALLOWED_TABS.has(tab)) {
        return true;
      }
      return this.router.createUrlTree(['/products-inventory/integration-api'], {
        queryParams: { mode: 'crossdock' },
      });
    }

    const usesExternalInventoryApi = user.inventoryDataSource === 'EXTERNAL_API';
    if (inventoryMgmtEnabled && !usesExternalInventoryApi && tab === 'integration-setup') {
      return this.router.createUrlTree(['/products-inventory/warehouses']);
    }

    return true;
  }
}
