import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { TripTrackingPortalService } from '../../services/trip-tracking-portal.service';
import type { TripDetail, TripRow } from '../../models/trip-tracking.model';

export interface ViewTripDialogData {
  trip: TripRow;
}

export type ViewTripDialogResult =
  | { action: 'arrival_triggered' }
  | { action: 'otp_verified' };

@Component({
  selector: 'app-view-trip-dialog',
  templateUrl: './view-trip-dialog.component.html',
  styleUrl: './view-trip-dialog.component.scss',
  standalone: false,
})
export class ViewTripDialogComponent implements OnInit, OnDestroy {
  detail: TripDetail | null = null;
  detailLoading = true;
  detailError = '';

  otpMode = false;
  otp = '';
  otpError = '';
  actionBusy = false;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly dialogRef: MatDialogRef<ViewTripDialogComponent, ViewTripDialogResult | undefined>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ViewTripDialogData,
    private readonly tripTracking: TripTrackingPortalService,
    private readonly authState: AuthStateService,
    private readonly notifications: NotificationService,
    private readonly router: Router,
  ) {}

  openLiveMap(): void {
    this.dialogRef.close();
    void this.router.navigate(['/shipments/live', this.data.trip.id]);
  }

  ngOnInit(): void {
    this.loadDetail();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadDetail(): void {
    this.detailLoading = true;
    this.detailError = '';
    this.tripTracking
      .trackTrip(this.data.trip.id)
      .pipe(
        finalize(() => (this.detailLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (detail) => (this.detail = detail),
        error: (err: Error) => (this.detailError = err.message || 'Failed to load trip details.'),
      });
  }

  get canTriggerArrival(): boolean {
    return !!(this.detail?.canTriggerArrival) && !this.otpMode;
  }

  get canVerifyOtp(): boolean {
    return !!(this.detail?.canVerifyOtp) && !this.otpMode;
  }

  triggerArrival(): void {
    const userId = Number(this.authState.currentUser?.userId ?? 0);
    if (!userId) {
      this.notifications.error('Cannot determine current user.');
      return;
    }
    this.actionBusy = true;
    this.tripTracking
      .triggerArrival({ tripId: this.data.trip.id, driverUserId: userId })
      .pipe(
        finalize(() => (this.actionBusy = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Arrival confirmed.');
          this.dialogRef.close({ action: 'arrival_triggered' });
        },
        error: (err: Error) => this.notifications.error(err.message || 'Could not trigger arrival.'),
      });
  }

  startOtpEntry(): void {
    this.otpMode = true;
    this.otp = '';
    this.otpError = '';
  }

  cancelOtp(): void {
    this.otpMode = false;
    this.otp = '';
    this.otpError = '';
  }

  verifyOtp(): void {
    const code = this.otp.trim();
    if (code.length < 4) {
      this.otpError = 'Enter the 4-6 digit OTP provided by the receiver.';
      return;
    }
    const userId = Number(this.authState.currentUser?.userId ?? 0);
    this.actionBusy = true;
    this.tripTracking
      .verifyDeliveryOtp({ tripId: this.data.trip.id, otp: code, receiverUserId: userId })
      .pipe(
        finalize(() => (this.actionBusy = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Delivery verified successfully.');
          this.dialogRef.close({ action: 'otp_verified' });
        },
        error: (err: Error) => {
          this.otpError = err.message || 'OTP verification failed.';
        },
      });
  }

  close(): void {
    this.dialogRef.close();
  }

  eventIcon(eventType: string): string {
    const icons: Record<string, string> = {
      DEPARTURE: 'departure_board',
      DEPARTED: 'departure_board',
      CHECKPOINT: 'where_to_vote',
      ARRIVED_AT_BORDER: 'flag',
      BORDER_CLEARED: 'verified',
      ROADSIDE_FUEL_STOP: 'local_gas_station',
      ROADSIDE_MECHANIC_STOP: 'build',
      ROADSIDE_RESUMED: 'play_arrow',
      BREAK: 'coffee',
      DELAY: 'hourglass_top',
      ARRIVAL: 'place',
      ARRIVED: 'place',
      DELIVERED: 'check_circle',
      INCIDENT: 'warning',
      OTHER: 'fiber_manual_record',
      NOTE: 'fiber_manual_record',
    };
    return icons[eventType] ?? 'fiber_manual_record';
  }

  statusClass(tone: string): string {
    const map: Record<string, string> = {
      muted: 'trt-badge--muted',
      warn: 'trt-badge--warn',
      success: 'trt-badge--success',
      danger: 'trt-badge--danger',
      info: 'trt-badge--info',
    };
    return map[tone] ?? 'trt-badge--muted';
  }
}
