import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { TripTrackingComponentsModule } from './trip-tracking-components.module';
import { TripTrackingRoutingModule } from './trip-tracking-routing.module';
import { TripTrackingWorkspaceComponent } from './pages/trip-tracking-workspace/trip-tracking-workspace.component';
import { AllocateShipmentDialogComponent } from './components/allocate-shipment-dialog/allocate-shipment-dialog.component';
import { AssignTransportCompanyDialogComponent } from './components/assign-transport-company-dialog/assign-transport-company-dialog.component';
import { ViewTripDialogComponent } from './components/view-trip-dialog/view-trip-dialog.component';
import { ActiveClearancesWorkspaceComponent } from './pages/active-clearances-workspace/active-clearances-workspace.component';
import { TripHistoryPageComponent } from './pages/trip-history-page/trip-history-page.component';
import { TripReplayPageComponent } from './pages/trip-replay-page/trip-replay-page.component';

@NgModule({
  declarations: [
    TripTrackingWorkspaceComponent,
    TripHistoryPageComponent,
    TripReplayPageComponent,
    ActiveClearancesWorkspaceComponent,
    AllocateShipmentDialogComponent,
    ViewTripDialogComponent,
  ],
  imports: [
    SharedModule,
    TripTrackingComponentsModule,
    TripTrackingRoutingModule,
    AssignTransportCompanyDialogComponent,
  ],
  exports: [TripTrackingComponentsModule],
})
export class TripTrackingModule {}
