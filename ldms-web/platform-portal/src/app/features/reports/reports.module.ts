import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { UsageChargeReportPageComponent } from './pages/usage-charge-report-page/usage-charge-report-page.component';
import { TripJourneyReportPageComponent } from './pages/trip-journey-report-page/trip-journey-report-page.component';
import { AnalyticsSubnavComponent } from './components/analytics-subnav/analytics-subnav.component';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'platform-usage' },
  {
    path: 'platform-usage',
    component: UsageChargeReportPageComponent,
    data: { title: 'Platform usage & changes', breadcrumb: 'Platform usage & changes' },
  },
  {
    path: 'trips',
    component: TripJourneyReportPageComponent,
    data: { title: 'Trip analytics', breadcrumb: 'Trips' },
  },
  { path: 'usage-charges', redirectTo: 'platform-usage', pathMatch: 'full' },
  { path: 'trip-journeys', redirectTo: 'trips', pathMatch: 'full' },
];

@NgModule({
  declarations: [UsageChargeReportPageComponent, TripJourneyReportPageComponent, AnalyticsSubnavComponent],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class ReportsModule {}
