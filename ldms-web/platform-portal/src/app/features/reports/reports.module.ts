import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { UsageChargeReportPageComponent } from './pages/usage-charge-report-page/usage-charge-report-page.component';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'usage-charges' },
  {
    path: 'usage-charges',
    component: UsageChargeReportPageComponent,
    data: { title: 'Usage charges', breadcrumb: 'Usage charges' },
  },
];

@NgModule({
  declarations: [UsageChargeReportPageComponent],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class ReportsModule {}
