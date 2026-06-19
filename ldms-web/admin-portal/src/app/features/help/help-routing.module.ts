import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HelpShellComponent } from './components/help-shell/help-shell.component';
import { HelpSupportAdminPageComponent } from './pages/help-support-admin-page/help-support-admin-page.component';
import { DemoRequisitionsAdminPageComponent } from './pages/demo-requisitions-admin-page/demo-requisitions-admin-page.component';
import { BotServiceAdminPageComponent } from './pages/bot-service-admin-page/bot-service-admin-page.component';
import { BotAnalyticsAdminPageComponent } from './pages/bot-analytics-admin-page/bot-analytics-admin-page.component';
import { BotFaqAdminPageComponent } from './pages/bot-faq-admin-page/bot-faq-admin-page.component';

const routes: Routes = [
  {
    path: '',
    component: HelpShellComponent,
    children: [
      { path: '', redirectTo: 'live-chat', pathMatch: 'full' },
      {
        path: 'live-chat',
        component: HelpSupportAdminPageComponent,
        data: { title: 'Live chat', breadcrumb: 'Live chat' },
      },
      { path: 'tickets', redirectTo: 'live-chat', pathMatch: 'full' },
      {
        path: 'requisitions',
        component: DemoRequisitionsAdminPageComponent,
        data: { title: 'Demo requisitions', breadcrumb: 'Demo requisitions' },
      },
      {
        path: 'bot-service',
        component: BotServiceAdminPageComponent,
        data: { title: 'Bot service', breadcrumb: 'Bot service' },
      },
      {
        path: 'bot-analytics',
        component: BotAnalyticsAdminPageComponent,
        data: { title: 'Bot analytics', breadcrumb: 'Bot analytics' },
      },
      {
        path: 'bot-knowledge',
        component: BotFaqAdminPageComponent,
        data: { title: 'Bot knowledge', breadcrumb: 'Bot knowledge' },
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class HelpRoutingModule {}
