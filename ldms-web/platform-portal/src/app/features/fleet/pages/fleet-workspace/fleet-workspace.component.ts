import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Subject, debounceTime, from, of } from 'rxjs';
import { catchError, concatMap, finalize, map, takeUntil } from 'rxjs/operators';
import {
  exportClientTableAsCsv,
  exportFormatLabel,
  type LxExportFormat,
} from '../../../../shared/utils/lx-export.util';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import { RegisterTransporterDialogComponent } from '../../components/register-transporter-dialog/register-transporter-dialog.component';
import type { RegisterTransporterDialogData } from '../../components/register-transporter-dialog/register-transporter-dialog.component';
import { OwnFleetDialogComponent } from '../../components/own-fleet-dialog/own-fleet-dialog.component';
import type { OwnFleetDialogData } from '../../components/own-fleet-dialog/own-fleet-dialog.component';
import { FleetDriverDialogComponent } from '../../components/fleet-driver-dialog/fleet-driver-dialog.component';
import type { FleetDriverDialogData } from '../../components/fleet-driver-dialog/fleet-driver-dialog.component';
import { FleetAssignDriverDialogComponent } from '../../components/fleet-assign-driver-dialog/fleet-assign-driver-dialog.component';
import type { FleetAssignDriverDialogData } from '../../components/fleet-assign-driver-dialog/fleet-assign-driver-dialog.component';
import { FleetComplianceDialogComponent } from '../../components/fleet-compliance-dialog/fleet-compliance-dialog.component';
import type { FleetComplianceDialogData } from '../../components/fleet-compliance-dialog/fleet-compliance-dialog.component';
import { FleetComplianceBundleReviewDialogComponent } from '../../components/fleet-compliance-bundle-review-dialog/fleet-compliance-bundle-review-dialog.component';
import type { FleetComplianceBundleReviewDialogData } from '../../components/fleet-compliance-bundle-review-dialog/fleet-compliance-bundle-review-dialog.component';
import {
  findComplianceBundleForRow,
  pendingComplianceBundles,
  type FleetComplianceSubjectBundle,
} from '../../utils/fleet-compliance-bundle.util';
import { FleetInstallTrackingDeviceDialogComponent } from '../../components/fleet-install-tracking-device-dialog/fleet-install-tracking-device-dialog.component';
import type { FleetInstallTrackingDeviceDialogData } from '../../components/fleet-install-tracking-device-dialog/fleet-install-tracking-device-dialog.component';
import { DriverMarketplaceSearchComponent } from '../../components/driver-marketplace-search/driver-marketplace-search.component';
import { DriverSignupRequestsComponent } from '../../components/driver-signup-requests/driver-signup-requests.component';
import type { OrganizationPartnerMetadata } from '../../../../shared/models/organization-metadata.model';
import {
  FleetComplianceRow,
  FleetDriverRow,
  FleetTrackingDeviceRow,
  FleetVehicleRow,
  FleetVehicleStatus,
  FleetWorkspaceMetrics,
  FleetWorkspaceTab,
  TrackingDeviceType,
  TrackingInstallStatus,
  TransporterContractStatus,
  TransporterPartnerRow,
} from '../../models/fleet.model';
import { FleetPortalService, isCompliancePendingReview } from '../../services/fleet-portal.service';
import {
  DRIVER_EXPORT_COLUMNS,
  FLEET_VEHICLE_EXPORT_COLUMNS,
  TRANSPORTER_EXPORT_COLUMNS,
  downloadFleetSampleCsv,
  parseFleetDriverImportCsv,
  parseFleetVehicleImportCsv,
} from '../../utils/fleet-export.util';
import {
  FLEET_CARD_DEFAULT_PAGE_SIZE,
  FLEET_CARD_PAGE_SIZE_OPTIONS,
  FLEET_TABLE_PAGE_SIZE_OPTIONS,
  FleetTablePage,
  fleetPageSummary,
} from '../../utils/fleet-table-page.util';

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
  vehicleSearch = '';
  vehicleCategory: 'all' | 'owned' | 'contracted' | FleetVehicleRow['type'] = 'all';
  vehicleFiltersOpen = false;
  partnerFiltersOpen = false;
  vehicleStatusFilter: '' | FleetVehicleStatus = '';
  vehicleRegistrationFilter = '';
  vehicleMakeModelFilter = '';
  vehicleDriverFilter = '';
  partnerContractFilter: '' | TransporterContractStatus = '';
  partnerVerifiedFilter: '' | 'yes' | 'no' = '';
  driverSearch = '';
  driverFiltersOpen = false;
  driverNameFilter = '';
  driverPhoneFilter = '';
  driverLicenseFilter = '';
  driverLicenseClassFilter = '';
  driverLinkedFilter: '' | 'yes' | 'no' = '';
  selectedDriverId: number | null = null;
  fleetExporting = false;
  partnerExporting = false;
  driverExporting = false;
  fleetImporting = false;
  driverImporting = false;
  showFleetCsvInfo = false;
  showPartnerCsvInfo = false;
  showDriverCsvInfo = false;
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
  complianceSearch = '';
  trackingDevices: FleetTrackingDeviceRow[] = [];
  trackingLoading = false;
  trackingError = '';
  trackingSearch = '';

  readonly convoyPage = new FleetTablePage(FLEET_CARD_DEFAULT_PAGE_SIZE);
  readonly partnersPage = new FleetTablePage(FLEET_CARD_DEFAULT_PAGE_SIZE);
  readonly driversPage = new FleetTablePage(FLEET_CARD_DEFAULT_PAGE_SIZE);
  readonly compliancePage = new FleetTablePage();
  readonly cardPageSizeOptions = FLEET_CARD_PAGE_SIZE_OPTIONS;
  readonly tablePageSizeOptions = FLEET_TABLE_PAGE_SIZE_OPTIONS;
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
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const tab = params.get('tab') as FleetWorkspaceTab | null;
      if (tab && this.isFleetTab(tab)) {
        this.applyRouteTab(tab);
      }
    });
    this.route.data.pipe(takeUntil(this.destroy$)).subscribe((data) => {
      const tab = data['tab'] as FleetWorkspaceTab | undefined;
      if (tab && this.isFleetTab(tab)) {
        this.applyRouteTab(tab);
      }
    });
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const tab = params.get('tab') as FleetWorkspaceTab | null;
      if (tab && this.isFleetTab(tab)) {
        this.applyRouteTab(tab);
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

  get isCustomer(): boolean {
    return this.classification === 'CUSTOMER';
  }

  /** Suppliers and customers can both link contracted transporters. */
  get canContractFleet(): boolean {
    return this.isSupplier || this.isCustomer;
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
    if (this.isCustomer) {
      return `Fleet overview for ${this.orgName} — manage your delivery vehicles and contracted transporters in one place.`;
    }
    return `Corridor control for ${this.orgName} — your owned assets and contracted transporters on one live board.`;
  }

  readonly vehicleStatusOptions: { id: '' | FleetVehicleStatus; label: string }[] = [
    { id: '', label: 'Any status' },
    { id: 'available', label: 'Ready to dispatch' },
    { id: 'on_road', label: 'On corridor' },
    { id: 'yard', label: 'At yard' },
    { id: 'maintenance', label: 'In workshop' },
  ];

  readonly partnerContractOptions: { id: '' | TransporterContractStatus; label: string }[] = [
    { id: '', label: 'Any contract' },
    { id: 'active', label: 'Active' },
    { id: 'open_ended', label: 'Open-ended' },
    { id: 'upcoming', label: 'Starts soon' },
    { id: 'expired', label: 'Ended' },
  ];

  get fleetSampleCsvDescription(): string {
    return 'Columns: REGISTRATION, MAKE_MODEL, ASSET_TYPE (rig|van|tanker|flatbed), STATUS, OWNERSHIP (owned), optional DRIVER_NAME and UTILIZATION_PCT.';
  }

  get fleetImportCsvDisclaimer(): string {
    return 'CSV import creates owned fleet assets with core details only. Complete registration documents in the Add vehicle flow. Contracted assets must be added manually.';
  }

  get partnerSampleCsvDescription(): string {
    return 'Sample shows organisation, contract dates, and primary contact columns. Full registration (KYC, identity scans) uses Register partner.';
  }

  get partnerImportCsvDisclaimer(): string {
    return 'Bulk transporter registration requires identity documents — use Register partner per organisation. Export and sample CSV support planning and reporting.';
  }

  get driverSampleCsvDescription(): string {
    return 'Columns: FIRST_NAME, LAST_NAME, optional PHONE, LICENSE_NUMBER, and LICENSE_CLASS. Identity scans and address must be added via Add driver.';
  }

  get driverImportCsvDisclaimer(): string {
    return 'CSV import creates core driver profiles only. Upload licence and identity documents in the Add/Edit driver flow.';
  }

  get hasVehicleScopeFilters(): boolean {
    return (
      this.vehicleStatusFilter !== '' ||
      !!this.vehicleRegistrationFilter.trim() ||
      !!this.vehicleMakeModelFilter.trim() ||
      !!this.vehicleDriverFilter.trim() ||
      this.vehicleCategory !== 'all'
    );
  }

  get hasPartnerScopeFilters(): boolean {
    return this.partnerContractFilter !== '' || this.partnerVerifiedFilter !== '';
  }

  get hasDriverScopeFilters(): boolean {
    return (
      !!this.driverNameFilter.trim() ||
      !!this.driverPhoneFilter.trim() ||
      !!this.driverLicenseFilter.trim() ||
      !!this.driverLicenseClassFilter.trim() ||
      this.driverLinkedFilter !== ''
    );
  }

  get filteredDrivers(): FleetDriverRow[] {
    const q = this.driverSearch.trim().toLowerCase();
    return this.drivers.filter((driver) => {
      if (this.driverLinkedFilter === 'yes' && !driver.userId) {
        return false;
      }
      if (this.driverLinkedFilter === 'no' && driver.userId) {
        return false;
      }
      const nameQ = this.driverNameFilter.trim().toLowerCase();
      if (nameQ && !driver.fullName.toLowerCase().includes(nameQ)) {
        return false;
      }
      const phoneQ = this.driverPhoneFilter.trim().toLowerCase();
      if (phoneQ && !driver.phoneNumber.toLowerCase().includes(phoneQ)) {
        return false;
      }
      const licenseQ = this.driverLicenseFilter.trim().toLowerCase();
      if (licenseQ && !driver.licenseNumber.toLowerCase().includes(licenseQ)) {
        return false;
      }
      const classQ = this.driverLicenseClassFilter.trim().toLowerCase();
      if (classQ && !driver.licenseClass.toLowerCase().includes(classQ)) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay = [
        driver.fullName,
        driver.phoneNumber,
        driver.licenseNumber,
        driver.licenseClass,
        driver.nationalIdNumber ?? '',
        driver.passportNumber ?? '',
        driver.addressCity ?? '',
        driver.addressProvince ?? '',
        driver.addressCountry ?? '',
      ]
        .join(' ')
        .toLowerCase();
      return hay.includes(q);
    });
  }

  get driverRosterMetrics(): { total: number; linked: number; licensed: number; pool: number } {
    return {
      total: this.drivers.length,
      linked: this.drivers.filter((d) => d.userId != null).length,
      licensed: this.drivers.filter((d) => d.licenseNumber && d.licenseNumber !== '—').length,
      pool: this.drivers.filter((d) => d.employmentType === 'POOL').length,
    };
  }

  get filteredOwnFleet(): FleetVehicleRow[] {
    const q = this.vehicleSearch.trim().toLowerCase();
    const category = this.vehicleCategory;
    return this.ownFleet.filter((vehicle) => {
      if (this.vehicleStatusFilter && vehicle.status !== this.vehicleStatusFilter) {
        return false;
      }
      const regQ = this.vehicleRegistrationFilter.trim().toLowerCase();
      if (regQ && !vehicle.registration.toLowerCase().includes(regQ)) {
        return false;
      }
      const modelQ = this.vehicleMakeModelFilter.trim().toLowerCase();
      if (modelQ && !vehicle.makeModel.toLowerCase().includes(modelQ)) {
        return false;
      }
      const driverQ = this.vehicleDriverFilter.trim().toLowerCase();
      if (driverQ && !vehicle.driverName.toLowerCase().includes(driverQ)) {
        return false;
      }
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

  get filteredPartners(): TransporterPartnerRow[] {
    const q = this.partnerSearch.trim().toLowerCase();
    return this.partners.filter((p) => {
      if (this.partnerContractFilter && p.contractStatus !== this.partnerContractFilter) {
        return false;
      }
      if (this.partnerVerifiedFilter === 'yes' && !p.verified) {
        return false;
      }
      if (this.partnerVerifiedFilter === 'no' && p.verified) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay = `${p.name} ${p.email} ${p.phoneNumber} ${p.kycStatusLabel} ${p.contractStatusLabel}`.toLowerCase();
      return hay.includes(q);
    });
  }

  get pagedOwnFleet(): FleetVehicleRow[] {
    this.convoyPage.clamp(this.filteredOwnFleet.length);
    return this.convoyPage.slice(this.filteredOwnFleet);
  }

  get pagedPartners(): TransporterPartnerRow[] {
    this.partnersPage.clamp(this.filteredPartners.length);
    return this.partnersPage.slice(this.filteredPartners);
  }

  get pagedDrivers(): FleetDriverRow[] {
    this.driversPage.clamp(this.filteredDrivers.length);
    return this.driversPage.slice(this.filteredDrivers);
  }

  get filteredCompliance(): FleetComplianceRow[] {
    const base = this.complianceFilter === 'expiring' ? this.expiringCompliance : this.compliance;
    const q = this.complianceSearch.trim().toLowerCase();
    if (!q) {
      return base;
    }
    return base.filter((row) => {
      const hay = [
        row.subjectLabel,
        row.subjectTypeLabel,
        row.complianceTypeLabel,
        row.statusLabel,
        row.expiresLabel,
        row.notes,
        String(row.subjectId),
      ]
        .join(' ')
        .toLowerCase();
      return hay.includes(q);
    });
  }

  get pagedCompliance(): FleetComplianceRow[] {
    this.compliancePage.clamp(this.filteredCompliance.length);
    return this.compliancePage.slice(this.filteredCompliance);
  }

  readonly vehicleCategories: { id: FleetWorkspaceComponent['vehicleCategory']; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'owned', label: 'Owned' },
    { id: 'contracted', label: 'Contracted' },
    { id: 'rig', label: 'Rigs' },
    { id: 'van', label: 'Vans' },
    { id: 'tanker', label: 'Tankers' },
    { id: 'flatbed', label: 'Flatbeds' },
  ];

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
    return this.filteredCompliance;
  }

  get pendingComplianceBundles(): FleetComplianceSubjectBundle[] {
    return pendingComplianceBundles(this.compliance);
  }

  get filteredPendingComplianceBundles(): FleetComplianceSubjectBundle[] {
    const q = this.complianceSearch.trim().toLowerCase();
    const bundles = this.pendingComplianceBundles;
    if (!q) {
      return bundles;
    }
    return bundles.filter((bundle) => {
      const hay = [
        bundle.subjectLabel,
        bundle.subjectType,
        ...bundle.pendingRecords.map((r) => r.complianceTypeLabel),
      ]
        .join(' ')
        .toLowerCase();
      return hay.includes(q);
    });
  }

  pageSummary(page: FleetTablePage, total: number): string {
    return fleetPageSummary(page, total);
  }

  resetConvoyPaging(): void {
    this.convoyPage.reset();
  }

  resetPartnerPaging(): void {
    this.partnersPage.reset();
  }

  resetDriverPaging(): void {
    this.driversPage.reset();
  }

  resetCompliancePaging(): void {
    this.compliancePage.reset();
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

  get selectedDriver(): FleetDriverRow | null {
    if (this.selectedDriverId == null) {
      return null;
    }
    return this.drivers.find((d) => d.id === this.selectedDriverId) ?? null;
  }

  refresh(): void {
    this.reload$.next();
  }

  setTab(tab: FleetWorkspaceTab): void {
    this.applyRouteTab(tab);
    void this.router.navigate(['/fleet', tab]);
  }

  private applyRouteTab(tab: FleetWorkspaceTab): void {
    if (tab !== 'drivers') {
      this.selectedDriverId = null;
    }
    if (tab !== 'convoy' && tab !== 'overview') {
      this.selectedVehicleId = null;
    }
    if (tab !== 'partners' && tab !== 'overview') {
      this.selectedPartnerId = null;
      this.clearPartnerMetadata();
    }
    this.activeTab = tab;
    this.ensureTabDataLoaded(tab);
  }

  selectDriver(row: FleetDriverRow): void {
    this.selectedDriverId = row.id === this.selectedDriverId ? null : row.id;
    this.selectedVehicleId = null;
    this.selectedPartnerId = null;
    this.clearPartnerMetadata();
  }

  clearDriverFilters(): void {
    this.driverSearch = '';
    this.driverNameFilter = '';
    this.driverPhoneFilter = '';
    this.driverLicenseFilter = '';
    this.driverLicenseClassFilter = '';
    this.driverLinkedFilter = '';
    this.resetDriverPaging();
  }

  clearComplianceSearch(): void {
    this.complianceSearch = '';
    this.resetCompliancePaging();
  }

  driverKindLabel(driver: FleetDriverRow): string {
    return driver.userId ? 'Platform user' : 'Manual profile';
  }

  driverEmploymentLabel(driver: FleetDriverRow): string {
    return driver.employmentLabel;
  }

  driverEmploymentClass(driver: FleetDriverRow): string {
    return driver.employmentType === 'POOL' ? 'flt-pill--pool' : 'flt-pill--employed';
  }

  driverKindClass(driver: FleetDriverRow): string {
    return driver.userId ? 'flt-driver-card__kind--linked' : 'flt-driver-card__kind--manual';
  }

  driverIdentityLabel(driver: FleetDriverRow): string {
    if (driver.nationalIdNumber) {
      return `National ID · ${driver.nationalIdNumber}`;
    }
    if (driver.passportNumber) {
      return `Passport · ${driver.passportNumber}`;
    }
    return 'Identity not captured';
  }

  driverAddressLine(driver: FleetDriverRow): string {
    const parts = [driver.addressCity, driver.addressProvince, driver.addressCountry].filter(Boolean);
    return parts.length ? parts.join(', ') : 'No address on file';
  }

  triggerDriverImport(input: HTMLInputElement): void {
    input.click();
  }

  downloadDriverSampleCsv(): void {
    downloadFleetSampleCsv('drivers');
    this.notifications.success('Driver sample CSV downloaded.');
  }

  onDriverImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const rows = parseFleetDriverImportCsv(String(reader.result ?? ''));
        if (!rows.length) {
          this.notifications.error('No importable rows found in the CSV.');
          return;
        }
        this.driverImporting = true;
        let imported = 0;
        let failed = 0;
        from(rows)
          .pipe(
            concatMap((row) =>
              this.fleet
                .createDriver({
                  firstName: row.firstName,
                  lastName: row.lastName,
                  phoneNumber: row.phoneNumber,
                  licenseNumber: row.licenseNumber,
                  licenseClass: row.licenseClass,
                })
                .pipe(
                  map(() => {
                    imported += 1;
                  }),
                  catchError(() => {
                    failed += 1;
                    return of(null);
                  }),
                ),
            ),
            finalize(() => {
              this.driverImporting = false;
              this.loadDrivers();
              if (imported) {
                this.notifications.success(
                  `Imported ${imported} driver${imported === 1 ? '' : 's'}${failed ? ` (${failed} failed)` : ''}.`,
                );
              } else {
                this.notifications.error(`Import failed for all ${failed} row(s). Check names and try again.`);
              }
            }),
            takeUntil(this.destroy$),
          )
          .subscribe();
      } catch (err: unknown) {
        this.notifications.error(err instanceof Error ? err.message : 'Invalid driver CSV.');
      }
    };
    reader.readAsText(file);
  }

  exportDriversAs(format: LxExportFormat): void {
    this.driverExporting = true;
    const rows = this.filteredDrivers;
    const saved = exportClientTableAsCsv(format, rows, DRIVER_EXPORT_COLUMNS, 'fleet-drivers', (message) =>
      this.notifications.show(message),
      { title: 'Fleet drivers' },
    );
    this.driverExporting = false;
    if (saved) {
      this.notifications.success(
        `Exported ${rows.length} driver${rows.length === 1 ? '' : 's'} as ${exportFormatLabel(format)}.`,
      );
    }
  }

  driverLicenseLabel(driver: FleetDriverRow): string {
    if (driver.licenseNumber === '—') {
      return 'Licence pending';
    }
    return `${driver.licenseNumber} · Class ${driver.licenseClass}`;
  }

  setComplianceFilter(filter: 'all' | 'expiring'): void {
    this.complianceFilter = filter;
    this.resetCompliancePaging();
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
        width: '720px',
        maxWidth: '95vw',
        disableClose: true,
        data: {
          isSupplier: this.isSupplier,
          isCustomer: this.isCustomer,
          canContractFleet: this.canContractFleet,
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
        width: '720px',
        maxWidth: '95vw',
        disableClose: true,
        data: {
          vehicle,
          isSupplier: this.isSupplier,
          isCustomer: this.isCustomer,
          canContractFleet: this.canContractFleet,
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

  openAssignDriver(vehicle: FleetVehicleRow, event?: Event): void {
    event?.stopPropagation();
    if (typeof vehicle.id === 'string') {
      return;
    }
    this.dialog
      .open(FleetAssignDriverDialogComponent, {
        width: '560px',
        maxWidth: '95vw',
        disableClose: true,
        data: {
          vehicle,
          drivers: this.drivers,
        } satisfies FleetAssignDriverDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((updated: FleetVehicleRow | undefined) => {
        if (updated) {
          this.notifications.success(
            this.vehicleHasAssignedDriver(updated)
              ? `${updated.driverName} assigned to ${updated.registration}.`
              : `Driver removed from ${updated.registration}.`,
          );
          this.reload$.next();
        }
      });
  }

  vehicleHasAssignedDriver(vehicle: FleetVehicleRow): boolean {
    return !this.isSpotlightPlaceholder(vehicle.driverName);
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

  setVehicleCategory(category: FleetWorkspaceComponent['vehicleCategory']): void {
    this.vehicleCategory = category;
    this.resetConvoyPaging();
    if (
      this.selectedVehicleId != null &&
      !this.filteredOwnFleet.some((vehicle) => vehicle.id === this.selectedVehicleId)
    ) {
      this.selectedVehicleId = null;
    }
  }

  clearVehicleFilters(): void {
    this.vehicleSearch = '';
    this.vehicleCategory = 'all';
    this.vehicleStatusFilter = '';
    this.vehicleRegistrationFilter = '';
    this.vehicleMakeModelFilter = '';
    this.vehicleDriverFilter = '';
    this.resetConvoyPaging();
  }

  clearPartnerFilters(): void {
    this.partnerSearch = '';
    this.partnerContractFilter = '';
    this.partnerVerifiedFilter = '';
    this.resetPartnerPaging();
  }

  triggerFleetImport(input: HTMLInputElement): void {
    input.click();
  }

  triggerPartnerImport(input: HTMLInputElement): void {
    input.click();
  }

  onFleetImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const rows = parseFleetVehicleImportCsv(String(reader.result ?? ''));
        if (!rows.length) {
          this.notifications.error('No importable rows found in the CSV.');
          return;
        }
        this.fleetImporting = true;
        let imported = 0;
        let failed = 0;
        from(rows)
          .pipe(
            concatMap((row) =>
              this.fleet
                .createFleetVehicle({
                  registration: row.registration,
                  makeModel: row.makeModel,
                  type: row.type,
                  status: row.status,
                  ownershipType: row.ownershipType,
                  driverName: row.driverName,
                  utilizationPct: row.utilizationPct,
                })
                .pipe(
                  map(() => {
                    imported += 1;
                  }),
                  catchError(() => {
                    failed += 1;
                    return of(null);
                  }),
                ),
            ),
            finalize(() => {
              this.fleetImporting = false;
              this.reload$.next();
              if (imported) {
                this.notifications.success(
                  `Imported ${imported} vehicle${imported === 1 ? '' : 's'}${failed ? ` (${failed} failed)` : ''}.`,
                );
              } else {
                this.notifications.error(`Import failed for all ${failed} row(s). Check registrations and try again.`);
              }
            }),
            takeUntil(this.destroy$),
          )
          .subscribe();
      } catch (err: unknown) {
        this.notifications.error(err instanceof Error ? err.message : 'Invalid fleet CSV.');
      }
    };
    reader.readAsText(file);
  }

  onPartnerImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = '';
    this.notifications.show(
      'Transporter bulk import is not available yet — identity documents and KYC are required. Use Register partner for each organisation.',
    );
  }

  downloadFleetSampleCsv(): void {
    downloadFleetSampleCsv('vehicles');
    this.notifications.success('Fleet vehicle sample CSV downloaded.');
  }

  downloadPartnerSampleCsv(): void {
    downloadFleetSampleCsv('transporters');
    this.notifications.success('Transporter sample CSV downloaded.');
  }

  exportFleetAs(format: LxExportFormat): void {
    this.fleetExporting = true;
    const rows = this.filteredOwnFleet;
    const saved = exportClientTableAsCsv(format, rows, FLEET_VEHICLE_EXPORT_COLUMNS, 'fleet-vehicles', (message) =>
      this.notifications.show(message),
      { title: 'Own fleet vehicles' },
    );
    this.fleetExporting = false;
    if (saved) {
      this.notifications.success(
        `Exported ${rows.length} vehicle${rows.length === 1 ? '' : 's'} as ${exportFormatLabel(format)}.`,
      );
    }
  }

  exportPartnersAs(format: LxExportFormat): void {
    this.partnerExporting = true;
    const rows = this.filteredPartners;
    const saved = exportClientTableAsCsv(format, rows, TRANSPORTER_EXPORT_COLUMNS, 'transporters', (message) =>
      this.notifications.show(message),
      { title: 'Contracted transporters' },
    );
    this.partnerExporting = false;
    if (saved) {
      this.notifications.success(
        `Exported ${rows.length} partner${rows.length === 1 ? '' : 's'} as ${exportFormatLabel(format)}.`,
      );
    }
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
    return `flt-asset-card__status-ring--${status}`;
  }

  vehicleTypeLabel(type: FleetVehicleRow['type']): string {
    const map: Record<FleetVehicleRow['type'], string> = {
      rig: 'Heavy rig',
      van: 'Van / LDV',
      tanker: 'Tanker',
      flatbed: 'Flatbed',
    };
    return map[type] ?? type;
  }

  vehicleStatusPillClass(status: FleetVehicleRow['status']): string {
    return `flt-pill flt-pill--status flt-pill--status-${status}`;
  }

  assetKindLabel(vehicle: FleetVehicleRow): string {
    return vehicle.ownershipType === 'contracted' ? 'Contracted asset' : 'Owned asset';
  }

  assetKindClass(vehicle: FleetVehicleRow): string {
    return vehicle.ownershipType === 'contracted'
      ? 'flt-asset-card__kind--contracted'
      : 'flt-asset-card__kind--owned';
  }

  assetKindPillClass(vehicle: FleetVehicleRow): string {
    return vehicle.ownershipType === 'contracted' ? 'flt-pill--kind-contracted' : 'flt-pill--kind-owned';
  }

  isSpotlightPlaceholder(value: string): boolean {
    const trimmed = String(value ?? '').trim();
    return !trimmed || trimmed === '—' || trimmed === '-';
  }

  vehicleAvatarStyle(vehicle: FleetVehicleRow): Record<string, string> {
    const hue = vehicle.accentHue;
    return {
      background: `linear-gradient(135deg, hsl(${hue} 68% 42%), hsl(${(hue + 36) % 360} 64% 52%))`,
      color: '#fff',
    };
  }

  vehicleContractHighlight(vehicle: FleetVehicleRow): string {
    const parts: string[] = [];
    if (vehicle.contractedTransporterOrganizationName) {
      parts.push(vehicle.contractedTransporterOrganizationName);
    }
    if (vehicle.contractScope === 'job') {
      parts.push('Job scope');
    } else if (vehicle.contractScope === 'long_term') {
      parts.push('Long-term');
    }
    const dates = this.formatVehicleContractRange(vehicle.contractStartDate, vehicle.contractEndDate);
    if (dates) {
      parts.push(dates);
    }
    return parts.length ? parts.join(' · ') : 'Under contracted transporter link';
  }

  private formatVehicleContractRange(start?: string, end?: string): string | null {
    const from = String(start ?? '').trim().slice(0, 10);
    const to = String(end ?? '').trim().slice(0, 10);
    if (from && to) {
      return `${from} → ${to}`;
    }
    if (from) {
      return `From ${from}`;
    }
    if (to) {
      return `Until ${to}`;
    }
    return null;
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
        width: '760px',
        maxWidth: '96vw',
        maxHeight: '92vh',
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

  openMarketplaceSearch(): void {
    this.dialog
      .open(DriverMarketplaceSearchComponent, {
        width: '680px',
        maxWidth: '96vw',
        maxHeight: '90vh',
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((hired: FleetDriverRow | undefined) => {
        if (hired) {
          this.notifications.success(`${hired.fullName} was hired and added to your roster.`);
          this.loadDrivers();
        }
      });
  }

  openDriverSignupRequests(): void {
    this.dialog.open(DriverSignupRequestsComponent, {
      width: '720px',
      maxWidth: '96vw',
      maxHeight: '90vh',
    });
  }

  openEditDriver(driver: FleetDriverRow, event?: Event): void {
    event?.stopPropagation();
    this.dialog
      .open(FleetDriverDialogComponent, {
        width: '760px',
        maxWidth: '96vw',
        maxHeight: '92vh',
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
              if (this.selectedDriverId === driver.id) {
                this.selectedDriverId = null;
              }
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
    const hasDocument = !!record.fileUploadId;
    this.dialog
      .open(FleetComplianceDialogComponent, {
        width: hasDocument ? '920px' : '560px',
        maxWidth: '96vw',
        maxHeight: '96vh',
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

  isCompliancePendingReview(record: FleetComplianceRow): boolean {
    return isCompliancePendingReview(record.status);
  }

  hasPendingBundleForRow(row: FleetComplianceRow): boolean {
    return this.isCompliancePendingReview(row);
  }

  openComplianceBundleReview(bundle: FleetComplianceSubjectBundle, event?: Event): void {
    event?.stopPropagation();
    if (!bundle.pendingCount) {
      return;
    }
    this.dialog
      .open(FleetComplianceBundleReviewDialogComponent, {
        width: 'min(1120px, 98vw)',
        maxWidth: '98vw',
        maxHeight: '96vh',
        panelClass: 'lx-dialog-panel--wide',
        disableClose: true,
        data: { bundle } satisfies FleetComplianceBundleReviewDialogData,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((result) => {
        if (result?.changed) {
          this.notifications.success('Compliance documents were updated.');
          this.loadCompliance();
          this.loadExpiringCompliance();
        }
      });
  }

  openComplianceReviewForRow(row: FleetComplianceRow, event?: Event): void {
    event?.stopPropagation();
    const bundle = findComplianceBundleForRow(this.compliance, row);
    if (bundle?.pendingCount) {
      this.openComplianceBundleReview(bundle, event);
    }
  }

  complianceBundleIcon(bundle: FleetComplianceSubjectBundle): string {
    return bundle.subjectType === 'driver' ? 'badge' : 'local_shipping';
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
    this.loadTrackingDevices(false);
  }

  private ensureTabDataLoaded(tab: FleetWorkspaceTab): void {
    if (tab === 'drivers' && !this.drivers.length && !this.driversLoading) {
      this.loadDrivers();
    }
    if (tab === 'compliance' && !this.compliance.length && !this.complianceLoading) {
      this.loadCompliance();
      this.loadExpiringCompliance();
    }
    if (tab === 'tracking' && !this.trackingDevices.length && !this.trackingLoading) {
      this.loadTrackingDevices();
    }
  }

  private isFleetTab(tab: string): tab is FleetWorkspaceTab {
    return ['overview', 'convoy', 'partners', 'drivers', 'compliance', 'tracking'].includes(tab);
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

  // ── Tracking device tab ───────────────────────────────────────────────────

  get filteredTrackingDevices(): FleetTrackingDeviceRow[] {
    const q = this.trackingSearch.trim().toLowerCase();
    if (!q) return this.trackingDevices;
    return this.trackingDevices.filter((d) => {
      return [d.deviceLabel, d.deviceTypeLabel, d.integrationProviderLabel, d.vehicleRegistration ?? '', d.installStatusLabel]
        .join(' ')
        .toLowerCase()
        .includes(q);
    });
  }

  get trackingMetrics(): { total: number; active: number; mobile: number; hardware: number } {
    return {
      total: this.trackingDevices.length,
      active: this.trackingDevices.filter((d) => d.installStatus === 'ACTIVE').length,
      mobile: this.trackingDevices.filter((d) => d.deviceType === 'MOBILE_PHONE').length,
      hardware: this.trackingDevices.filter((d) => d.deviceType !== 'MOBILE_PHONE').length,
    };
  }

  get trackingCoverage(): {
    totalVehicles: number;
    coveredVehicles: number;
    missingVehicles: number;
    coveragePct: number;
  } {
    const totalVehicles = this.ownFleet.length;
    if (!totalVehicles) {
      return { totalVehicles: 0, coveredVehicles: 0, missingVehicles: 0, coveragePct: 0 };
    }

    const activeAssets = new Set(
      this.trackingDevices
        .filter((d) => d.installStatus === 'ACTIVE' && d.fleetAssetId != null)
        .map((d) => d.fleetAssetId as number),
    );

    const coveredVehicles = activeAssets.size;
    const missingVehicles = Math.max(0, totalVehicles - coveredVehicles);
    const coveragePct = totalVehicles ? Math.round((coveredVehicles / totalVehicles) * 100) : 0;

    return { totalVehicles, coveredVehicles, missingVehicles, coveragePct };
  }

  deviceTypeIcon(type: TrackingDeviceType): string {
    const map: Record<TrackingDeviceType, string> = {
      MOBILE_PHONE: 'smartphone',
      OBD_TELEMATICS: 'settings_remote',
      DEDICATED_GPS: 'gps_fixed',
      FUEL_SENSOR: 'local_gas_station',
      COMBO_UNIT: 'hub',
    };
    return map[type] ?? 'sensors';
  }

  deviceStatusPillClass(status: TrackingInstallStatus): string {
    if (status === 'ACTIVE') return 'flt-pill flt-pill--ok';
    if (status === 'SUSPENDED') return 'flt-pill flt-pill--warn';
    return 'flt-pill flt-pill--muted';
  }

  openInstallDevice(): void {
    const data: FleetInstallTrackingDeviceDialogData = {
      assets: this.ownFleet,
      drivers: this.drivers,
    };
    this.dialog
      .open(FleetInstallTrackingDeviceDialogComponent, {
        width: '780px',
        maxWidth: '96vw',
        maxHeight: '92vh',
        disableClose: true,
        data,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((device: FleetTrackingDeviceRow | null | undefined) => {
        if (device) {
          this.notifications.success(`Tracking device "${device.deviceLabel}" installed.`);
          this.loadTrackingDevices();
        }
      });
  }

  openEditDevice(device: FleetTrackingDeviceRow, event?: Event): void {
    event?.stopPropagation();
    const data: FleetInstallTrackingDeviceDialogData = {
      device,
      assets: this.ownFleet,
      drivers: this.drivers,
    };
    this.dialog
      .open(FleetInstallTrackingDeviceDialogComponent, {
        width: '780px',
        maxWidth: '96vw',
        maxHeight: '92vh',
        disableClose: true,
        data,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((updated: FleetTrackingDeviceRow | null | undefined) => {
        if (updated) {
          this.notifications.success(`Device "${updated.deviceLabel}" updated.`);
          this.loadTrackingDevices();
        }
      });
  }

  confirmSuspendDevice(device: FleetTrackingDeviceRow, event?: Event): void {
    event?.stopPropagation();
    if (!confirm(`Suspend tracking device "${device.deviceLabel}"? Telemetry ingest will stop.`)) return;
    this.fleet
      .suspendTrackingDevice(device.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.notifications.success(`Device "${updated.deviceLabel}" suspended.`);
          this.loadTrackingDevices();
        },
        error: (err: Error) => {
          this.notifications.error(err.message ?? 'Could not suspend device.');
        },
      });
  }

  confirmDeleteDevice(device: FleetTrackingDeviceRow, event?: Event): void {
    event?.stopPropagation();
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        width: '420px',
        maxWidth: '92vw',
        data: { entityLabel: `tracking device "${device.deviceLabel}"` },
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (!confirmed) return;
        this.fleet
          .deleteTrackingDevice(device.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (message) => {
              this.notifications.success(message);
              this.loadTrackingDevices();
            },
            error: (err: Error) => {
              this.notifications.error(err.message ?? 'Could not delete tracking device.');
            },
          });
      });
  }

  private loadTrackingDevices(showLoading = true): void {
    if (showLoading) {
      this.trackingLoading = true;
      this.trackingError = '';
    }
    this.fleet
      .listTrackingDevices()
      .pipe(
        finalize(() => {
          this.trackingLoading = false;
          this.cdr.detectChanges();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.trackingDevices = rows;
        },
        error: (err: Error) => {
          this.trackingDevices = [];
          this.trackingError = err.message ?? 'Could not load tracking devices.';
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
