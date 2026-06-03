import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HelpRoutingModule } from './help-routing.module';
import { HelpSupportAdminPageComponent } from './pages/help-support-admin-page/help-support-admin-page.component';

@NgModule({
  declarations: [HelpSupportAdminPageComponent],
  imports: [SharedModule, HelpRoutingModule],
})
export class HelpModule {}
