import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { SettingsRoutingModule } from './settings-routing.module';
import { SettingsComponent } from './pages/settings/settings.component';
import { SettingsGroupRolesComponent } from './components/settings-group-roles/settings-group-roles.component';
import { SettingsKycApproversComponent } from './components/settings-kyc-approvers/settings-kyc-approvers.component';

@NgModule({
  declarations: [SettingsComponent, SettingsGroupRolesComponent, SettingsKycApproversComponent],
  imports: [SharedModule, SettingsRoutingModule],
})
export class SettingsModule {}
