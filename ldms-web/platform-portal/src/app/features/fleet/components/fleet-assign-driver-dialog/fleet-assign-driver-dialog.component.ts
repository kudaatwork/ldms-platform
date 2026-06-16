import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { Subject, finalize, forkJoin, of, takeUntil } from 'rxjs';
import type { DriverRosterSource, FleetDriverRow, FleetVehicleRow } from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';

export type FleetAssignDriverDialogData = {
  vehicle: FleetVehicleRow;
  drivers: FleetDriverRow[];
};

type DriverRosterGroup = {
  key: DriverRosterSource;
  label: string;
  hint: string;
  drivers: FleetDriverRow[];
};

@Component({
  selector: 'app-fleet-assign-driver-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatDialogModule, MatIconModule, MatRadioModule],
  templateUrl: './fleet-assign-driver-dialog.component.html',
  styleUrl: './fleet-assign-driver-dialog.component.scss',
})
export class FleetAssignDriverDialogComponent implements OnInit, OnDestroy {
  readonly vehicle: FleetVehicleRow;
  drivers: FleetDriverRow[] = [];
  driversLoading = false;
  driversError = '';
  searchQuery = '';
  selectedDriverId: number | 'none' | null = null;
  submitting = false;
  saveError = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly dialogRef: MatDialogRef<FleetAssignDriverDialogComponent, FleetVehicleRow | undefined>,
    private readonly fleet: FleetPortalService,
    @Optional() @Inject(MAT_DIALOG_DATA) data: FleetAssignDriverDialogData | null,
  ) {
    this.dialogRef.disableClose = true;
    this.vehicle = data?.vehicle as FleetVehicleRow;
    this.drivers = (data?.drivers ?? []).filter((row) => row.id > 0);
  }

  ngOnInit(): void {
    const isContracted = this.vehicle.ownershipType === 'contracted';
    const hasOrgCache = this.drivers.length > 0 && !isContracted;
    if (hasOrgCache) {
      this.prefillSelection();
      return;
    }
    this.loadDriverRoster();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get isContractedVehicle(): boolean {
    return this.vehicle.ownershipType === 'contracted';
  }

  get rosterSubtitle(): string {
    if (!this.isContractedVehicle) {
      return `Link a driver from your fleet roster to ${this.vehicle.registration} (${this.vehicle.makeModel}).`;
    }
    const partner = this.vehicle.contractedTransporterOrganizationName?.trim();
    return partner
      ? `Assign a driver from your organisation or from ${partner}'s pool to ${this.vehicle.registration}.`
      : `Assign a driver from your organisation or the contracted transporter's pool to ${this.vehicle.registration}.`;
  }

  get filteredDrivers(): FleetDriverRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) {
      return this.drivers;
    }
    return this.drivers.filter((driver) => {
      const haystack = [
        driver.fullName,
        driver.phoneNumber,
        driver.licenseNumber,
        driver.licenseClass,
        driver.employmentLabel,
        driver.homeOrganizationName ?? '',
        driver.rosterSource === 'transport_partner' ? 'transport partner' : 'organisation',
      ]
        .join(' ')
        .toLowerCase();
      return haystack.includes(q);
    });
  }

  get rosterGroups(): DriverRosterGroup[] {
    const orgDrivers = this.filteredDrivers.filter((row) => row.rosterSource !== 'transport_partner');
    const partnerDrivers = this.filteredDrivers.filter((row) => row.rosterSource === 'transport_partner');
    const groups: DriverRosterGroup[] = [];
    if (orgDrivers.length) {
      groups.push({
        key: 'organization',
        label: 'Your organisation',
        hint: 'Employed staff or drivers in your pool.',
        drivers: orgDrivers,
      });
    }
    if (partnerDrivers.length) {
      const partnerName = this.vehicle.contractedTransporterOrganizationName?.trim() || 'Transport partner';
      groups.push({
        key: 'transport_partner',
        label: partnerName,
        hint: 'Drivers supplied by the contracted transporter — pool drivers can be allocated to any contracted vehicle.',
        drivers: partnerDrivers,
      });
    }
    return groups;
  }

  get currentDriverLabel(): string {
    const name = String(this.vehicle.driverName ?? '').trim();
    return name && name !== '—' ? name : 'No driver assigned';
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  save(): void {
    this.saveError = '';
    if (this.selectedDriverId == null) {
      this.saveError = 'Select a driver from the roster, or choose Unassign.';
      return;
    }

    const driver =
      this.selectedDriverId === 'none'
        ? null
        : this.drivers.find((row) => row.id === this.selectedDriverId) ?? null;

    if (this.selectedDriverId !== 'none' && !driver) {
      this.saveError = 'Select a valid driver from the roster.';
      return;
    }

    this.submitting = true;
    this.fleet
      .assignDriverToVehicle(this.vehicle, driver)
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row) => this.dialogRef.close(row),
        error: (err: Error) => {
          this.saveError = err.message ?? 'Could not update driver assignment.';
        },
      });
  }

  employmentBadgeClass(driver: FleetDriverRow): string {
    return driver.employmentType === 'POOL' ? 'fad-assign__badge--pool' : 'fad-assign__badge--employed';
  }

  private loadDriverRoster(): void {
    this.driversLoading = true;
    this.driversError = '';

    const orgDrivers$ = this.fleet.listDrivers();
    const transporterId = this.vehicle.contractedTransporterOrganizationId;
    const partnerDrivers$ =
      this.isContractedVehicle && transporterId != null && transporterId > 0
        ? this.fleet.listTransporterPartnerDrivers(transporterId)
        : of([] as FleetDriverRow[]);

    forkJoin({ orgDrivers: orgDrivers$, partnerDrivers: partnerDrivers$ })
      .pipe(
        finalize(() => (this.driversLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: ({ orgDrivers, partnerDrivers }) => {
          const merged = [...orgDrivers, ...partnerDrivers].filter((row) => row.id > 0);
          const seen = new Set<number>();
          this.drivers = merged.filter((row) => {
            if (seen.has(row.id)) {
              return false;
            }
            seen.add(row.id);
            return true;
          });
          if (this.isContractedVehicle && !transporterId) {
            this.driversError =
              'This contracted vehicle has no transporter link — edit the vehicle and select a transport partner to load partner drivers.';
          } else if (!this.drivers.length) {
            this.driversError = 'No drivers found. Add drivers under the Drivers tab or ask your transport partner to register their pool.';
          }
          this.prefillSelection();
        },
        error: (err: Error) => {
          this.drivers = [];
          this.driversError = err.message ?? 'Could not load drivers.';
        },
      });
  }

  private prefillSelection(): void {
    const fleetDriverId = this.vehicle.fleetDriverId;
    if (fleetDriverId != null && fleetDriverId > 0) {
      const byId = this.drivers.find((row) => row.id === fleetDriverId);
      if (byId) {
        this.selectedDriverId = byId.id;
        return;
      }
    }

    const current = String(this.vehicle.driverName ?? '').trim().toLowerCase();
    if (!current || current === '—' || current === '-') {
      this.selectedDriverId = null;
      return;
    }
    const match = this.drivers.find((row) => row.fullName.trim().toLowerCase() === current);
    this.selectedDriverId = match?.id ?? null;
  }
}
