import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Subject, debounceTime } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { RegisterTransporterDialogComponent } from '../../components/register-transporter-dialog/register-transporter-dialog.component';
import type { RegisterTransporterDialogData } from '../../components/register-transporter-dialog/register-transporter-dialog.component';
import { OwnFleetDialogComponent } from '../../components/own-fleet-dialog/own-fleet-dialog.component';
import type { OwnFleetDialogData } from '../../components/own-fleet-dialog/own-fleet-dialog.component';
import { FleetDriverDialogComponent } from '../../components/fleet-driver-dialog/fleet-driver-dialog.component';
import type { FleetDriverDialogData } from '../../components/fleet-driver-dialog/fleet-driver-dialog.component';
import { FleetComplianceDialogComponent } from '../../components/fleet-compliance-dialog/fleet-compliance-dialog.component';
import type { FleetComplianceDialogData } from '../../components/fleet-compliance-dialog/fleet-compliance-dialog.component';
import type { OrganizationPartnerMetadata } from '../../../../shared/models/organization-metadata.model';
import {
  FleetComplianceRow,
  FleetDriverRow,
  FleetVehicleRow,
  FleetWorkspaceMetrics,
  FleetWorkspaceTab,
  TransporterPartnerRow,
} from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';

@Component({
  selector: 'app-fleet-workspace',
  templateUrl: './fleet-workspace.component.html',
  styleUrl: './fleet-workspace.component.scss',
  standalone: false,
})
export class FleetWorkspaceComponent implements OnInit, OnDestroy {
  fetching = true;
  loadError = '';
  activeTab: FleetWorkspaceTab = 'overview';
  partnerSearch = '';
  selectedVehicleId: number | string | null = null;
  selectedPartnerId: number | null = null;
  partnerMetadata: OrganizationPartnerMetadata | null = null;
  partnerMetadataLoading = false;
  partnerMetadataError = '';

  ownFleet: FleetVehicleRow[] = [];
  partnerFleet: FleetVehicleRow[] = [];
  partnerFleetLoading = false;
  partnerFleetError = '';
  drivers: FleetDriverRow[] = [];
  driversLoading = false;
  driversError = '';
  compliance: FleetComplianceRow[] = [];
  expiringCompliance: FleetComplianceRow[] = [];
  complianceLoading = false;
  complianceError = '';
  complianceFilter: 'all' | 'expiring' = 'all';
  private partnerMetadataRequestId = 0;
  partners: TransporterPartnerRow[] = [];

  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly title: Title,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly fleet: FleetPortalService,
    private readonly dialog: MatDialog,
    private readonly notifications: NotificationService,
    private readonly authState: AuthStateService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Fleet & Transporters | LX Platform');
    this.route.data.pipe(takeUntil(this.destroy$)).subscribe((data) => {
      const tab = data['tab'] as FleetWorkspaceTab | undefined;
      if (tab) {
        this.activeTab = tab;
        this.ensureTabDataLoaded(tab);
      }
    });
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const tab = params.get('tab') as FleetWorkspaceTab | null;
      if (tab && this.isFleetTab(tab)) {
        this.activeTab = tab;
        this.ensureTabDataLoaded(tab);
      }
    });
    this.reload$.pipe(debounceTime(120), takeUntil(this.destroy$)).subscribe(() => this.loadWorkspace());
    this.reload$.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get orgName(): string {
    return this.authState.currentUser?.orgName ?? 'Your organisation';
  }

  get classification(): string {
    return this.authState.currentUser?.orgClassification ?? 'SUPPLIER';
  }

  get isSupplier(): boolean {
    return this.classification === 'SUPPLIER';
  }

  get isTransportCompany(): boolean {
    return this.classification === 'TRANSPORT_COMPANY';
  }

  get canLinkTransporter(): boolean {
    return this.isSupplier;
  }

  get partnersSectionTitle(): string {
    return this.isTransportCompany ? 'Contracting shippers' : 'Contracted transporters';
  }

  get partnersSectionLead(): string {
    return this.isTransportCompany
      ? 'Shipper organisations that have contracted your transport company, with commercial dates on each link.'
      : 'Third-party transport companies under contract — distinct from vehicles you own and operate directly.';
  }

  get convoyTitle(): string {
    return this.isTransportCompany ? 'Own rolling stock' : 'Own fleet';
  }

  get convoySectionLead(): string {
    return this.isTransportCompany
      ? 'Assets your transport company owns and operates — not subcontracted corridor partners.'
      : 'Vehicles owned and operated by your organisation. Contracted transporters are managed separately.';
  }

  get contractedPartnersLabel(): string {
    return this.isTransportCompany ? 'Contracting shippers' : 'Contracted transporters';
  }

  get heroLead(): string {
    if (this.isTransportCompany) {
      return `Command centre for ${this.orgName} — utilisation, corridor readiness, and shipper relationships in one sweep.`;
    }
    return `Corridor control for ${this.orgName} — your owned assets and contracted transporters on one live board.`;
  }

  get filteredPartners(): TransporterPartnerRow[] {
    const q = this.partnerSearch.trim().toLowerCase();
    if (!q) {
      return this.partners;
    }
    return this.partners.filter((p) => {
      const hay = `${p.name} ${p.email} ${p.phoneNumber}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get metrics(): FleetWorkspaceMetrics {
    const fleet = this.ownFleet;
    const ownedFleet = fleet.filter((v) => v.ownershipType !== 'contracted');
    const contractedFleet = fleet.filter((v) => v.ownershipType === 'contracted');
    const onRoad = fleet.filter((v) => v.status === 'on_road').length;
    const available = fleet.filter((v) => v.status === 'available' || v.status === 'yard').length;
    const avgUtilization = fleet.length
      ? Math.round(fleet.reduce((sum, v) => sum + v.utilizationPct, 0) / fleet.length)
      : 0;
    const partnersVerified = this.partners.filter((p) => p.verified).length;
    return {
      ownFleetTotal: fleet.length,
      ownedFleetTotal: ownedFleet.length,
      contractedFleetTotal: contractedFleet.length,
      onRoad,
      available,
      avgUtilization,
      partnersTotal: this.partners.length,
      partnersVerified,
      driversTotal: this.drivers.length,
      expiringComplianceTotal: this.expiringCompliance.length,
    };
  }

  get visibleCompliance(): FleetComplianceRow[] {
    return this.complianceFilter === 'expiring' ? this.expiringCompliance : this.compliance;
  }

  get selectedVehicle(): FleetVehicleRow | null {
    if (this.selectedVehicleId == null) {
      return null;
    }
    return this.ownFleet.find((v) => v.id === this.selectedVehicleId) ?? null;
  }

  get selectedPartner(): TransporterPartnerRow | null {
    if (this.selectedPartnerId == null) {
      return null;
    }
    return this.filteredPartners.find((p) => p.id === this.selectedPartnerId) ?? null;
  }

  refresh(): void {
    this.reload$.next();
  }

  setTab(tab: FleetWorkspaceTab): void {
    this.activeTab = tab;
    this.ensureTabDataLoaded(tab);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  setComplianceFilter(filter: 'all' | 'expiring'): void {
    this.complianceFilter = filter;
    if (filter === 'expiring' && !this.expiringCompliance.length && !this.complianceLoading) {
      this.loadExpiringCompliance();
    }
  }

  selectVehicle(row: FleetVehicleRow): void {
    this.selectedVehicleId = row.id === this.selectedVehicleId ? null : row.id;
    this.selectedPartnerId = null;
    this.clearPartnerMetadata();
  }

  selectPartner(row: TransporterPartnerRow): void {
    const nextId = row.id === this.selectedPartnerId ? null : row.id;
    this.selectedPartnerId = nextId;
    this.selectedVehicleId = null;
    this.loadPartnerMetadata(nextId);
  }

  closePartnerSpotlight(): void {
    this.selectedPartnerId = null;
    this.clearPartnerMetadata();
  }

  // ── Own fleet CRUD ─────────────────────────────────────────────────────────

  openAddVehicle(): void {
    this.dialog
      .open(OwnFleetDialogComponent, {
        width: '540px',
        maxWidth: '95vw',
        disableClose: true,
        data: {
          isSupplier: this.isSupplier,
          transporterOptions: this.partners,
        } satisfies OwnFleetDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((vehicle: FleetVehicleRow | undefined) => {
        if (vehicle) {
          this.notifications.success(`Vehicle ${vehicle.registration} was added to your fleet.`);
          this.reload$.next();
        }
      });
  }

  openEditVehicle(vehicle: FleetVehicleRow, event?: Event): void {
    event?.stopPropagation();
    this.dialog
      .open(OwnFleetDialogComponent, {
        width: '540px',
        maxWidth: '95vw',
        disableClose: true,
        data: {
          vehicle,
          isSupplier: this.isSupplier,
          transporterOptions: this.partners,
        } satisfies OwnFleetDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((updated: FleetVehicleRow | undefined) => {
        if (updated) {
          this.notifications.success(`${updated.registration} was updated.`);
          this.reload$.next();
        }
      });
  }

  confirmDeleteVehicle(vehicle: FleetVehicleRow, event?: Event): void {
    event?.stopPropagation();
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: { entityLabel: `vehicle "${vehicle.registration}"` },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (!confirmed) {
          return;
        }
        this.fleet
          .deleteFleetVehicle(vehicle.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (message) => {
              if (this.selectedVehicleId === vehicle.id) {
                this.selectedVehicleId = null;
              }
              this.notifications.success(message);
              this.reload$.next();
            },
            error: (err: Error) => {
              this.notifications.error(err.message ?? 'Could not delete vehicle.');
            },
          });
      });
  }

  // ── Transport partner CRUD ─────────────────────────────────────────────────

  openLinkTransporter(): void {
    this.openTransporterDialog();
  }

  openEditTransporter(partner: TransporterPartnerRow, event?: Event): void {
    event?.stopPropagation();
    this.openTransporterDialog(partner.id);
  }

  confirmDeleteTransporter(partner: TransporterPartnerRow, event?: Event): void {
    event?.stopPropagation();
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: { entityLabel: `transport partner "${partner.name}"` },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (!confirmed) {
          return;
        }
        this.fleet
          .deleteTransporter(partner.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (message) => {
              if (this.selectedPartnerId === partner.id) {
                this.selectedPartnerId = null;
                this.clearPartnerMetadata();
              }
              this.notifications.success(message);
              this.reload$.next();
            },
            error: (err: Error) => {
              this.notifications.error(err.message ?? 'Could not remove transport partner.');
            },
          });
      });
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

  vehicleRingClass(status: FleetVehicleRow['status']): string {
    return `flt-vehicle__ring--${status}`;
  }

  avatarStyle(row: { accentHue: number }): Record<string, string> {
    const hue = row.accentHue;
    return {
      background: `linear-gradient(135deg, hsl(${hue} 72% 38%), hsl(${(hue + 40) % 360} 68% 50%))`,
    };
  }

  trackByVehicleId(_i: number, row: FleetVehicleRow): string {
    return String(row.id);
  }

  trackByPartnerId(_i: number, row: TransporterPartnerRow): number {
    return row.id;
  }

  trackByDriverId(_i: number, row: FleetDriverRow): number {
    return row.id;
  }

  trackByComplianceId(_i: number, row: FleetComplianceRow): number {
    return row.id;
  }

  expiryPillClass(status: FleetComplianceRow['expiryStatus']): string {
    return `flt-pill flt-pill--expiry flt-pill--expiry-${status}`;
  }

  // ── Drivers CRUD ───────────────────────────────────────────────────────────

  openAddDriver(): void {
    this.dialog
      .open(FleetDriverDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        disableClose: true,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((driver: FleetDriverRow | undefined) => {
        if (driver) {
          this.notifications.success(`${driver.fullName} was added to your driver roster.`);
          this.loadDrivers();
        }
      });
  }

  openEditDriver(driver: FleetDriverRow, event?: Event): void {
    event?.stopPropagation();
    this.dialog
      .open(FleetDriverDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        disableClose: true,
        data: { driver } satisfies FleetDriverDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((updated: FleetDriverRow | undefined) => {
        if (updated) {
          this.notifications.success(`${updated.fullName} was updated.`);
          this.loadDrivers();
        }
      });
  }

  confirmDeleteDriver(driver: FleetDriverRow, event?: Event): void {
    event?.stopPropagation();
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: { entityLabel: `driver "${driver.fullName}"` },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (!confirmed) {
          return;
        }
        this.fleet
          .deleteDriver(driver.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (message) => {
              this.notifications.success(message);
              this.loadDrivers();
              this.loadCompliance();
            },
            error: (err: Error) => {
              this.notifications.error(err.message ?? 'Could not delete driver.');
            },
          });
      });
  }

  // ── Compliance CRUD ──────────────────────────────────────────────────────

  openAddCompliance(): void {
    this.dialog
      .open(FleetComplianceDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        disableClose: true,
        data: {
          assets: this.ownFleet,
          drivers: this.drivers,
        } satisfies FleetComplianceDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((record: FleetComplianceRow | undefined) => {
        if (record) {
          this.notifications.success('Compliance record was added.');
          this.loadCompliance();
          this.loadExpiringCompliance();
        }
      });
  }

  openEditCompliance(record: FleetComplianceRow, event?: Event): void {
    event?.stopPropagation();
    this.dialog
      .open(FleetComplianceDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        disableClose: true,
        data: {
          record,
          assets: this.ownFleet,
          drivers: this.drivers,
        } satisfies FleetComplianceDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((updated: FleetComplianceRow | undefined) => {
        if (updated) {
          this.notifications.success('Compliance record was updated.');
          this.loadCompliance();
          this.loadExpiringCompliance();
        }
      });
  }

  contractPillClass(status: TransporterPartnerRow['contractStatus']): string {
    return `flt-pill flt-pill--contract flt-pill--contract-${status}`;
  }

  private openTransporterDialog(partnerId?: number): void {
    this.dialog
      .open(RegisterTransporterDialogComponent, {
        width: 'min(920px, 96vw)',
        maxWidth: '96vw',
        maxHeight: '92vh',
        panelClass: 'lx-dialog-panel--wide',
        disableClose: true,
        data: partnerId ? ({ partnerId } satisfies RegisterTransporterDialogData) : undefined,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((partner: TransporterPartnerRow | undefined) => {
        if (partner) {
          const message = partnerId
            ? `${partner.name} was updated.`
            : `${partner.name} is now a contracted transport partner.`;
          this.notifications.success(message);
          this.reload$.next();
        }
      });
  }

  private loadPartnerMetadata(partnerId: number | null): void {
    if (partnerId == null) {
      this.clearPartnerMetadata();
      return;
    }
    const requestId = ++this.partnerMetadataRequestId;
    this.partnerMetadataLoading = true;
    this.partnerMetadataError = '';
    this.partnerMetadata = null;
    this.partnerFleetLoading = this.isSupplier;
    this.partnerFleetError = '';
    this.partnerFleet = [];
    this.fleet
      .getPartnerMetadata(partnerId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (metadata) => {
          if (requestId !== this.partnerMetadataRequestId || this.selectedPartnerId !== partnerId) {
            return;
          }
          this.partnerMetadata = metadata;
          this.partnerMetadataLoading = false;
          this.cdr.detectChanges();
        },
        error: (err: Error) => {
          if (requestId !== this.partnerMetadataRequestId || this.selectedPartnerId !== partnerId) {
            return;
          }
          this.partnerMetadata = null;
          this.partnerMetadataLoading = false;
          this.partnerMetadataError = err.message ?? 'Could not load partner metadata.';
          this.cdr.detectChanges();
        },
      });

    if (this.isSupplier) {
      this.fleet
        .listTransporterFleetVehicles(partnerId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (rows) => {
            if (requestId !== this.partnerMetadataRequestId || this.selectedPartnerId !== partnerId) {
              return;
            }
            this.partnerFleet = rows;
            this.partnerFleetLoading = false;
            this.cdr.detectChanges();
          },
          error: (err: Error) => {
            if (requestId !== this.partnerMetadataRequestId || this.selectedPartnerId !== partnerId) {
              return;
            }
            this.partnerFleet = [];
            this.partnerFleetLoading = false;
            this.partnerFleetError = err.message ?? 'Could not load partner fleet.';
            this.cdr.detectChanges();
          },
        });
    }
  }

  private clearPartnerMetadata(): void {
    this.partnerMetadataRequestId += 1;
    this.partnerMetadata = null;
    this.partnerMetadataLoading = false;
    this.partnerMetadataError = '';
    this.partnerFleet = [];
    this.partnerFleetLoading = false;
    this.partnerFleetError = '';
  }

  private loadWorkspace(): void {
    this.fetching = true;
    this.loadError = '';

    this.fleet
      .listOwnFleet()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rows) => {
          this.ownFleet = rows;
          this.enrichComplianceSubjectLabels();
          this.cdr.detectChanges();
        },
        error: (err: Error) => {
          this.ownFleet = [];
          if (!this.loadError) {
            this.loadError = err.message ?? 'Could not load fleet vehicles.';
          }
          this.cdr.detectChanges();
        },
      });

    this.fleet
      .listPartners()
      .pipe(
        finalize(() => {
          this.fetching = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.partners = rows.sort((a, b) => a.name.localeCompare(b.name));
        },
        error: (err: Error) => {
          this.partners = [];
          this.loadError = err.message ?? 'Could not load transport partners.';
        },
      });

    this.loadDrivers(false);
    this.loadCompliance(false);
    this.loadExpiringCompliance(false);
  }

  private ensureTabDataLoaded(tab: FleetWorkspaceTab): void {
    if (tab === 'drivers' && !this.drivers.length && !this.driversLoading) {
      this.loadDrivers();
    }
    if (tab === 'compliance' && !this.compliance.length && !this.complianceLoading) {
      this.loadCompliance();
      this.loadExpiringCompliance();
    }
  }

  private isFleetTab(tab: string): tab is FleetWorkspaceTab {
    return ['overview', 'convoy', 'partners', 'drivers', 'compliance'].includes(tab);
  }

  private loadDrivers(showLoading = true): void {
    if (showLoading) {
      this.driversLoading = true;
      this.driversError = '';
    }
    this.fleet
      .listDrivers()
      .pipe(
        finalize(() => {
          this.driversLoading = false;
          this.enrichComplianceSubjectLabels();
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.drivers = rows.sort((a, b) => a.fullName.localeCompare(b.fullName));
        },
        error: (err: Error) => {
          this.drivers = [];
          this.driversError = err.message ?? 'Could not load drivers.';
        },
      });
  }

  private loadCompliance(showLoading = true): void {
    if (showLoading) {
      this.complianceLoading = true;
      this.complianceError = '';
    }
    this.fleet
      .listCompliance()
      .pipe(
        finalize(() => {
          this.complianceLoading = false;
          this.enrichComplianceSubjectLabels();
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.compliance = rows;
        },
        error: (err: Error) => {
          this.compliance = [];
          this.complianceError = err.message ?? 'Could not load compliance records.';
        },
      });
  }

  private loadExpiringCompliance(showLoading = false): void {
    if (showLoading) {
      this.complianceLoading = true;
    }
    this.fleet
      .listExpiringCompliance(30)
      .pipe(
        finalize(() => {
          if (showLoading) {
            this.complianceLoading = false;
          }
          this.enrichComplianceSubjectLabels();
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.expiringCompliance = rows;
        },
        error: () => {
          this.expiringCompliance = [];
        },
      });
  }

  private enrichComplianceSubjectLabels(): void {
    const enrich = (rows: FleetComplianceRow[]) =>
      rows.map((row) => ({
        ...row,
        subjectLabel: this.fleet.resolveComplianceSubjectLabel(
          row.subjectType,
          row.subjectId,
          this.ownFleet,
          this.drivers,
        ),
      }));
    this.compliance = enrich(this.compliance);
    this.expiringCompliance = enrich(this.expiringCompliance);
  }
}
