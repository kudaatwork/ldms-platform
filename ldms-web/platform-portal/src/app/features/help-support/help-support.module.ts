import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { HelpSupportRoutingModule } from './help-support-routing.module';
import { HelpSupportPageComponent } from './pages/help-support-page/help-support-page.component';
import { BotMessageMarkdownPipe } from './pipes/bot-message-markdown.pipe';

@NgModule({
  declarations: [HelpSupportPageComponent],
  imports: [SharedModule, HelpSupportRoutingModule, BotMessageMarkdownPipe],
})
export class HelpSupportModule {}
