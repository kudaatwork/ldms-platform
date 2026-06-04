import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { UsersDialogsModule } from '../users/users-dialogs.module';
import { MyAccountComponent } from './pages/my-account/my-account.component';

/** My Account lives outside lazy UsersModule but shares the same edit dialogs. */
@NgModule({
  declarations: [MyAccountComponent],
  imports: [SharedModule, UsersDialogsModule],
  exports: [MyAccountComponent],
})
export class AccountModule {}
