import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FleetWorkspaceComponent } from './pages/fleet-workspace/fleet-workspace.component';
import { FleetTrackingApiPageComponent } from './pages/fleet-tracking-api-page/fleet-tracking-api-page.component';
import { FleetTrackingIntegrationSetupPageComponent } from './pages/fleet-tracking-integration-setup-page/fleet-tracking-integration-setup-page.component';

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
  {
    path: 'tracking-api',
    component: FleetTrackingApiPageComponent,
    data: { tab: 'tracking', title: 'Integration API', breadcrumb: 'Integration API' },
  },
  {
    path: 'tracking-setup',
    component: FleetTrackingIntegrationSetupPageComponent,
    data: { tab: 'tracking', title: 'Integration setup', breadcrumb: 'Integration setup' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FleetRoutingModule {}
