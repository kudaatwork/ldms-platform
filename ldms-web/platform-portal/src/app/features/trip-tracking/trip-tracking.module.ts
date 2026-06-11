import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { TripTrackingRoutingModule } from './trip-tracking-routing.module';
import { TripTrackingWorkspaceComponent } from './pages/trip-tracking-workspace/trip-tracking-workspace.component';
import { AllocateShipmentDialogComponent } from './components/allocate-shipment-dialog/allocate-shipment-dialog.component';
import { ViewTripDialogComponent } from './components/view-trip-dialog/view-trip-dialog.component';

@NgModule({
  declarations: [
    TripTrackingWorkspaceComponent,
    AllocateShipmentDialogComponent,
    ViewTripDialogComponent,
  ],
  imports: [
    SharedModule,
    TripTrackingRoutingModule,
  ],
})
export class TripTrackingModule {}
