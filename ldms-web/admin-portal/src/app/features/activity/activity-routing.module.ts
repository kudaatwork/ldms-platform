import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ActivityPageComponent } from './pages/activity-page/activity-page.component';

const routes: Routes = [
  {
    path: '',
    component: ActivityPageComponent,
    data: { title: 'Activity Log', breadcrumb: 'Activity Log' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ActivityRoutingModule {}
