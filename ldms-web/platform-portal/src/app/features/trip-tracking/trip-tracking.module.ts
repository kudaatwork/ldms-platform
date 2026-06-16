import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { TripTrackingRoutingModule } from './trip-tracking-routing.module';
import { TripTrackingWorkspaceComponent } from './pages/trip-tracking-workspace/trip-tracking-workspace.component';
import { AllocateShipmentDialogComponent } from './components/allocate-shipment-dialog/allocate-shipment-dialog.component';
import { AssignTransportCompanyDialogComponent } from './components/assign-transport-company-dialog/assign-transport-company-dialog.component';
import { ViewTripDialogComponent } from './components/view-trip-dialog/view-trip-dialog.component';
import { LiveTripTrackingComponent } from './pages/live-trip-tracking/live-trip-tracking.component';
import { ActiveClearancesWorkspaceComponent } from './pages/active-clearances-workspace/active-clearances-workspace.component';

@NgModule({
  declarations: [
    TripTrackingWorkspaceComponent,
    LiveTripTrackingComponent,
    ActiveClearancesWorkspaceComponent,
    AllocateShipmentDialogComponent,
    ViewTripDialogComponent,
  ],
  imports: [
    SharedModule,
    TripTrackingRoutingModule,
    AssignTransportCompanyDialogComponent,
  ],
})
export class TripTrackingModule {}
