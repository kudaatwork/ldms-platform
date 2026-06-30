import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { InventoryWorkspaceComponent } from './pages/inventory-workspace/inventory-workspace.component';
import { OrdersWorkspaceComponent } from './pages/orders-workspace/orders-workspace.component';
import {
  customerCatalogTabCanMatch,
  customerOrderTabCanMatch,
} from './guards/customer-inventory-tab.guard';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'warehouses',
    pathMatch: 'full',
  },
  {
    path: ':tab',
    canMatch: [customerCatalogTabCanMatch],
    component: InventoryWorkspaceComponent,
    data: { title: 'Inventory management', breadcrumb: 'Inventory management', customerRoute: true },
  },
  {
    path: ':tab',
    canMatch: [customerOrderTabCanMatch],
    component: OrdersWorkspaceComponent,
    data: { title: 'Inventory management', breadcrumb: 'Inventory management' },
  },
  {
    path: '**',
    redirectTo: 'warehouses',
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class OrdersRoutingModule {}
