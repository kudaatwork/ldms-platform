import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ClerkShellComponent } from './components/clerk-shell/clerk-shell.component';
import { ClerkWorkspaceComponent } from './pages/clerk-workspace/clerk-workspace.component';
import { ClerkStockReceiveComponent } from './pages/clerk-stock-receive/clerk-stock-receive.component';
import { ClerkChatComponent } from './pages/clerk-chat/clerk-chat.component';
import { ClerkAuthGuard } from './guards/clerk-auth.guard';

const routes: Routes = [
  {
    path: '',
    component: ClerkShellComponent,
    canActivate: [ClerkAuthGuard],
    children: [
      { path: '', redirectTo: 'workspace', pathMatch: 'full' },
      {
        path: 'workspace',
        component: ClerkWorkspaceComponent,
        data: { title: 'Clerk workspace' },
      },
      {
        path: 'stock-receive/:tripId',
        component: ClerkStockReceiveComponent,
        data: { title: 'Receive stock' },
      },
      {
        path: 'chat/:tripId',
        component: ClerkChatComponent,
        data: { title: 'Message driver' },
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ClerkPortalRoutingModule {}
