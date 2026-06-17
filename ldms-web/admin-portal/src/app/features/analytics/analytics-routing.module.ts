import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AnalyticsOverviewPageComponent } from './pages/analytics-overview-page/analytics-overview-page.component';
import { CompanyShipmentsPageComponent } from './pages/company-shipments-page/company-shipments-page.component';
import { ShipmentLivePageComponent } from './pages/shipment-live-page/shipment-live-page.component';
import { RevenueReportPageComponent } from './pages/revenue-report-page/revenue-report-page.component';

const routes: Routes = [
  {
    path: '',
    component: AnalyticsOverviewPageComponent,
    data: { title: 'Shipment analytics', breadcrumb: 'Analytics' },
  },
  {
    path: 'revenue',
    component: RevenueReportPageComponent,
    data: { title: 'Platform revenue', breadcrumb: 'Revenue' },
  },
  {
    path: 'companies/:orgId',
    component: CompanyShipmentsPageComponent,
    data: { title: 'Company shipments', breadcrumb: 'Company' },
  },
  {
    path: 'companies/:orgId/shipments/:shipmentId',
    component: ShipmentLivePageComponent,
    data: { title: 'Shipment live', breadcrumb: 'Live' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AnalyticsRoutingModule {}
