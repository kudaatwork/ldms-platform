import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HelpSupportPageComponent } from './pages/help-support-page/help-support-page.component';

const routes: Routes = [
  {
    path: '',
    component: HelpSupportPageComponent,
    data: { title: 'Help & Support', breadcrumb: 'Help & Support' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class HelpSupportRoutingModule {}
