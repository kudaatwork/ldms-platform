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
import { DriverTripRow, DeliveryActorRole, DeliveryWorkflowPhase, TripDeliveryWorkflowResponse } from '../../models/driver-portal.model';

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
  returnTo: 'driver' | 'trips' = 'driver';

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
    const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
    this.returnTo = returnTo === 'trips' ? 'trips' : 'driver';

    this.loadTrip(id);
    if (workflow) {
      this.showWorkflow = true;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get backLabel(): string {
    return this.returnTo === 'trips' ? 'Trips' : 'My trips';
  }

  get receiverActorRole(): DeliveryActorRole {
    return this.trip?.inventoryTransferId ? 'DEPOT_CLERK' : 'CUSTOMER';
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

  onWorkflowStateLoaded(res: TripDeliveryWorkflowResponse): void {
    const tripDto = res.tripDto;
    if (!tripDto?.id) {
      return;
    }
    const status = String(tripDto.status ?? '').toUpperCase();
    if (!this.trip) {
      this.trip = {
        id: tripDto.id,
        tripNumber: tripDto.tripNumber ?? `Trip ${tripDto.id}`,
        shipmentNumber: '',
        route: '—',
        cargoLabel: '—',
        productName: '',
        quantity: 0,
        unitOfMeasure: '',
        vehicleRegistration: '—',
        status,
        statusLabel: status.replace(/_/g, ' '),
        statusTone: 'info',
        startedAtLabel: '—',
        canTriggerArrival: status === 'IN_TRANSIT',
        canStartDeliveryWorkflow: status !== 'IN_TRANSIT' && status !== 'SCHEDULED',
        canLiveTrack: status === 'IN_TRANSIT' || status === 'RETURN_IN_TRANSIT',
        deliveryWorkflowPhase: resolveWorkflowPhaseFromStatus(status),
      };
      if (this.error) {
        this.error = '';
      }
      this.cdr.markForCheck();
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
    void this.router.navigate(['/driver/workspace'], { replaceUrl: true });
  }

  goBack(): void {
    if (this.returnTo === 'trips') {
      void this.router.navigate(['/shipments/trips']);
      return;
    }
    void this.router.navigate(['/driver/workspace']);
  }

  openLiveTrack(): void {
    const tripId = this.trip?.id ?? this.tripIdFromRoute;
    if (tripId) {
      void this.router.navigate(['/driver', 'live', tripId]);
    }
  }

  openChat(): void {
    const tripId = this.trip?.id ?? this.tripIdFromRoute;
    if (tripId) {
      void this.router.navigate(['/driver', 'chat', tripId]);
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

function resolveWorkflowPhaseFromStatus(status: string): DeliveryWorkflowPhase {
  switch (status) {
    case 'ARRIVED':
    case 'COUNTING_STOCK':
      return 'STOCK_COUNTING';
    case 'COUNT_COMPLETE':
      return 'SEND_OTP';
    case 'OTP_PENDING':
      return 'OTP_VERIFICATION';
    case 'DELIVERED':
      return 'START_RETURN';
    case 'RETURN_IN_TRANSIT':
      return 'RETURNS';
    case 'RETURNED':
      return 'COMPLETE';
    default:
      return 'ARRIVAL';
  }
}
