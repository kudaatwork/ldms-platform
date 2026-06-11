import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TripTrackingWorkspaceComponent } from './pages/trip-tracking-workspace/trip-tracking-workspace.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'shipments',
    pathMatch: 'full',
  },
  {
    path: ':tab',
    component: TripTrackingWorkspaceComponent,
    data: { title: 'Shipments & Trips', breadcrumb: 'Shipments & Trips' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class TripTrackingRoutingModule {}
