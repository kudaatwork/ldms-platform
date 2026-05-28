import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { OrganizationsListComponent } from './pages/organizations-list/organizations-list.component';
import { OrganizationsByClassificationComponent } from './pages/organizations-by-classification/organizations-by-classification.component';
import { BranchesListComponent } from './pages/branches-list/branches-list.component';
import { AgentsListComponent } from './pages/agents-list/agents-list.component';
import { IndustriesListComponent } from './pages/industries-list/industries-list.component';
import { OrganizationDetailShellComponent } from './pages/organization-detail-shell/organization-detail-shell.component';

const routes: Routes = [
  {
    path: '',
    component: OrganizationsListComponent,
    data: { title: 'Organizations', breadcrumb: 'All' },
  },
  {
    path: 'branches',
    component: BranchesListComponent,
    data: { title: 'Branches', breadcrumb: 'Branches' },
  },
  {
    path: 'agents',
    component: AgentsListComponent,
    data: { title: 'Agents', breadcrumb: 'Agents' },
  },
  {
    path: 'industries',
    component: IndustriesListComponent,
    data: { title: 'Industries', breadcrumb: 'Industries' },
  },
  {
    path: 'classification/:slug',
    component: OrganizationsByClassificationComponent,
    data: { title: 'Organizations', breadcrumb: 'Classification' },
  },
  {
    path: ':orgId',
    component: OrganizationDetailShellComponent,
    data: { title: 'Organisation', breadcrumb: 'Detail' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class OrganizationsRoutingModule {}
