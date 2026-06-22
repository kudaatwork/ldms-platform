import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { TripLiveMapPanelComponent } from './components/trip-live-map-panel/trip-live-map-panel.component';
import { LiveTripTrackingComponent } from './pages/live-trip-tracking/live-trip-tracking.component';

/** Shared trip-tracking UI without routing — safe to import from dashboard and driver portal. */
@NgModule({
  declarations: [TripLiveMapPanelComponent, LiveTripTrackingComponent],
  imports: [SharedModule],
  exports: [TripLiveMapPanelComponent, LiveTripTrackingComponent],
})
export class TripTrackingComponentsModule {}
