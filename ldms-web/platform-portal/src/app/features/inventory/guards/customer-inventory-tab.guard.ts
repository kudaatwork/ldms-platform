import { CanMatchFn } from '@angular/router';
import {
  isCustomerCatalogTab,
  isCustomerOrderTab,
} from '../utils/customer-inventory-tabs.util';

/** Routes catalog tabs to InventoryWorkspaceComponent under /my-orders. */
export const customerCatalogTabCanMatch: CanMatchFn = (_route, segments) => {
  const tab = segments.at(-1)?.path ?? '';
  return isCustomerCatalogTab(tab);
};

/** Routes procurement tabs to OrdersWorkspaceComponent under /my-orders. */
export const customerOrderTabCanMatch: CanMatchFn = (_route, segments) => {
  const tab = segments.at(-1)?.path ?? '';
  return isCustomerOrderTab(tab);
};
