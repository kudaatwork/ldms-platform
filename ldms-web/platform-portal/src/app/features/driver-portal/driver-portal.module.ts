import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { DriverPortalRoutingModule } from './driver-portal-routing.module';
import { DriverShellComponent } from './components/driver-shell/driver-shell.component';
import { DriverWorkspaceComponent } from './pages/driver-workspace/driver-workspace.component';
import { DriverTripDetailComponent } from './pages/driver-trip-detail/driver-trip-detail.component';
import { DriverSignupComponent } from './pages/driver-signup/driver-signup.component';
import { DriverLiveHubComponent } from './pages/driver-live-hub/driver-live-hub.component';
import { DriverProfileComponent } from './pages/driver-profile/driver-profile.component';
import { DriverChatComponent } from './pages/driver-chat/driver-chat.component';
import { DeliveryWorkflowComponent } from './components/delivery-workflow/delivery-workflow.component';
import { TripTrackingComponentsModule } from '../trip-tracking/trip-tracking-components.module';

@NgModule({
  declarations: [
    DriverShellComponent,
    DriverWorkspaceComponent,
    DriverTripDetailComponent,
    DriverSignupComponent,
    DriverLiveHubComponent,
    DriverProfileComponent,
    DriverChatComponent,
    DeliveryWorkflowComponent,
  ],
  imports: [
    SharedModule,
    DriverPortalRoutingModule,
    TripTrackingComponentsModule,
  ],
})
export class DriverPortalModule {}
