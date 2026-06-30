import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { ClerkPortalRoutingModule } from './clerk-portal-routing.module';
import { ClerkShellComponent } from './components/clerk-shell/clerk-shell.component';
import { ClerkWorkspaceComponent } from './pages/clerk-workspace/clerk-workspace.component';
import { ClerkStockReceiveComponent } from './pages/clerk-stock-receive/clerk-stock-receive.component';
import { ClerkChatComponent } from './pages/clerk-chat/clerk-chat.component';

@NgModule({
  declarations: [
    ClerkShellComponent,
    ClerkWorkspaceComponent,
    ClerkStockReceiveComponent,
    ClerkChatComponent,
  ],
  imports: [SharedModule, ClerkPortalRoutingModule],
})
export class ClerkPortalModule {}
