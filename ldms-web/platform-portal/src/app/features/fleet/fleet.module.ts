import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { FleetRoutingModule } from './fleet-routing.module';
import { FleetWorkspaceComponent } from './pages/fleet-workspace/fleet-workspace.component';
import { RegisterTransporterDialogComponent } from './components/register-transporter-dialog/register-transporter-dialog.component';
import { OwnFleetDialogComponent } from './components/own-fleet-dialog/own-fleet-dialog.component';
import { FleetDriverDialogComponent } from './components/fleet-driver-dialog/fleet-driver-dialog.component';
import { FleetAssignDriverDialogComponent } from './components/fleet-assign-driver-dialog/fleet-assign-driver-dialog.component';
import { FleetComplianceDialogComponent } from './components/fleet-compliance-dialog/fleet-compliance-dialog.component';
import { FleetComplianceBundleReviewDialogComponent } from './components/fleet-compliance-bundle-review-dialog/fleet-compliance-bundle-review-dialog.component';
import { FleetInstallTrackingDeviceDialogComponent } from './components/fleet-install-tracking-device-dialog/fleet-install-tracking-device-dialog.component';
import { FleetTrackingSubnavComponent } from './components/fleet-tracking-subnav/fleet-tracking-subnav.component';
import { FleetTrackingApiPageComponent } from './pages/fleet-tracking-api-page/fleet-tracking-api-page.component';
import { FleetTrackingIntegrationSetupPageComponent } from './pages/fleet-tracking-integration-setup-page/fleet-tracking-integration-setup-page.component';
import { UserAddressCascadeFieldsComponent } from '../users/components/user-address-cascade-fields/user-address-cascade-fields.component';
import { DriverMarketplaceSearchComponent } from './components/driver-marketplace-search/driver-marketplace-search.component';
import { DriverSignupRequestsComponent } from './components/driver-signup-requests/driver-signup-requests.component';

@NgModule({
  declarations: [
    FleetWorkspaceComponent,
    RegisterTransporterDialogComponent,
    FleetTrackingSubnavComponent,
    FleetTrackingApiPageComponent,
    FleetTrackingIntegrationSetupPageComponent,
  ],
  exports: [RegisterTransporterDialogComponent],
  imports: [
    SharedModule,
    FleetRoutingModule,
    UserAddressCascadeFieldsComponent,
    OwnFleetDialogComponent,
    FleetDriverDialogComponent,
    FleetAssignDriverDialogComponent,
    FleetComplianceDialogComponent,
    FleetComplianceBundleReviewDialogComponent,
    FleetInstallTrackingDeviceDialogComponent,
    DriverMarketplaceSearchComponent,
    DriverSignupRequestsComponent,
  ],
})
export class FleetModule {}
