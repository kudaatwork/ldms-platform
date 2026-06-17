import { NgModule } from '@angular/core';
import { NgChartsModule } from 'ng2-charts';
import { SharedModule } from '../../shared/shared.module';
import { AnalyticsRoutingModule } from './analytics-routing.module';
import { AnalyticsOverviewPageComponent } from './pages/analytics-overview-page/analytics-overview-page.component';
import { CompanyShipmentsPageComponent } from './pages/company-shipments-page/company-shipments-page.component';
import { ShipmentLivePageComponent } from './pages/shipment-live-page/shipment-live-page.component';
import { RevenueReportPageComponent } from './pages/revenue-report-page/revenue-report-page.component';

@NgModule({
  declarations: [
    AnalyticsOverviewPageComponent,
    CompanyShipmentsPageComponent,
    ShipmentLivePageComponent,
    RevenueReportPageComponent,
  ],
  imports: [SharedModule, NgChartsModule, AnalyticsRoutingModule],
})
export class AnalyticsModule {}
