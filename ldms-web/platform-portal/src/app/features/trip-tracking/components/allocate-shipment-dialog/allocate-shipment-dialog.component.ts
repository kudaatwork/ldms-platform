import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { FleetPortalService } from '../../../fleet/services/fleet-portal.service';
import { TripTrackingPortalService } from '../../services/trip-tracking-portal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import type {
  FleetDriverRow,
  FleetVehicleRow,
  FleetVehicleType,
} from '../../../fleet/models/fleet.model';
import type { ShipmentRow } from '../../models/trip-tracking.model';

export interface AllocateShipmentDialogData {
  shipment: ShipmentRow;
}

export type AllocateShipmentDialogResult = { action: 'allocated'; shipment: ShipmentRow };

type VehicleCategoryFilter = 'all' | 'owned' | 'contracted' | FleetVehicleType;

const VEHICLE_TYPE_LABELS: Record<FleetVehicleType, string> = {
  rig: 'Rig / truck',
  van: 'Van',
  tanker: 'Tanker',
  flatbed: 'Flatbed',
};

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

  driverSearch = '';
  vehicleSearch = '';
  vehicleCategory: VehicleCategoryFilter = 'all';

  selectedDriverId: number | null = null;
  selectedVehicleId: number | string | null = null;

  readonly vehicleCategories: { id: VehicleCategoryFilter; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'owned', label: 'Owned' },
    { id: 'contracted', label: 'Contracted' },
    { id: 'rig', label: 'Rigs' },
    { id: 'van', label: 'Vans' },
    { id: 'tanker', label: 'Tankers' },
    { id: 'flatbed', label: 'Flatbeds' },
  ];

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
        next: (rows) => (this.vehicles = rows),
        error: () => this.notifications.error('Failed to load fleet vehicles.'),
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredDrivers(): FleetDriverRow[] {
    const q = this.driverSearch.trim().toLowerCase();
    if (!q) {
      return this.drivers;
    }
    return this.drivers.filter((driver) => {
      const hay = `${driver.fullName} ${driver.licenseClass} ${driver.phoneNumber ?? ''}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get filteredVehicles(): FleetVehicleRow[] {
    const q = this.vehicleSearch.trim().toLowerCase();
    const category = this.vehicleCategory;
    return this.vehicles.filter((vehicle) => {
      if (category !== 'all') {
        if (category === 'owned' || category === 'contracted') {
          if (vehicle.ownershipType !== category) {
            return false;
          }
        } else if (vehicle.type !== category) {
          return false;
        }
      }
      if (!q) {
        return true;
      }
      const hay = [
        vehicle.registration,
        vehicle.makeModel,
        VEHICLE_TYPE_LABELS[vehicle.type],
        vehicle.type,
        vehicle.statusLabel,
        vehicle.ownershipLabel,
        vehicle.driverName,
        vehicle.contractedTransporterOrganizationName ?? '',
      ]
        .join(' ')
        .toLowerCase();
      return hay.includes(q);
    });
  }

  get canSubmit(): boolean {
    return !!this.selectedDriverId && this.selectedVehicleId != null && !this.submitting;
  }

  get loading(): boolean {
    return this.driversLoading || this.vehiclesLoading;
  }

  vehicleIcon(type: FleetVehicleRow['type']): string {
    const map: Record<FleetVehicleRow['type'], string> = {
      rig: 'local_shipping',
      van: 'airport_shuttle',
      tanker: 'propane_tank',
      flatbed: 'rv_hookup',
    };
    return map[type] ?? 'local_shipping';
  }

  setVehicleCategory(category: VehicleCategoryFilter): void {
    this.vehicleCategory = category;
    if (
      this.selectedVehicleId != null &&
      !this.filteredVehicles.some((vehicle) => vehicle.id === this.selectedVehicleId)
    ) {
      this.selectedVehicleId = null;
    }
  }

  selectVehicle(vehicle: FleetVehicleRow): void {
    this.selectedVehicleId = vehicle.id;
    const assignedDriverId = vehicle.fleetDriverId;
    if (assignedDriverId != null && assignedDriverId > 0) {
      const driverExists = this.drivers.some((driver) => driver.id === assignedDriverId);
      if (driverExists) {
        this.selectedDriverId = assignedDriverId;
      }
    }
  }

  clearVehicleFilters(): void {
    this.vehicleSearch = '';
    this.vehicleCategory = 'all';
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
        fleetAssetId: Number(this.selectedVehicleId),
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
