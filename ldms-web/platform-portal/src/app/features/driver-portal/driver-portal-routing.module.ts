import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DriverShellComponent } from './components/driver-shell/driver-shell.component';
import { DriverWorkspaceComponent } from './pages/driver-workspace/driver-workspace.component';
import { DriverTripDetailComponent } from './pages/driver-trip-detail/driver-trip-detail.component';
import { DriverSignupComponent } from './pages/driver-signup/driver-signup.component';
import { LiveTripTrackingComponent } from '../trip-tracking/pages/live-trip-tracking/live-trip-tracking.component';
import { DriverLiveHubComponent } from './pages/driver-live-hub/driver-live-hub.component';
import { DriverAuthGuard } from '../../core/guards/driver-auth.guard';

const routes: Routes = [
  // Public: driver signup/registration — no auth guard
  {
    path: 'signup',
    component: DriverSignupComponent,
    data: { title: 'Driver registration' },
  },
  // Authenticated driver routes wrapped in shell with bottom nav
  {
    path: '',
    component: DriverShellComponent,
    canActivate: [DriverAuthGuard],
    children: [
      { path: '', redirectTo: 'workspace', pathMatch: 'full' },
      {
        path: 'workspace',
        component: DriverWorkspaceComponent,
        data: { title: 'Driver workspace' },
      },
      {
        path: 'live',
        component: DriverLiveHubComponent,
        data: { title: 'Live tracking' },
      },
      {
        path: 'live/:tripId',
        component: LiveTripTrackingComponent,
        data: { title: 'Live trip map' },
      },
      {
        path: 'trip/:tripId',
        component: DriverTripDetailComponent,
        data: { title: 'Trip detail' },
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class DriverPortalRoutingModule {}
