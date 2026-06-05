import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CustomersListComponent } from './pages/customers-list/customers-list.component';

const routes: Routes = [
  { path: '', component: CustomersListComponent, data: { title: 'Customers', breadcrumb: 'Customers' } },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class CustomersRoutingModule {}
