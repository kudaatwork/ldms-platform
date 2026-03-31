import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SystemHealthComponent } from './pages/system-health/system-health.component';
import { SystemMonitoringComponent } from './pages/system-monitoring/system-monitoring.component';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'health' },
  {
    path: 'health',
    component: SystemHealthComponent,
    data: { title: 'System Health', breadcrumb: 'Health' },
  },
  {
    path: 'monitoring',
    component: SystemMonitoringComponent,
    data: { title: 'Monitoring', breadcrumb: 'Monitoring' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class SystemRoutingModule {}
