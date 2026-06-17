import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HelpRoutingModule } from './help-routing.module';
import { HelpShellComponent } from './components/help-shell/help-shell.component';
import { HelpSupportAdminPageComponent } from './pages/help-support-admin-page/help-support-admin-page.component';
import { DemoRequisitionsAdminPageComponent } from './pages/demo-requisitions-admin-page/demo-requisitions-admin-page.component';
import { BotServiceAdminPageComponent } from './pages/bot-service-admin-page/bot-service-admin-page.component';

@NgModule({
  declarations: [
    HelpShellComponent,
    HelpSupportAdminPageComponent,
    DemoRequisitionsAdminPageComponent,
    BotServiceAdminPageComponent,
  ],
  imports: [SharedModule, HelpRoutingModule],
})
export class HelpModule {}
