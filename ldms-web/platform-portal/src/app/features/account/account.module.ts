import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { UsersDialogsModule } from '../users/users-dialogs.module';
import { AccountRoutingModule } from './account-routing.module';
import { MyAccountComponent } from './pages/my-account/my-account.component';

/** Lazy-loaded so edit dialogs are not in the initial bundle. */
@NgModule({
  declarations: [MyAccountComponent],
  imports: [SharedModule, UsersDialogsModule, AccountRoutingModule],
})
export class AccountModule {}
