import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { SettingsRoutingModule } from './settings-routing.module';
import { SettingsComponent } from './pages/settings/settings.component';
import { SettingsGroupRolesComponent } from './components/settings-group-roles/settings-group-roles.component';
import { SettingsKycApproversComponent } from './components/settings-kyc-approvers/settings-kyc-approvers.component';
import { SettingsCurrencyComponent } from './components/settings-currency/settings-currency.component';
import { SettingsProcurementApproversComponent } from './components/settings-procurement-approvers/settings-procurement-approvers.component';
import { SettingsBillingComponent } from './components/settings-billing/settings-billing.component';
import { SettingsOperationalModeComponent } from './components/settings-operational-mode/settings-operational-mode.component';
import { SettingsOrganizationComponent } from './components/settings-organization/settings-organization.component';
import { UserAddressCascadeFieldsComponent } from '../users/components/user-address-cascade-fields/user-address-cascade-fields.component';

@NgModule({
  declarations: [
    SettingsComponent,
    SettingsGroupRolesComponent,
    SettingsKycApproversComponent,
    SettingsCurrencyComponent,
    SettingsProcurementApproversComponent,
    SettingsBillingComponent,
    SettingsOperationalModeComponent,
    SettingsOrganizationComponent,
  ],
  imports: [SharedModule, SettingsRoutingModule, UserAddressCascadeFieldsComponent],
})
export class SettingsModule {}
