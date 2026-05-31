import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HelpSupportAdminPageComponent } from './pages/help-support-admin-page/help-support-admin-page.component';

const routes: Routes = [
  {
    path: '',
    component: HelpSupportAdminPageComponent,
    data: { title: 'Help & Support', breadcrumb: 'Help & Support' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class HelpRoutingModule {}
