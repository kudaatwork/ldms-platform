import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TripTrackingWorkspaceComponent } from './pages/trip-tracking-workspace/trip-tracking-workspace.component';
import { LiveTripTrackingComponent } from './pages/live-trip-tracking/live-trip-tracking.component';
import { TripHistoryPageComponent } from './pages/trip-history-page/trip-history-page.component';
import { TripReplayPageComponent } from './pages/trip-replay-page/trip-replay-page.component';
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
    path: 'replay/:tripId',
    component: TripReplayPageComponent,
    data: { title: 'Trip replay', breadcrumb: 'Trip replay' },
  },
  {
    path: 'history',
    component: TripHistoryPageComponent,
    data: { title: 'Trip history', breadcrumb: 'Trip history' },
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
