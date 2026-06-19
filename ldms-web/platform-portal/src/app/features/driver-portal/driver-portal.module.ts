import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { DriverPortalRoutingModule } from './driver-portal-routing.module';
import { DriverShellComponent } from './components/driver-shell/driver-shell.component';
import { DriverWorkspaceComponent } from './pages/driver-workspace/driver-workspace.component';
import { DriverTripDetailComponent } from './pages/driver-trip-detail/driver-trip-detail.component';
import { DriverSignupComponent } from './pages/driver-signup/driver-signup.component';
import { DeliveryWorkflowComponent } from './components/delivery-workflow/delivery-workflow.component';

@NgModule({
  declarations: [
    DriverShellComponent,
    DriverWorkspaceComponent,
    DriverTripDetailComponent,
    DriverSignupComponent,
    DeliveryWorkflowComponent,
  ],
  imports: [
    SharedModule,
    DriverPortalRoutingModule,
  ],
})
export class DriverPortalModule {}
