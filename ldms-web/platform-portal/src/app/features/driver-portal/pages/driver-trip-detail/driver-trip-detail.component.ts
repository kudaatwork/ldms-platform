import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { DriverPortalService } from '../../services/driver-portal.service';
import { DriverTripRow, DeliveryWorkflowPhase } from '../../models/driver-portal.model';

@Component({
  selector: 'app-driver-trip-detail',
  templateUrl: './driver-trip-detail.component.html',
  styleUrls: ['./driver-trip-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DriverTripDetailComponent implements OnInit, OnDestroy {
  trip: DriverTripRow | null = null;
  tripIdFromRoute = 0;
  loading = true;
  error = '';
  showWorkflow = false;
  workflowInitialPhase: DeliveryWorkflowPhase = 'ARRIVAL';
  arrivalSuggested = false;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly driverService: DriverPortalService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('tripId'));
    const workflow = this.route.snapshot.queryParamMap.get('workflow') === '1';

    if (!id) {
      void this.router.navigate(['/driver']);
      return;
    }

    this.tripIdFromRoute = id;
    const arrivalSuggested = this.route.snapshot.queryParamMap.get('arrivalSuggested') === '1';
    this.arrivalSuggested = arrivalSuggested;

    this.loadTrip(id);
    if (workflow) {
      this.showWorkflow = true;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadTrip(id: number, showLoading = true): void {
    if (showLoading) {
      this.loading = true;
    }
    this.driverService
      .getMyTripById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (t) => {
          this.trip = t;
          this.error = '';
          this.loading = false;
          if (t.deliveryWorkflowPhase) {
            this.workflowInitialPhase = t.deliveryWorkflowPhase;
          }
          this.cdr.markForCheck();
        },
        error: (e: Error) => {
          this.error = e.message;
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
  }

  onWorkflowPhaseChange(_phase: DeliveryWorkflowPhase): void {
    if (this.tripIdFromRoute) {
      this.loadTrip(this.tripIdFromRoute, false);
    }
  }

  openDeliveryWorkflow(): void {
    this.showWorkflow = true;
    this.cdr.markForCheck();
    setTimeout(() => {
      document.querySelector('.dtd-workflow')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 80);
  }

  onWorkflowComplete(): void {
    void this.router.navigate(['/driver'], { replaceUrl: true });
  }

  goBack(): void {
    void this.router.navigate(['/driver']);
  }

  openLiveTrack(): void {
    const tripId = this.trip?.id ?? this.tripIdFromRoute;
    if (tripId) {
      void this.router.navigate(['/shipments', 'live', tripId]);
    }
  }

  statusIcon(tone: DriverTripRow['statusTone']): string {
    const map: Record<string, string> = {
      success: 'check_circle',
      danger: 'error',
      warn: 'warning',
      info: 'info',
      muted: 'radio_button_unchecked',
    };
    return map[tone] ?? 'radio_button_unchecked';
  }
}
