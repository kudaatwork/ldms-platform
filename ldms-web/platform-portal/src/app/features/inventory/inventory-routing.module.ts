import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { InventoryWorkspaceComponent } from './pages/inventory-workspace/inventory-workspace.component';
import { InventoryIntegrationApiPageComponent } from './pages/inventory-integration-api-page/inventory-integration-api-page.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'warehouses',
    pathMatch: 'full',
  },
  {
    path: 'integration-api',
    component: InventoryIntegrationApiPageComponent,
    data: { title: 'Inventory Integration API', breadcrumb: 'Inventory integration API' },
  },
  {
    path: ':tab',
    component: InventoryWorkspaceComponent,
    data: { title: 'Inventory management', breadcrumb: 'Inventory management' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class InventoryRoutingModule {}
