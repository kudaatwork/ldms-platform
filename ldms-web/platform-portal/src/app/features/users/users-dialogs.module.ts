import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { UserEditProfileDialogComponent } from './components/user-edit-profile-dialog/user-edit-profile-dialog.component';
import { UserEditAccountDialogComponent } from './components/user-edit-account-dialog/user-edit-account-dialog.component';
import { UserEditAddressDialogComponent } from './components/user-edit-address-dialog/user-edit-address-dialog.component';
import { UserEditSecurityDialogComponent } from './components/user-edit-security-dialog/user-edit-security-dialog.component';
import { UserAddressCascadeFieldsComponent } from './components/user-address-cascade-fields/user-address-cascade-fields.component';

/** Edit dialogs shared by Users admin pages and `/account` (eager AppModule route). */
@NgModule({
  declarations: [
    UserEditProfileDialogComponent,
    UserEditAccountDialogComponent,
    UserEditAddressDialogComponent,
    UserEditSecurityDialogComponent,
  ],
  imports: [SharedModule, UserAddressCascadeFieldsComponent],
  exports: [
    UserEditProfileDialogComponent,
    UserEditAccountDialogComponent,
    UserEditAddressDialogComponent,
    UserEditSecurityDialogComponent,
  ],
})
export class UsersDialogsModule {}
