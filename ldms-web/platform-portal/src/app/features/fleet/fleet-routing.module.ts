import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FleetWorkspaceComponent } from './pages/fleet-workspace/fleet-workspace.component';

const routes: Routes = [
  {
    path: '',
    component: FleetWorkspaceComponent,
    data: { title: 'Fleet & Transporters', breadcrumb: 'Fleet & Transporters' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FleetRoutingModule {}
