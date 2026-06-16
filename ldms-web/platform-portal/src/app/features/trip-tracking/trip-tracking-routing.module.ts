import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TripTrackingWorkspaceComponent } from './pages/trip-tracking-workspace/trip-tracking-workspace.component';
import { LiveTripTrackingComponent } from './pages/live-trip-tracking/live-trip-tracking.component';
import { ActiveClearancesWorkspaceComponent } from './pages/active-clearances-workspace/active-clearances-workspace.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'shipments',
    pathMatch: 'full',
  },
  {
    path: 'live/:tripId',
    component: LiveTripTrackingComponent,
    data: { title: 'Live tracking', breadcrumb: 'Live tracking' },
  },
  {
    path: 'clearances',
    component: ActiveClearancesWorkspaceComponent,
    data: { title: 'Border clearance', breadcrumb: 'Border clearance' },
  },
  {
    path: 'shipments',
    component: TripTrackingWorkspaceComponent,
    data: { tab: 'shipments', title: 'Shipments', breadcrumb: 'Shipments' },
  },
  {
    path: 'trips',
    component: TripTrackingWorkspaceComponent,
    data: { tab: 'trips', title: 'Trips', breadcrumb: 'Trips' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class TripTrackingRoutingModule {}
