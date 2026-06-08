import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FleetWorkspaceComponent } from './pages/fleet-workspace/fleet-workspace.component';

const routes: Routes = [
  {
    path: '',
    component: FleetWorkspaceComponent,
    data: { title: 'Fleet & Transporters', breadcrumb: 'Fleet & Transporters' },
  },
  {
    path: 'drivers',
    component: FleetWorkspaceComponent,
    data: { title: 'Drivers', breadcrumb: 'Drivers', tab: 'drivers' },
  },
  {
    path: 'compliance',
    component: FleetWorkspaceComponent,
    data: { title: 'Compliance', breadcrumb: 'Compliance', tab: 'compliance' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FleetRoutingModule {}
