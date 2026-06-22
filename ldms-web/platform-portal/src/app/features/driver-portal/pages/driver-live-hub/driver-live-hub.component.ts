import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { DriverPortalService } from '../../services/driver-portal.service';
import { DriverTripRow } from '../../models/driver-portal.model';

const LIVE_TRACK_STATUSES = new Set([
  'IN_TRANSIT',
  'AT_BORDER_HOLD',
  'ROADSIDE_HOLD',
  'ARRIVED',
  'COUNTING_STOCK',
  'COUNT_COMPLETE',
  'OTP_PENDING',
  'RETURN_IN_TRANSIT',
]);

@Component({
  selector: 'app-driver-live-hub',
  templateUrl: './driver-live-hub.component.html',
  styleUrls: ['./driver-live-hub.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DriverLiveHubComponent implements OnInit, OnDestroy {
  loading = true;
  empty = false;
  error = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly driverService: DriverPortalService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.driverService
      .getMyTrips()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (trips) => {
          const target = this.pickLiveTrip(trips);
          if (target) {
            void this.router.navigate(['/driver', 'live', target.id], { replaceUrl: true });
            return;
          }
          this.loading = false;
          this.empty = true;
          this.cdr.markForCheck();
        },
        error: (e: Error) => {
          this.loading = false;
          this.error = e.message;
          this.cdr.markForCheck();
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  goToTrips(): void {
    void this.router.navigate(['/driver/workspace']);
  }

  private pickLiveTrip(trips: DriverTripRow[]): DriverTripRow | undefined {
    return (
      trips.find((t) => t.canLiveTrack) ??
      trips.find((t) => LIVE_TRACK_STATUSES.has((t.status ?? '').toUpperCase()))
    );
  }
}
