import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FleetWorkspaceComponent } from './pages/fleet-workspace/fleet-workspace.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'overview',
    pathMatch: 'full',
  },
  {
    path: 'overview',
    component: FleetWorkspaceComponent,
    data: { tab: 'overview', title: 'Fleet overview', breadcrumb: 'Overview' },
  },
  {
    path: 'convoy',
    component: FleetWorkspaceComponent,
    data: { tab: 'convoy', title: 'Own fleet', breadcrumb: 'Own fleet' },
  },
  {
    path: 'partners',
    component: FleetWorkspaceComponent,
    data: { tab: 'partners', title: 'Contracted transporters', breadcrumb: 'Contracted transporters' },
  },
  {
    path: 'drivers',
    component: FleetWorkspaceComponent,
    data: { tab: 'drivers', title: 'Drivers', breadcrumb: 'Drivers' },
  },
  {
    path: 'compliance',
    component: FleetWorkspaceComponent,
    data: { tab: 'compliance', title: 'Fleet compliance', breadcrumb: 'Compliance' },
  },
  {
    path: 'tracking',
    component: FleetWorkspaceComponent,
    data: { tab: 'tracking', title: 'Device installation', breadcrumb: 'Device installation' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FleetRoutingModule {}
