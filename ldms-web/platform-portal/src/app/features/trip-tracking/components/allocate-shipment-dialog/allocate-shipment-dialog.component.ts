import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { FleetPortalService } from '../../../fleet/services/fleet-portal.service';
import { TripTrackingPortalService } from '../../services/trip-tracking-portal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import type { FleetDriverRow, FleetVehicleRow } from '../../../fleet/models/fleet.model';
import type { ShipmentRow } from '../../models/trip-tracking.model';

export interface AllocateShipmentDialogData {
  shipment: ShipmentRow;
}

export type AllocateShipmentDialogResult = { action: 'allocated'; shipment: ShipmentRow };

@Component({
  selector: 'app-allocate-shipment-dialog',
  templateUrl: './allocate-shipment-dialog.component.html',
  styleUrl: './allocate-shipment-dialog.component.scss',
  standalone: false,
})
export class AllocateShipmentDialogComponent implements OnInit, OnDestroy {
  drivers: FleetDriverRow[] = [];
  vehicles: FleetVehicleRow[] = [];
  driversLoading = true;
  vehiclesLoading = true;
  submitting = false;

  selectedDriverId: number | null = null;
  selectedVehicleId: number | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly dialogRef: MatDialogRef<AllocateShipmentDialogComponent, AllocateShipmentDialogResult | undefined>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AllocateShipmentDialogData,
    private readonly fleet: FleetPortalService,
    private readonly tripTracking: TripTrackingPortalService,
    private readonly notifications: NotificationService,
  ) {}

  ngOnInit(): void {
    this.fleet
      .listDrivers()
      .pipe(
        finalize(() => (this.driversLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => (this.drivers = rows),
        error: () => this.notifications.error('Failed to load drivers.'),
      });

    this.fleet
      .listOwnFleet()
      .pipe(
        finalize(() => (this.vehiclesLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => (this.vehicles = rows.filter((v) => v.ownershipType === 'owned')),
        error: () => this.notifications.error('Failed to load fleet vehicles.'),
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get canSubmit(): boolean {
    return !!this.selectedDriverId && !!this.selectedVehicleId && !this.submitting;
  }

  get loading(): boolean {
    return this.driversLoading || this.vehiclesLoading;
  }

  confirm(): void {
    if (!this.canSubmit) {
      return;
    }
    this.submitting = true;
    this.tripTracking
      .allocateShipment({
        shipmentId: this.data.shipment.id,
        fleetDriverId: this.selectedDriverId!,
        fleetAssetId: this.selectedVehicleId!,
      })
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (shipment) => {
          this.notifications.success('Driver and vehicle allocated successfully.');
          this.dialogRef.close({ action: 'allocated', shipment });
        },
        error: (err: Error) => this.notifications.error(err.message || 'Allocation failed.'),
      });
  }

  close(): void {
    this.dialogRef.close();
  }
}
