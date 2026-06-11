import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { InventoryWorkspaceComponent } from './pages/inventory-workspace/inventory-workspace.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'warehouses',
    pathMatch: 'full',
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
