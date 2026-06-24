import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { SettingsRoutingModule } from './settings-routing.module';
import { SettingsComponent } from './pages/settings/settings.component';
import { SettingsGroupRolesComponent } from './components/settings-group-roles/settings-group-roles.component';
import { SettingsKycApproversComponent } from './components/settings-kyc-approvers/settings-kyc-approvers.component';
import { SettingsCurrencyComponent } from './components/settings-currency/settings-currency.component';
import { SettingsPlatformBillingComponent } from './components/settings-platform-billing/settings-platform-billing.component';
import { SettingsOrganizationDocumentsComponent } from './components/settings-organization-documents/settings-organization-documents.component';
import { PlatformActionChargeFormDialogComponent } from './components/platform-action-charge-form-dialog/platform-action-charge-form-dialog.component';
import { SubscriptionPackageFormDialogComponent } from './components/subscription-package-form-dialog/subscription-package-form-dialog.component';
import { WalletDepositDetailDialogComponent } from './components/wallet-deposit-detail-dialog/wallet-deposit-detail-dialog.component';
import { WalletDepositRejectDialogComponent } from './components/wallet-deposit-reject-dialog/wallet-deposit-reject-dialog.component';
import { OrganizationDocumentsPanelComponent } from '../../shared/components/organization-documents-panel/organization-documents-panel.component';

@NgModule({
  declarations: [
    SettingsComponent,
    SettingsGroupRolesComponent,
    SettingsKycApproversComponent,
    SettingsCurrencyComponent,
    SettingsPlatformBillingComponent,
    SettingsOrganizationDocumentsComponent,
    PlatformActionChargeFormDialogComponent,
    SubscriptionPackageFormDialogComponent,
    WalletDepositDetailDialogComponent,
    WalletDepositRejectDialogComponent,
  ],
  imports: [SharedModule, SettingsRoutingModule, OrganizationDocumentsPanelComponent],
})
export class SettingsModule {}
