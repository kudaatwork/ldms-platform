import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { CustomersRoutingModule } from './customers-routing.module';
import { CustomersListComponent } from './pages/customers-list/customers-list.component';
import { DuplexModeOfferDialogComponent } from './components/duplex-mode-offer-dialog/duplex-mode-offer-dialog.component';
import { RegisterCustomerDialogComponent } from './components/register-customer-dialog/register-customer-dialog.component';
import { UserAddressCascadeFieldsComponent } from '../users/components/user-address-cascade-fields/user-address-cascade-fields.component';

@NgModule({
  declarations: [CustomersListComponent, RegisterCustomerDialogComponent, DuplexModeOfferDialogComponent],
  imports: [SharedModule, CustomersRoutingModule, UserAddressCascadeFieldsComponent],
})
export class CustomersModule {}
