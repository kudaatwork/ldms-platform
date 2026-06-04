import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginAnalyticsPageComponent } from './pages/login-analytics-page/login-analytics-page.component';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'activity-logs',
  },
  {
    path: 'activity-logs',
    component: LoginAnalyticsPageComponent,
    data: { title: 'Login & Activity', breadcrumb: 'Login & Activity' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ActivityRoutingModule {}
