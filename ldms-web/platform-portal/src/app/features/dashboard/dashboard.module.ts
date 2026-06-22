import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { TripTrackingComponentsModule } from '../trip-tracking/trip-tracking-components.module';
import { DashboardRoutingModule } from './dashboard-routing.module';
import { DashboardComponent } from './pages/dashboard/dashboard.component';

@NgModule({
  declarations: [DashboardComponent],
  imports: [SharedModule, TripTrackingComponentsModule, DashboardRoutingModule],
})
export class DashboardModule {}
