import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { Subject, debounceTime } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { RegisterTransporterDialogComponent } from '../../components/register-transporter-dialog/register-transporter-dialog.component';
import type { OrganizationPartnerMetadata } from '../../../../shared/models/organization-metadata.model';
import {
  FleetVehicleRow,
  FleetWorkspaceMetrics,
  FleetWorkspaceTab,
  TransporterPartnerRow,
} from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';
import { buildPreviewFleet } from '../../utils/fleet-preview.util';

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
  selectedVehicleId: string | null = null;
  selectedPartnerId: number | null = null;
  partnerMetadata: OrganizationPartnerMetadata | null = null;
  partnerMetadataLoading = false;
  partnerMetadataError = '';

  ownFleet: FleetVehicleRow[] = [];
  private partnerMetadataRequestId = 0;
  partners: TransporterPartnerRow[] = [];

  private readonly reload$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly title: Title,
    private readonly fleet: FleetPortalService,
    private readonly dialog: MatDialog,
    private readonly notifications: NotificationService,
    private readonly authState: AuthStateService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Fleet & Transporters | LX Platform');
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
    const onRoad = fleet.filter((v) => v.status === 'on_road').length;
    const available = fleet.filter((v) => v.status === 'available' || v.status === 'yard').length;
    const avgUtilization = fleet.length
      ? Math.round(fleet.reduce((sum, v) => sum + v.utilizationPct, 0) / fleet.length)
      : 0;
    const partnersVerified = this.partners.filter((p) => p.verified).length;
    return {
      ownFleetTotal: fleet.length,
      onRoad,
      available,
      avgUtilization,
      partnersTotal: this.partners.length,
      partnersVerified,
    };
  }

  get selectedVehicle(): FleetVehicleRow | null {
    if (!this.selectedVehicleId) {
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

  openLinkTransporter(): void {
    this.dialog
      .open(RegisterTransporterDialogComponent, {
        width: 'min(920px, 96vw)',
        maxWidth: '96vw',
        maxHeight: '92vh',
        panelClass: 'lx-dialog-panel--wide',
        disableClose: true,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((partner: TransporterPartnerRow | undefined) => {
        if (partner) {
          this.notifications.success(`${partner.name} is now a contracted transport partner.`);
          this.reload$.next();
        }
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
    return row.id;
  }

  trackByPartnerId(_i: number, row: TransporterPartnerRow): number {
    return row.id;
  }

  contractPillClass(status: TransporterPartnerRow['contractStatus']): string {
    return `flt-pill flt-pill--contract flt-pill--contract-${status}`;
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
  }

  private clearPartnerMetadata(): void {
    this.partnerMetadataRequestId += 1;
    this.partnerMetadata = null;
    this.partnerMetadataLoading = false;
    this.partnerMetadataError = '';
  }

  private loadWorkspace(): void {
    this.fetching = true;
    this.loadError = '';
    const orgId = Number(this.authState.currentUser?.organizationId ?? 0) || 1;
    this.ownFleet = buildPreviewFleet(orgId, this.orgName);

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
  }
}
