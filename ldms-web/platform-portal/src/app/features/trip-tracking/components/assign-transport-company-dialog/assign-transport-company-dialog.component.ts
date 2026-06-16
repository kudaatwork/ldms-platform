import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { FleetPortalService } from '../../../fleet/services/fleet-portal.service';
import { TripTrackingPortalService } from '../../services/trip-tracking-portal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { SharedModule } from '../../../../shared/shared.module';
import type { TransporterPartnerRow } from '../../../fleet/models/fleet.model';
import type { ShipmentRow } from '../../models/trip-tracking.model';

export interface AssignTransportCompanyDialogData {
  shipment: ShipmentRow;
}

export type AssignTransportCompanyDialogResult = { action: 'assigned'; shipment: ShipmentRow };

type TransportProviderId = 'own' | number;

interface TransportProviderOption {
  id: TransportProviderId;
  name: string;
  subtitle: string;
  kind: 'own' | 'partner';
  searchText: string;
}

@Component({
  selector: 'app-assign-transport-company-dialog',
  templateUrl: './assign-transport-company-dialog.component.html',
  styleUrl: './assign-transport-company-dialog.component.scss',
  standalone: true,
  imports: [SharedModule],
})
export class AssignTransportCompanyDialogComponent implements OnInit, OnDestroy {
  transportProviders: TransportProviderOption[] = [];
  loading = false;
  submitting = false;
  selectedTransportProviderId: TransportProviderId | null = null;
  searchQuery = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly dialogRef: MatDialogRef<
      AssignTransportCompanyDialogComponent,
      AssignTransportCompanyDialogResult | undefined
    >,
    @Inject(MAT_DIALOG_DATA) public readonly data: AssignTransportCompanyDialogData,
    private readonly fleet: FleetPortalService,
    private readonly tripTracking: TripTrackingPortalService,
    private readonly notifications: NotificationService,
    private readonly authState: AuthStateService,
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.fleet
      .listPartners()
      .pipe(
        finalize(() => (this.loading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (partners) => {
          this.transportProviders = this.buildTransportProviderOptions(partners);
        },
        error: () => this.notifications.error('Failed to load contracted transporters.'),
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get canSubmit(): boolean {
    return this.selectedTransportProviderId !== null && !this.submitting && !this.loading;
  }

  get filteredTransportProviders(): TransportProviderOption[] {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) {
      return this.transportProviders;
    }
    return this.transportProviders.filter((option) => option.searchText.includes(q));
  }

  get selectedProviderLabel(): string {
    if (this.selectedTransportProviderId === null) {
      return '';
    }
    return this.transportProviders.find((option) => option.id === this.selectedTransportProviderId)?.name ?? '';
  }

  selectTransportProvider(providerId: TransportProviderId | string | null): void {
    if (providerId === null || providerId === undefined || providerId === '') {
      this.selectedTransportProviderId = null;
      return;
    }
    if (providerId === 'own') {
      this.selectedTransportProviderId = 'own';
      return;
    }
    const parsed = typeof providerId === 'number' ? providerId : Number(providerId);
    this.selectedTransportProviderId = Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  clearSelection(): void {
    this.selectedTransportProviderId = null;
  }

  confirm(): void {
    if (!this.canSubmit || this.selectedTransportProviderId === null) {
      return;
    }
    const transportCompanyOrganizationId =
      this.selectedTransportProviderId === 'own'
        ? Number(this.authState.currentUser?.organizationId ?? 0)
        : this.selectedTransportProviderId;
    if (!transportCompanyOrganizationId) {
      this.notifications.error('Could not resolve your organisation.');
      return;
    }

    this.submitting = true;
    this.tripTracking
      .assignTransportCompany({
        shipmentId: this.data.shipment.id,
        transportCompanyOrganizationId,
      })
      .pipe(
        finalize(() => (this.submitting = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (shipment) => {
          const provider = this.transportProviders.find((option) => option.id === this.selectedTransportProviderId);
          this.notifications.success(
            `${provider?.name ?? 'Transport company'} assigned. They will allocate a driver and vehicle.`,
          );
          this.dialogRef.close({ action: 'assigned', shipment });
        },
        error: (err: Error) => this.notifications.error(err.message || 'Could not assign transport company.'),
      });
  }

  close(): void {
    this.dialogRef.close();
  }

  private buildTransportProviderOptions(partners: TransporterPartnerRow[]): TransportProviderOption[] {
    const options: TransportProviderOption[] = [
      this.toOption('own', 'Own fleet', 'Your organisation will allocate drivers and vehicles', 'own'),
    ];
    for (const partner of partners) {
      options.push(
        this.toOption(
          partner.id,
          partner.name,
          partner.contractRangeLabel || partner.contractStatusLabel || 'Contracted transport company',
          'partner',
        ),
      );
    }
    return options;
  }

  private toOption(
    id: TransportProviderId,
    name: string,
    subtitle: string,
    kind: 'own' | 'partner',
  ): TransportProviderOption {
    return {
      id,
      name,
      subtitle,
      kind,
      searchText: `${name} ${subtitle} ${kind === 'own' ? 'own fleet internal' : 'contracted partner transporter'}`.toLowerCase(),
    };
  }
}
