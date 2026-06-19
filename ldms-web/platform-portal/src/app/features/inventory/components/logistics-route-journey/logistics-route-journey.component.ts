import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import type { LogisticsRouteStopRow } from '../../models/inventory.model';

@Component({
  selector: 'app-logistics-route-journey',
  templateUrl: './logistics-route-journey.component.html',
  styleUrl: './logistics-route-journey.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LogisticsRouteJourneyComponent {
  @Input() originLabel = '';
  @Input() destinationLabel = '';
  @Input() stops: LogisticsRouteStopRow[] = [];
  @Input() compact = false;

  get journeyStops(): LogisticsRouteStopRow[] {
    if (this.stops?.length) {
      return [...this.stops].sort((a, b) => a.stopSequence - b.stopSequence);
    }
    const built: LogisticsRouteStopRow[] = [];
    if (this.originLabel) {
      built.push({ stopSequence: 0, stopType: 'ORIGIN', locationLabel: this.originLabel });
    }
    if (this.destinationLabel) {
      built.push({ stopSequence: 1, stopType: 'DESTINATION', locationLabel: this.destinationLabel });
    }
    return built;
  }

  stopIcon(type: string): string {
    switch (type) {
      case 'ORIGIN':
        return 'trip_origin';
      case 'DESTINATION':
        return 'flag';
      default:
        return 'warehouse';
    }
  }

  stopTitle(stop: LogisticsRouteStopRow): string {
    return (
      stop.locationLabel?.trim() ||
      stop.warehouseName?.trim() ||
      (stop.stopType === 'ORIGIN' ? 'Origin' : stop.stopType === 'DESTINATION' ? 'Destination' : 'En-route depot')
    );
  }

  stopSubtitle(stop: LogisticsRouteStopRow): string {
    switch (stop.stopType) {
      case 'ORIGIN':
        return 'Pickup / dispatch';
      case 'DESTINATION':
        return 'Final delivery';
      case 'EN_ROUTE_DEPOT':
        return 'Intermediate depot';
      default:
        return '';
    }
  }
}
