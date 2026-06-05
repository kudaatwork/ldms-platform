import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { FleetRoutingModule } from './fleet-routing.module';
import { FleetWorkspaceComponent } from './pages/fleet-workspace/fleet-workspace.component';
import { RegisterTransporterDialogComponent } from './components/register-transporter-dialog/register-transporter-dialog.component';
import { UserAddressCascadeFieldsComponent } from '../users/components/user-address-cascade-fields/user-address-cascade-fields.component';

@NgModule({
  declarations: [FleetWorkspaceComponent, RegisterTransporterDialogComponent],
  imports: [SharedModule, FleetRoutingModule, UserAddressCascadeFieldsComponent],
})
export class FleetModule {}
