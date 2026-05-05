import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ChurnoutHistoryPageComponent } from './pages/churnout-history-page/churnout-history-page.component';
import { ActivityPageComponent } from './pages/activity-page/activity-page.component';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'request-logs',
  },
  {
    path: 'request-logs',
    component: ActivityPageComponent,
    data: { title: 'Request Logs', breadcrumb: 'Request Logs', activityView: 'request' },
  },
  {
    path: 'activity-logs',
    component: ActivityPageComponent,
    data: { title: 'Activity Logs', breadcrumb: 'Activity Logs', activityView: 'activity' },
  },
  {
    path: 'churnout-history',
    component: ChurnoutHistoryPageComponent,
    data: { title: 'Churnout History', breadcrumb: 'Churnout History' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ActivityRoutingModule {}
