import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { OrganizationsListComponent } from './pages/organizations-list/organizations-list.component';
import { OrganizationsByClassificationComponent } from './pages/organizations-by-classification/organizations-by-classification.component';

const routes: Routes = [
  {
    path: '',
    component: OrganizationsListComponent,
    data: { title: 'Organizations', breadcrumb: 'All' },
  },
  {
    path: 'classification/:slug',
    component: OrganizationsByClassificationComponent,
    data: { title: 'Organizations', breadcrumb: 'Classification' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class OrganizationsRoutingModule {}
