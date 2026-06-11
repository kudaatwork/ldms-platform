import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { OrdersWorkspaceComponent } from './pages/orders-workspace/orders-workspace.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'requisitions',
    pathMatch: 'full',
  },
  {
    path: ':tab',
    component: OrdersWorkspaceComponent,
    data: { title: 'My Orders', breadcrumb: 'My Orders' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class OrdersRoutingModule {}
