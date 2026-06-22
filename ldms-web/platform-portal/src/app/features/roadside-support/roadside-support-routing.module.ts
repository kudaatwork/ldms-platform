import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RoadsideWorkspaceComponent } from './pages/roadside-workspace/roadside-workspace.component';

const routes: Routes = [
  {
    path: 'truck-visits',
    component: RoadsideWorkspaceComponent,
    data: { section: 'visits', title: 'Truck visits', breadcrumb: 'Truck visits' },
  },
  {
    path: 'fuel-log',
    component: RoadsideWorkspaceComponent,
    data: { section: 'fuel-log', title: 'Fuel log', breadcrumb: 'Fuel log' },
  },
  {
    path: 'incidents',
    component: RoadsideWorkspaceComponent,
    data: { section: 'incidents', title: 'Incidents', breadcrumb: 'Incidents' },
  },
  {
    path: 'service-log',
    component: RoadsideWorkspaceComponent,
    data: { section: 'service-log', title: 'Service log', breadcrumb: 'Service log' },
  },
  { path: '', pathMatch: 'full', redirectTo: 'truck-visits' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class RoadsideSupportRoutingModule {}
