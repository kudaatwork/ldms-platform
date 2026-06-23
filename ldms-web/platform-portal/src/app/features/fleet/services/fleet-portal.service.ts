import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import {
  isApiFailureEnvelope,
  readApiFailureMessage,
} from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { extractFileUploadDtoFromResponse } from '../../../core/utils/file-upload-dto-extract.util';
import type { OrganizationType } from '../../../core/models/auth.model';
import type { OrganizationPartnerMetadata } from '../../../shared/models/organization-metadata.model';
import {
  extractOrganizationDto,
  mapOrganizationPartnerMetadata,
} from '../../../shared/utils/map-organization-metadata.util';
import {
  CompleteFleetRegistrationPayload,
  CreateFleetCompliancePayload,
  CreateFleetDriverPayload,
  CreateFleetTrackingIntegrationCredentialPayload,
  CreateFleetVehiclePayload,
  DriverEmploymentType,
  DriverRosterSource,
  DriverSignupRequestRow,
  DriverSignupRequestStatus,
  EditFleetCompliancePayload,
  EditFleetDriverPayload,
  EditFleetVehiclePayload,
  EditFleetTrackingDevicePayload,
  FleetComplianceRow,
  FleetComplianceSubjectType,
  FleetComplianceType,
  FleetContractScope,
  FleetDriverRow,
  FleetTrackingDeviceRow,
  ProvisionDriverPlatformAccessPayload,
  ProvisionDriverPlatformAccessResult,
  FleetTrackingIntegrationCredentialRow,
  FleetVehicleOwnershipType,
  FleetVehicleRow,
  FleetVehicleStatus,
  FleetVehicleType,
  FleetRegistrationDocumentPayload,
  InstallFleetTrackingDevicePayload,
  MarketplaceDriverAvailability,
  MarketplaceDriverRow,
  RegisterTransporterPayload,
  TrackingDeviceType,
  TrackingInstallStatus,
  TrackingIntegrationProvider,
  TransporterEditDetail,
  TransporterPartnerRow,
} from '../models/fleet.model';
import { presentTransporterContract } from '../utils/transporter-contract.util';

export interface OrganizationFleetDashboardCounts {
  ownedFleetCount: number;
  contractedFleetCount: number;
  organizationDriverCount: number;
  contractedDriverCount: number;
}

@Injectable({ providedIn: 'root' })
export class FleetPortalService {
  /** Transporter contracts remain in organization-management. */
  private readonly orgBase = ldmsServiceUrl('organization-management', 'organization', undefined, 'frontend');
  /** Physical assets, drivers, and compliance live in fleet-management. */
  private readonly fleetBase = ldmsServiceUrl('fleet-management', 'fleet', undefined, 'frontend');
  /** File-upload service — frontend surface. */
  private readonly fileUploadBase = ldmsServiceUrl('file-upload-service', 'file-upload', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  /** GET /dashboard-summary — owned fleet and contracted driver counts for the signed-in organisation. */
  getOrganizationDashboardSummary(): Observable<OrganizationFleetDashboardCounts> {
    return this.http.get<unknown>(`${this.fleetBase}/dashboard-summary`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractOrganizationFleetDashboard(resp);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET file-upload by id (frontend surface — includes base64 content for previews). */
  getFileUploadById(id: number): Observable<Record<string, unknown> | null> {
    if (!Number.isFinite(id) || id < 1) {
      return of(null);
    }
    return this.http.get<unknown>(`${this.fileUploadBase}/find-by-id/${id}`).pipe(
      map((resp) => extractFileUploadDtoFromResponse(resp)),
      catchError(() => of(null)),
    );
  }

  /** GET /assets — trucks/trailers for the signed-in organisation. */
  listOwnFleet(): Observable<FleetVehicleRow[]> {
    return this.http.get<unknown>(`${this.fleetBase}/assets`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractFleetRows(resp).map((dto) => this.mapVehicleRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /assets — add a fleet asset. */
  createFleetVehicle(payload: CreateFleetVehiclePayload): Observable<FleetVehicleRow> {
    return this.http.post<unknown>(`${this.fleetBase}/assets`, this.toFleetVehicleApiPayload(payload)).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingleFleetVehicle(resp);
        return this.mapVehicleRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** PUT /assets/{id} — update a fleet asset. */
  updateFleetVehicle(id: number | string, payload: EditFleetVehiclePayload): Observable<FleetVehicleRow> {
    return this.http.put<unknown>(`${this.fleetBase}/assets/${id}`, this.toFleetVehicleApiPayload(payload)).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingleFleetVehicle(resp);
        return this.mapVehicleRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** Build an edit payload from an existing vehicle row (for partial updates such as driver assignment). */
  vehicleToEditPayload(
    vehicle: FleetVehicleRow,
    driver?: Pick<FleetDriverRow, 'fullName' | 'id'> | null,
  ): EditFleetVehiclePayload {
    return {
      registration: vehicle.registration,
      makeModel: vehicle.makeModel,
      type: vehicle.type,
      status: vehicle.status,
      ownershipType: vehicle.ownershipType,
      contractedTransporterOrganizationId: vehicle.contractedTransporterOrganizationId,
      contractScope: vehicle.contractScope ?? (vehicle.ownershipType === 'contracted' ? 'long_term' : undefined),
      contractStartDate: vehicle.contractStartDate,
      contractEndDate: vehicle.contractEndDate,
      driverName: driver?.fullName?.trim() || undefined,
      fleetDriverId: driver?.id ?? null,
      utilizationPct: vehicle.utilizationPct,
    };
  }

  /** Assign or clear the driver on a vehicle (driver-only update — no contract re-validation). */
  assignDriverToVehicle(vehicle: FleetVehicleRow, driver: FleetDriverRow | null): Observable<FleetVehicleRow> {
    const body: Record<string, unknown> = {};
    if (driver?.id != null && driver.id > 0) {
      body['fleetDriverId'] = driver.id;
    } else {
      body['fleetDriverId'] = null;
    }
    const assignUrl = `${this.fleetBase}/assets/${vehicle.id}/assign-driver`;
    return this.http.post<unknown>(assignUrl, body).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingleFleetVehicle(resp);
        return this.mapVehicleRow(dto);
      }),
      catchError((err: HttpErrorResponse | Error) => {
        if (err instanceof HttpErrorResponse && (err.status === 404 || err.status === 405)) {
          const payload = this.vehicleToEditPayload(vehicle, driver);
          return this.updateFleetVehicle(vehicle.id, this.ensureContractFieldsOnEditPayload(payload));
        }
        return throwError(() => this.toError(err));
      }),
    );
  }

  /** Ensures contracted vehicles always send scope + contract dates on full PUT updates. */
  private ensureContractFieldsOnEditPayload(payload: EditFleetVehiclePayload): EditFleetVehiclePayload {
    if (payload.ownershipType !== 'contracted') {
      return payload;
    }
    const scope = payload.contractScope ?? 'long_term';
    return {
      ...payload,
      contractScope: scope,
      contractStartDate: payload.contractStartDate,
      contractEndDate: payload.contractEndDate,
    };
  }

  /** DELETE /assets/{id} — soft-delete a fleet asset. */
  deleteFleetVehicle(id: number | string): Observable<string> {
    return this.http.delete<unknown>(`${this.fleetBase}/assets/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const parsed = this.toObj(resp);
        const message = parsed?.['message'];
        return typeof message === 'string' && message.trim() ? message.trim() : 'Vehicle removed.';
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /**
   * Upload a compliance document for a fleet asset and return the numeric file-upload id.
   * POST /ldms-file-upload-service/v1/frontend/file-upload/upload
   * ownerType must be FLEET_ASSET so the backend helper resolves the correct owner bucket.
   */
  uploadFleetAssetDocument(
    assetId: number,
    file: File,
    complianceType: FleetRegistrationDocumentPayload['complianceType'],
  ): Observable<number> {
    const form = new FormData();
    form.append('files', file, file.name);
    form.append(
      'fileUploadRequest',
      JSON.stringify({
        ownerType: 'FLEET_ASSET',
        ownerId: assetId,
        filesMetadata: [{ fileType: this.fleetComplianceFileType(complianceType) }],
      }),
    );
    return this.http.post<unknown>(`${this.fileUploadBase}/upload`, form).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const envelope = this.unwrapEnvelope(resp);
        const dto = this.toObj(envelope['fileUploadDto']) ?? envelope;
        const id = Number(dto['id'] ?? dto['fileUploadId'] ?? 0);
        if (!id) {
          throw new Error('File upload service did not return a file id.');
        }
        return id;
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** Upload a driver document (national ID, passport, or licence). */
  uploadFleetDriverDocument(
    driverId: number,
    file: File,
    fileType: 'NATIONAL_ID' | 'PASSPORT' | 'DRIVER_LICENCE',
  ): Observable<number> {
    const form = new FormData();
    form.append('files', file, file.name);
    form.append(
      'fileUploadRequest',
      JSON.stringify({
        ownerType: 'FLEET_DRIVER',
        ownerId: driverId,
        filesMetadata: [{ fileType }],
      }),
    );
    return this.http.post<unknown>(`${this.fileUploadBase}/upload`, form).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const envelope = this.unwrapEnvelope(resp);
        const dto = this.toObj(envelope['fileUploadDto']) ?? envelope;
        const id = Number(dto['id'] ?? dto['fileUploadId'] ?? 0);
        if (!id) {
          throw new Error('File upload service did not return a file id.');
        }
        return id;
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /assets/{id}/complete-registration — submit required compliance docs to finalise asset registration. */
  completeFleetRegistration(assetId: number, payload: CompleteFleetRegistrationPayload): Observable<FleetVehicleRow> {
    return this.http
      .post<unknown>(`${this.fleetBase}/assets/${assetId}/complete-registration`, payload)
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          return this.mapVehicleRow(this.extractSingleFleetVehicle(resp));
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** GET /transporters/{id}/fleet-vehicles — partner rolling stock + supplier-tagged contracted units. */
  listTransporterFleetVehicles(transporterId: number): Observable<FleetVehicleRow[]> {
    return this.http.get<unknown>(`${this.orgBase}/transporters/${transporterId}/fleet-vehicles`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractFleetRows(resp).map((dto) => this.mapVehicleRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  // ── Transport partners ─────────────────────────────────────────────────────

  listPartners(): Observable<TransporterPartnerRow[]> {
    return this.http.get<unknown>(`${this.orgBase}/transporters`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractOrganizationRows(resp).map((dto) => this.mapPartnerRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /transporters/{id} — full linked metadata for spotlight panels. */
  getPartnerMetadata(partnerId: number): Observable<OrganizationPartnerMetadata> {
    return this.http.get<unknown>(`${this.orgBase}/transporters/${partnerId}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return mapOrganizationPartnerMetadata(extractOrganizationDto(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /transporters/{id} — mapped to edit-form detail. */
  getTransporterEditDetail(partnerId: number): Observable<TransporterEditDetail> {
    return this.http.get<unknown>(`${this.orgBase}/transporters/${partnerId}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = extractOrganizationDto(resp);
        return this.mapTransporterEditDetail(dto, partnerId);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  searchTransportCandidates(search: string): Observable<TransporterPartnerRow[]> {
    const q = search.trim();
    const url = q
      ? `${this.orgBase}/transporters/candidates?search=${encodeURIComponent(q)}`
      : `${this.orgBase}/transporters/candidates`;
    return this.http.get<unknown>(url).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractOrganizationRows(resp).map((dto) => this.mapPartnerRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /transporters/register (multipart) — transport org + supplier link. */
  registerTransporter(payload: RegisterTransporterPayload): Observable<TransporterPartnerRow> {
    return this.http.post<unknown>(`${this.orgBase}/transporters/register`, this.buildRegisterFormData(payload)).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const dto = this.extractSingleOrganization(resp);
        if (!dto['id']) {
          throw new Error('Partner was created but the response did not include an organisation id.');
        }
        return this.mapPartnerRow(dto);
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** PUT /transporters/{id} (multipart) — update a contracted transport partner. */
  updateTransporter(partnerId: number, payload: RegisterTransporterPayload): Observable<TransporterPartnerRow> {
    return this.http
      .put<unknown>(`${this.orgBase}/transporters/${partnerId}`, this.buildRegisterFormData(payload))
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          const dto = this.extractSingleOrganization(resp);
          if (!dto['id']) {
            throw new Error('Partner was updated but the response did not include an organisation id.');
          }
          return this.mapPartnerRow(dto);
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** DELETE /transporters/{id} — unlink (soft-delete) a contracted transport partner. */
  deleteTransporter(partnerId: number): Observable<string> {
    return this.http.delete<unknown>(`${this.orgBase}/transporters/${partnerId}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const parsed = this.toObj(resp);
        const message = parsed?.['message'];
        return typeof message === 'string' && message.trim() ? message.trim() : 'Transport partner removed.';
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /transporters/link — link an existing transport company to this supplier
   * with a contract window.
   */
  linkTransporter(
    transporterOrganizationId: number,
    contractStartDate: string,
    contractEndDate?: string,
  ): Observable<void> {
    return this.http
      .post<unknown>(`${this.orgBase}/transporters/link`, {
        transporterOrganizationId,
        contractStartDate,
        contractEndDate: contractEndDate || undefined,
      })
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          return void 0;
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  // ── Drivers (fleet-management) ─────────────────────────────────────────────

  listDrivers(): Observable<FleetDriverRow[]> {
    return this.http.get<unknown>(`${this.fleetBase}/drivers`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractDriverRows(resp).map((dto) => this.mapDriverRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** GET /transporter-partners/{id}/drivers — roster of a linked transport partner. */
  listTransporterPartnerDrivers(transporterOrganizationId: number): Observable<FleetDriverRow[]> {
    return this.http
      .get<unknown>(`${this.fleetBase}/transporter-partners/${transporterOrganizationId}/drivers`)
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          return this.extractDriverRows(resp).map((dto) => this.mapDriverRow(dto));
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  createDriver(payload: CreateFleetDriverPayload): Observable<FleetDriverRow> {
    return this.http.post<unknown>(`${this.fleetBase}/drivers`, this.toDriverApiPayload(payload)).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapDriverRow(this.extractSingleDriver(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  updateDriver(id: number, payload: EditFleetDriverPayload): Observable<FleetDriverRow> {
    return this.http.put<unknown>(`${this.fleetBase}/drivers/${id}`, this.toDriverApiPayload(payload)).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapDriverRow(this.extractSingleDriver(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /**
   * POST /fleet/drivers/{id}/provision-platform-access
   * Enables platform login for a legacy driver or re-sends temporary credentials.
   */
  provisionDriverPlatformAccess(
    id: number,
    payload: ProvisionDriverPlatformAccessPayload,
  ): Observable<ProvisionDriverPlatformAccessResult> {
    const body: Record<string, unknown> = { email: payload.email.trim() };
    if (payload.reissueCredentials) {
      body['reissueCredentials'] = true;
    }
    return this.http.post<unknown>(`${this.fleetBase}/drivers/${id}/provision-platform-access`, body).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const parsed = this.toObj(resp);
        const message = typeof parsed?.['message'] === 'string' ? parsed['message'].trim() : '';
        return {
          driver: this.mapDriverRow(this.extractSingleDriver(resp)),
          message: message || 'Driver platform login enabled.',
        };
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  deleteDriver(id: number): Observable<string> {
    return this.http.delete<unknown>(`${this.fleetBase}/drivers/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const parsed = this.toObj(resp);
        const message = parsed?.['message'];
        return typeof message === 'string' && message.trim() ? message.trim() : 'Driver removed.';
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  // ── Compliance (fleet-management) ──────────────────────────────────────────

  listCompliance(): Observable<FleetComplianceRow[]> {
    return this.http.get<unknown>(`${this.fleetBase}/compliance`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractComplianceRows(resp).map((dto) => this.mapComplianceRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  listExpiringCompliance(withinDays = 30): Observable<FleetComplianceRow[]> {
    return this.http.get<unknown>(`${this.fleetBase}/compliance/expiring?withinDays=${withinDays}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractComplianceRows(resp).map((dto) => this.mapComplianceRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  createCompliance(payload: CreateFleetCompliancePayload): Observable<FleetComplianceRow> {
    return this.http
      .post<unknown>(`${this.fleetBase}/compliance`, this.toComplianceApiPayload(payload))
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          return this.mapComplianceRow(this.extractSingleCompliance(resp));
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  updateCompliance(id: number, payload: EditFleetCompliancePayload): Observable<FleetComplianceRow> {
    return this.http.put<unknown>(`${this.fleetBase}/compliance/${id}`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapComplianceRow(this.extractSingleCompliance(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** Resolve display labels for compliance subject pickers. */
  resolveComplianceSubjectLabel(
    subjectType: FleetComplianceSubjectType,
    subjectId: number,
    assets: FleetVehicleRow[],
    drivers: FleetDriverRow[],
  ): string {
    if (subjectType === 'driver') {
      const driver = drivers.find((d) => d.id === subjectId);
      return driver?.fullName ?? `Driver #${subjectId}`;
    }
    const asset = assets.find((a) => Number(a.id) === subjectId);
    return asset?.registration ?? `Asset #${subjectId}`;
  }

  // ── Tracking devices (fleet-management) ───────────────────────────────────

  /** GET /fleet/tracking-devices — installed devices for the signed-in organisation. */
  listTrackingDevices(): Observable<FleetTrackingDeviceRow[]> {
    return this.http.get<unknown>(`${this.fleetBase}/tracking-devices`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractTrackingDeviceRows(resp).map((dto) => this.mapTrackingDeviceRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /fleet/tracking-devices — install a new tracking device and return the row with ingest credentials. */
  installTrackingDevice(payload: InstallFleetTrackingDevicePayload): Observable<FleetTrackingDeviceRow> {
    return this.http.post<unknown>(`${this.fleetBase}/tracking-devices`, this.toTrackingDeviceApiPayload(payload)).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTrackingDeviceRow(this.extractSingleTrackingDevice(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** PUT /fleet/tracking-devices/{id} — update an existing tracking device. */
  updateTrackingDevice(id: number, payload: EditFleetTrackingDevicePayload): Observable<FleetTrackingDeviceRow> {
    return this.http.put<unknown>(`${this.fleetBase}/tracking-devices/${id}`, this.toTrackingDeviceApiPayload(payload)).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTrackingDeviceRow(this.extractSingleTrackingDevice(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /fleet/tracking-devices/{id}/suspend — suspend an active tracking device. */
  suspendTrackingDevice(id: number): Observable<FleetTrackingDeviceRow> {
    return this.http.post<unknown>(`${this.fleetBase}/tracking-devices/${id}/suspend`, {}).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTrackingDeviceRow(this.extractSingleTrackingDevice(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** DELETE /fleet/tracking-devices/{id} — remove a tracking device. */
  deleteTrackingDevice(id: number): Observable<string> {
    return this.http.delete<unknown>(`${this.fleetBase}/tracking-devices/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        const parsed = this.toObj(resp);
        const message = parsed?.['message'];
        return typeof message === 'string' && message.trim() ? message.trim() : 'Tracking device removed.';
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  // ── Tracking integration credentials (integrator ingest keys) ─────────────

  private get trackingIntegrationCredentialBase(): string {
    return `${this.fleetBase}/tracking-integration-credentials`;
  }

  listTrackingIntegrationCredentials(
    organizationId: number,
  ): Observable<FleetTrackingIntegrationCredentialRow[]> {
    return this.http
      .get<unknown>(`${this.trackingIntegrationCredentialBase}/find-by-organization/${organizationId}`)
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          return this.extractTrackingIntegrationCredentialRows(resp).map((dto) =>
            this.mapTrackingIntegrationCredentialRow(dto),
          );
        }),
        catchError((err) => {
          if (this.isNotFound(err)) {
            return this.listTrackingIntegrationCredentialsFromDevices();
          }
          return throwError(() => this.toError(err));
        }),
      );
  }

  createTrackingIntegrationCredential(
    payload: CreateFleetTrackingIntegrationCredentialPayload,
  ): Observable<FleetTrackingIntegrationCredentialRow> {
    return this.http.post<unknown>(`${this.trackingIntegrationCredentialBase}/create`, payload).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTrackingIntegrationCredentialRow(this.extractSingleTrackingIntegrationCredential(resp));
      }),
      catchError((err) => {
        if (this.isNotFound(err)) {
          return this.installTrackingDevice({
            deviceLabel: payload.credentialLabel,
            deviceType: 'DEDICATED_GPS',
            integrationProvider: payload.integrationProvider,
            fleetAssetId: payload.fleetAssetId,
            externalDeviceId: payload.externalDeviceId,
            tracksGps: true,
            tracksFuel: false,
            notes: payload.notes,
          }).pipe(map((device) => this.mapTrackingDeviceToCredentialRow(device, payload.organizationId)));
        }
        return throwError(() => this.toError(err));
      }),
    );
  }

  suspendTrackingIntegrationCredential(id: number): Observable<FleetTrackingIntegrationCredentialRow> {
    return this.http.post<unknown>(`${this.trackingIntegrationCredentialBase}/${id}/suspend`, {}).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapTrackingIntegrationCredentialRow(this.extractSingleTrackingIntegrationCredential(resp));
      }),
      catchError((err) => {
        if (this.isNotFound(err)) {
          return this.suspendTrackingDevice(id).pipe(map((device) => this.mapTrackingDeviceToCredentialRow(device)));
        }
        return throwError(() => this.toError(err));
      }),
    );
  }

  deleteTrackingIntegrationCredential(id: number): Observable<void> {
    return this.http.delete<unknown>(`${this.trackingIntegrationCredentialBase}/delete/${id}`).pipe(
      map((resp) => {
        this.assertSuccess(resp);
      }),
      catchError((err) => {
        if (this.isNotFound(err)) {
          return this.deleteTrackingDevice(id).pipe(map(() => void 0));
        }
        return throwError(() => this.toError(err));
      }),
    );
  }

  private listTrackingIntegrationCredentialsFromDevices(): Observable<FleetTrackingIntegrationCredentialRow[]> {
    return this.listTrackingDevices().pipe(
      map((devices) =>
        devices
          .filter((device) => device.integrationProvider !== 'LDMS_MOBILE')
          .map((device) => this.mapTrackingDeviceToCredentialRow(device)),
      ),
    );
  }

  private mapTrackingDeviceToCredentialRow(
    device: FleetTrackingDeviceRow,
    organizationId = 0,
  ): FleetTrackingIntegrationCredentialRow {
    return {
      id: device.id,
      organizationId,
      credentialLabel: device.deviceLabel,
      ingestKey: device.ingestKey,
      integrationProvider: device.integrationProvider,
      integrationProviderLabel: device.integrationProviderLabel,
      status: device.installStatus,
      statusLabel: device.installStatusLabel,
      fleetAssetId: device.fleetAssetId,
      vehicleRegistration: device.vehicleRegistration,
      vehicleMakeModel: device.vehicleMakeModel,
      externalDeviceId: device.externalDeviceId,
      mqttTopic: device.mqttTopic,
      lastTelemetryAt: device.lastTelemetryAt,
    };
  }

  private isNotFound(err: unknown): boolean {
    return err instanceof HttpErrorResponse && err.status === 404;
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private toTrackingDeviceApiPayload(
    payload: InstallFleetTrackingDevicePayload | EditFleetTrackingDevicePayload,
  ): Record<string, unknown> {
    const body: Record<string, unknown> = {};
    if (payload.deviceLabel != null) {
      body['deviceLabel'] = payload.deviceLabel;
    }
    if (payload.deviceType != null) {
      body['deviceType'] = payload.deviceType;
    }
    if (payload.integrationProvider != null) {
      body['integrationProvider'] = payload.integrationProvider;
    }
    if (payload.tracksGps != null) {
      body['tracksGps'] = payload.tracksGps;
    }
    if (payload.tracksFuel != null) {
      body['tracksFuel'] = payload.tracksFuel;
    }
    if (payload.notes != null) {
      body['notes'] = payload.notes;
    }
    this.appendOptionalLong(body, 'fleetAssetId', (payload as InstallFleetTrackingDevicePayload).fleetAssetId);
    this.appendOptionalLong(body, 'fleetDriverId', (payload as InstallFleetTrackingDevicePayload).fleetDriverId);
    const deviceSerial = (payload as InstallFleetTrackingDevicePayload).deviceSerial;
    if (deviceSerial) {
      body['deviceSerial'] = deviceSerial;
    }
    const externalDeviceId = (payload as InstallFleetTrackingDevicePayload).externalDeviceId;
    if (externalDeviceId) {
      body['externalDeviceId'] = externalDeviceId;
    }
    return body;
  }

  private mapTrackingDeviceRow(dto: Record<string, unknown>): FleetTrackingDeviceRow {
    const optionalStr = (key: string): string | undefined => {
      const raw = String(dto[key] ?? '').trim();
      return raw || undefined;
    };
    const optionalLongDirect = (key: string): number | undefined => {
      const raw = dto[key];
      if (raw == null || String(raw).trim() === '') return undefined;
      const n = Number(raw);
      return Number.isFinite(n) && n > 0 ? n : undefined;
    };
    const deviceType = normalizeTrackingDeviceType(dto['deviceType']);
    const provider = normalizeTrackingProvider(dto['integrationProvider']);
    const installStatus = normalizeInstallStatus(dto['installStatus']);
    return {
      id: Number(dto['id'] ?? 0),
      deviceLabel: String(dto['deviceLabel'] ?? '').trim() || 'Unnamed device',
      deviceType,
      deviceTypeLabel: trackingDeviceTypeLabel(deviceType),
      installStatus,
      installStatusLabel: trackingInstallStatusLabel(installStatus),
      integrationProvider: provider,
      integrationProviderLabel: trackingProviderLabel(provider),
      fleetAssetId: optionalLongDirect('fleetAssetId'),
      fleetDriverId: optionalLongDirect('fleetDriverId'),
      linkedUserId: optionalLongDirect('linkedUserId'),
      deviceSerial: optionalStr('deviceSerial'),
      externalDeviceId: optionalStr('externalDeviceId'),
      ingestKey: optionalStr('ingestKey'),
      tracksGps: Boolean(dto['tracksGps'] ?? dto['tracks_gps'] ?? false),
      tracksFuel: Boolean(dto['tracksFuel'] ?? dto['tracks_fuel'] ?? false),
      mqttTopic: optionalStr('mqttTopic'),
      vehicleRegistration: optionalStr('vehicleRegistration'),
      vehicleMakeModel: optionalStr('vehicleMakeModel'),
      installedAt: optionalStr('installedAt'),
      lastTelemetryAt: optionalStr('lastTelemetryAt'),
      notes: optionalStr('notes'),
    };
  }

  private extractSingleTrackingDevice(response: unknown): Record<string, unknown> {
    const envelope = this.unwrapEnvelope(response);
    const one = this.toObj(envelope['fleetTrackingDeviceDto']);
    if (one) return one;
    const list = envelope['fleetTrackingDeviceDtoList'];
    if (Array.isArray(list) && list.length) {
      const first = this.toObj(list[0]);
      if (first) return first;
    }
    return envelope;
  }

  private extractTrackingDeviceRows(response: unknown): Record<string, unknown>[] {
    const envelope = this.unwrapEnvelope(response);
    const list = envelope['fleetTrackingDeviceDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(envelope['fleetTrackingDeviceDto']);
    return one ? [one] : [];
  }

  private extractSingleTrackingIntegrationCredential(response: unknown): Record<string, unknown> {
    const envelope = this.unwrapEnvelope(response);
    const one = this.toObj(envelope['fleetTrackingIntegrationCredentialDto']);
    if (one) return one;
    const list = envelope['fleetTrackingIntegrationCredentialDtoList'];
    if (Array.isArray(list) && list.length) {
      const first = this.toObj(list[0]);
      if (first) return first;
    }
    return envelope;
  }

  private extractTrackingIntegrationCredentialRows(response: unknown): Record<string, unknown>[] {
    const envelope = this.unwrapEnvelope(response);
    const list = envelope['fleetTrackingIntegrationCredentialDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(envelope['fleetTrackingIntegrationCredentialDto']);
    return one ? [one] : [];
  }

  private mapTrackingIntegrationCredentialRow(
    dto: Record<string, unknown>,
  ): FleetTrackingIntegrationCredentialRow {
    const optionalStr = (key: string): string | undefined => {
      const raw = String(dto[key] ?? '').trim();
      return raw || undefined;
    };
    const optionalLongDirect = (key: string): number | undefined => {
      const raw = dto[key];
      if (raw == null || String(raw).trim() === '') return undefined;
      const n = Number(raw);
      return Number.isFinite(n) && n > 0 ? n : undefined;
    };
    const provider = normalizeTrackingProvider(dto['integrationProvider']);
    const status = normalizeInstallStatus(dto['status']);
    return {
      id: Number(dto['id'] ?? 0),
      organizationId: Number(dto['organizationId'] ?? 0),
      credentialLabel: String(dto['credentialLabel'] ?? '').trim() || 'Unnamed integrator',
      ingestKey: optionalStr('ingestKey'),
      integrationProvider: provider,
      integrationProviderLabel: trackingProviderLabel(provider),
      status,
      statusLabel: trackingInstallStatusLabel(status),
      fleetAssetId: optionalLongDirect('fleetAssetId'),
      vehicleRegistration: optionalStr('vehicleRegistration'),
      vehicleMakeModel: optionalStr('vehicleMakeModel'),
      externalDeviceId: optionalStr('externalDeviceId'),
      mqttTopic: optionalStr('mqttTopic'),
      lastTelemetryAt: optionalStr('lastTelemetryAt'),
      createdAt: optionalStr('createdAt'),
    };
  }

  private toFleetVehicleApiPayload(
    payload: CreateFleetVehiclePayload | EditFleetVehiclePayload,
  ): Record<string, unknown> {
    const ownershipType = payload.ownershipType ?? 'owned';
    const body: Record<string, unknown> = {
      registration: payload.registration,
      makeModel: payload.makeModel,
      assetType: payload.type.toUpperCase(),
      status: payload.status.toUpperCase(),
      ownershipType: ownershipType.toUpperCase(),
      driverName: payload.driverName,
      utilizationPct: payload.utilizationPct ?? 0,
    };
    if (payload.maxSpeedKmh != null && payload.maxSpeedKmh > 0) {
      body['maxSpeedKmh'] = payload.maxSpeedKmh;
    }
    if (payload.fleetDriverId != null && payload.fleetDriverId > 0) {
      body['fleetDriverId'] = payload.fleetDriverId;
    } else if ('fleetDriverId' in payload && payload.fleetDriverId === null) {
      body['fleetDriverId'] = null;
    }
    if (ownershipType === 'contracted' && payload.contractedTransporterOrganizationId != null) {
      body['contractedTransporterOrganizationId'] = payload.contractedTransporterOrganizationId;
    }
    if (ownershipType === 'contracted') {
      const scope = (payload.contractScope ?? 'long_term') as FleetContractScope;
      body['contractScope'] = scope === 'job' ? 'JOB' : 'LONG_TERM';
      if (scope === 'job' && payload.jobReference) {
        body['jobReference'] = payload.jobReference;
      }
      if (scope === 'long_term') {
        const contractStartDate = String(payload.contractStartDate ?? '').trim();
        const contractEndDate = String(payload.contractEndDate ?? '').trim();
        if (contractStartDate) {
          body['contractStartDate'] = contractStartDate;
        }
        if (contractEndDate) {
          body['contractEndDate'] = contractEndDate;
        }
      }
    }
    return body;
  }

  private mapVehicleRow(dto: Record<string, unknown>): FleetVehicleRow {
    const rawId = dto['id'];
    const id = rawId != null ? Number(rawId) : 0;
    const status = (String(dto['status'] ?? 'available').toLowerCase() as FleetVehicleStatus) || 'available';
    const type =
      (String(dto['assetType'] ?? dto['vehicleType'] ?? dto['vehicle_type'] ?? dto['type'] ?? 'rig')
        .toLowerCase() as FleetVehicleType) || 'rig';
    const reg = String(dto['registration'] ?? '').trim() || '—';
    const seed = hash(reg);
    const ownershipType = normalizeOwnershipType(dto['ownershipType'] ?? dto['ownership_type']);
    const transporterIdRaw = dto['contractedTransporterOrganizationId'] ?? dto['contracted_transporter_organization_id'];
    const transporterId =
      transporterIdRaw != null && String(transporterIdRaw).trim() !== '' ? Number(transporterIdRaw) : undefined;
    const transporterName = String(
      dto['contractedTransporterOrganizationName'] ?? dto['contracted_transporter_organization_name'] ?? '',
    ).trim();
    const contractScopeRaw = String(dto['contractScope'] ?? dto['contract_scope'] ?? '').trim().toLowerCase();
    const contractScope: FleetContractScope | undefined =
      contractScopeRaw === 'job'
        ? 'job'
        : contractScopeRaw === 'long_term'
          ? 'long_term'
          : ownershipType === 'contracted'
            ? 'long_term'
            : undefined;
    const contractStartDate =
      this.apiDateToInputString(dto['contractStartDate'] ?? dto['contract_start_date']) || undefined;
    const contractEndDate =
      this.apiDateToInputString(dto['contractEndDate'] ?? dto['contract_end_date']) || undefined;
    const lastTripRaw = dto['lastTripAt'] ?? dto['last_trip_at'];
    const lastTripLabel =
      String(dto['lastTripLabel'] ?? dto['last_trip_label'] ?? '').trim() ||
      (lastTripRaw ? String(lastTripRaw).slice(0, 10) : '—');
    return {
      id,
      registration: reg,
      makeModel: String(dto['makeModel'] ?? dto['make_model'] ?? '').trim() || '—',
      type,
      status,
      statusLabel: vehicleStatusLabel(status),
      ownershipType,
      ownershipLabel: ownershipLabel(ownershipType, transporterName),
      contractedTransporterOrganizationId: transporterId,
      contractedTransporterOrganizationName: transporterName || undefined,
      contractScope,
      contractStartDate,
      contractEndDate,
      utilizationPct: Math.min(100, Math.max(0, Number(dto['utilizationPct'] ?? dto['utilization_pct'] ?? 0))),
      maxSpeedKmh: dto['maxSpeedKmh'] != null ? Number(dto['maxSpeedKmh']) : dto['max_speed_kmh'] != null ? Number(dto['max_speed_kmh']) : undefined,
      lastTripLabel,
      driverName: String(dto['driverName'] ?? dto['driver_name'] ?? '—').trim() || '—',
      fleetDriverId: this.optionalLongFromDto(dto, 'fleetDriverId') ?? this.optionalLongFromDto(dto, 'fleet_driver_id'),
      accentHue: 168 + (seed % 42),
    };
  }

  private optionalLongFromDto(dto: Record<string, unknown>, key: string): number | undefined {
    const raw = dto[key];
    if (raw == null || String(raw).trim() === '') {
      return undefined;
    }
    const n = Number(raw);
    return Number.isFinite(n) && n > 0 ? n : undefined;
  }

  private mapTransporterEditDetail(dto: Record<string, unknown>, id: number): TransporterEditDetail {
    const today = new Date().toISOString().slice(0, 10);
    return {
      id,
      name: String(dto['name'] ?? '').trim(),
      email: String(dto['email'] ?? '').trim(),
      phoneNumber: String(dto['phoneNumber'] ?? '').trim(),
      organizationType: (String(dto['organizationType'] ?? 'PRIVATE').toUpperCase() as OrganizationType),
      industryId: this.toPositiveId(dto['industryId']),
      contractStartDate: String(dto['contractStartDate'] ?? today).trim() || today,
      contractEndDate: String(dto['contractEndDate'] ?? '').trim() || undefined,
      contactPersonFirstName: String(dto['contactPersonFirstName'] ?? '').trim(),
      contactPersonLastName: String(dto['contactPersonLastName'] ?? '').trim(),
      contactPersonEmail: String(dto['contactPersonEmail'] ?? '').trim(),
      contactPersonPhoneNumber: String(dto['contactPersonPhoneNumber'] ?? '').trim(),
      contactPersonGender: String(dto['contactPersonGender'] ?? '').trim(),
      contactPersonDateOfBirth: String(dto['contactPersonDateOfBirth'] ?? '').trim(),
      contactPersonNationalIdNumber: String(dto['contactPersonNationalIdNumber'] ?? '').trim() || undefined,
      contactPersonPassportNumber: String(dto['contactPersonPassportNumber'] ?? '').trim() || undefined,
      registrationNumber: String(dto['registrationNumber'] ?? '').trim() || undefined,
      taxNumber: String(dto['taxNumber'] ?? '').trim() || undefined,
      taxClearanceCertificateUploadId: this.toPositiveId(dto['taxClearanceCertificateUploadId']),
      contactPersonNationalIdUploadId: this.toPositiveId(dto['contactPersonNationalIdUploadId']),
      contactPersonPassportUploadId: this.toPositiveId(dto['contactPersonPassportUploadId']),
      locationId: this.toPositiveId(dto['locationId']),
      addressLine1: String(dto['addressLine1'] ?? '').trim() || undefined,
      addressLine2: String(dto['addressLine2'] ?? '').trim() || undefined,
      postalCode: String(dto['addressPostalCode'] ?? dto['postalCode'] ?? '').trim() || undefined,
      suburbId: this.toPositiveId(dto['addressSuburbId'] ?? dto['suburbId']),
    };
  }

  private buildRegisterFormData(payload: RegisterTransporterPayload): FormData {
    const form = new FormData();
    this.appendFormValue(form, 'name', payload.name);
    this.appendFormValue(form, 'email', payload.email);
    this.appendFormValue(form, 'phoneNumber', payload.phoneNumber);
    this.appendFormValue(form, 'organizationClassification', 'TRANSPORT_COMPANY');
    this.appendFormValue(form, 'organizationType', payload.organizationType);
    this.appendFormValue(form, 'industryId', payload.industryId);
    this.appendFormValue(form, 'contactPersonFirstName', payload.contactPersonFirstName);
    this.appendFormValue(form, 'contactPersonLastName', payload.contactPersonLastName);
    this.appendFormValue(form, 'contactPersonEmail', payload.contactPersonEmail);
    this.appendFormValue(form, 'contactPersonPhoneNumber', payload.contactPersonPhoneNumber);
    this.appendFormValue(form, 'contactPersonGender', payload.contactPersonGender);
    this.appendFormValue(form, 'contactPersonDateOfBirth', payload.contactPersonDateOfBirth);
    this.appendFormValue(form, 'contactPersonNationalIdNumber', payload.contactPersonNationalIdNumber);
    this.appendFormValue(form, 'contactPersonNationalIdExpiryDate', payload.contactPersonNationalIdExpiryDate);
    this.appendFormValue(form, 'contactPersonPassportNumber', payload.contactPersonPassportNumber);
    this.appendFormValue(form, 'contactPersonPassportExpiryDate', payload.contactPersonPassportExpiryDate);
    this.appendFormValue(form, 'contactPersonNationalIdUploadId', payload.contactPersonNationalIdUploadId);
    this.appendFormFile(form, 'contactPersonNationalIdUpload', payload.contactPersonNationalIdUpload);
    this.appendFormValue(form, 'contactPersonPassportUploadId', payload.contactPersonPassportUploadId);
    this.appendFormFile(form, 'contactPersonPassportUpload', payload.contactPersonPassportUpload);
    this.appendFormValue(form, 'registrationNumber', payload.registrationNumber);
    this.appendFormValue(form, 'taxNumber', payload.taxNumber);
    this.appendFormValue(form, 'createdViaSignup', false);
    this.appendFormValue(form, 'taxClearanceCertificateUploadId', payload.taxClearanceCertificateUploadId);
    this.appendFormFile(form, 'taxClearanceCertificateUpload', payload.taxClearanceCertificateUpload);
    this.appendFormValue(form, 'addressLine1', payload.addressLine1);
    this.appendFormValue(form, 'addressLine2', payload.addressLine2);
    this.appendFormValue(form, 'postalCode', payload.postalCode);
    this.appendFormValue(form, 'suburbId', payload.suburbId);
    this.appendFormValue(form, 'contractStartDate', payload.contractStartDate);
    this.appendFormValue(form, 'contractEndDate', payload.contractEndDate);
    return form;
  }

  private appendFormValue(form: FormData, key: string, value: unknown): void {
    if (value == null || value === '') {
      return;
    }
    form.append(key, String(value));
  }

  private appendFormFile(form: FormData, key: string, file: File | undefined): void {
    if (file) {
      form.append(key, file, file.name);
    }
  }

  private extractSingleFleetVehicle(response: unknown): Record<string, unknown> {
    const envelope = this.unwrapEnvelope(response);
    const one = this.toObj(
      envelope['fleetAssetDto'] ?? envelope['fleetVehicleDto'] ?? envelope['vehicleDto'],
    );
    if (one) {
      return one;
    }
    const list = envelope['fleetAssetDtoList'] ?? envelope['fleetVehicleDtoList'] ?? envelope['vehicleDtoList'];
    if (Array.isArray(list) && list.length) {
      const first = this.toObj(list[0]);
      if (first) {
        return first;
      }
    }
    return envelope;
  }

  private extractOrganizationFleetDashboard(response: unknown): OrganizationFleetDashboardCounts {
    const envelope = this.unwrapEnvelope(response);
    const dto = this.toObj(envelope['organizationFleetDashboardDto']) ?? envelope;
    const num = (key: string) => {
      const raw = dto[key];
      const n = Number(raw ?? 0);
      return Number.isFinite(n) && n >= 0 ? n : 0;
    };
    return {
      ownedFleetCount: num('ownedFleetCount'),
      contractedFleetCount: num('contractedFleetCount'),
      organizationDriverCount: num('organizationDriverCount'),
      contractedDriverCount: num('contractedDriverCount'),
    };
  }

  private extractFleetRows(response: unknown): Record<string, unknown>[] {
    const envelope = this.unwrapEnvelope(response);
    const list = envelope['fleetAssetDtoList'] ?? envelope['fleetVehicleDtoList'] ?? envelope['vehicleDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(envelope['fleetAssetDto'] ?? envelope['fleetVehicleDto'] ?? envelope['vehicleDto']);
    return one ? [one] : [];
  }

  private extractSingleOrganization(response: unknown): Record<string, unknown> {
    const envelope = this.unwrapEnvelope(response);
    const one = this.toObj(envelope['organizationDto']);
    if (one) {
      return one;
    }
    const list = envelope['organizationDtoList'];
    if (Array.isArray(list) && list.length) {
      const first = this.toObj(list[0]);
      if (first) {
        return first;
      }
    }
    return envelope;
  }

  private mapPartnerRow(dto: Record<string, unknown>): TransporterPartnerRow {
    const name = String(dto['name'] ?? '').trim() || 'Unnamed partner';
    const verified = Boolean(dto['isVerified'] ?? dto['verified']);
    const kyc = String(dto['kycStatus'] ?? 'APPROVED');
    const contract = presentTransporterContract(dto);
    return {
      id: Number(dto['id'] ?? 0),
      name,
      email: String(dto['email'] ?? '').trim(),
      phoneNumber: String(dto['phoneNumber'] ?? '').trim(),
      verified,
      verifiedLabel: verified ? 'Verified' : 'Pending trust',
      kycStatusLabel: this.kycLabel(kyc),
      initials: this.initialsFromName(name),
      accentHue: 32 + (hash(name) % 55),
      linkedSinceLabel: contract.linkedSinceLabel,
      contractStartLabel: contract.startLabel,
      contractEndLabel: contract.endLabel,
      contractRangeLabel: contract.rangeLabel,
      contractStatus: contract.status,
      contractStatusLabel: contract.statusLabel,
      partnerKind: 'contracted',
      contractStartDate: this.apiDateToInputString(dto['contractStartDate']) || undefined,
      contractEndDate: this.apiDateToInputString(dto['contractEndDate']) || undefined,
    };
  }

  /** Normalises API dates (ISO string, Jackson [y,m,d] array, or {year,month,day} object) to yyyy-MM-dd. */
  private apiDateToInputString(value: unknown): string {
    if (value == null || value === '') {
      return '';
    }
    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (/^\d{4}-\d{2}-\d{2}/.test(trimmed)) {
        return trimmed.slice(0, 10);
      }
      const parsed = new Date(trimmed);
      return Number.isNaN(parsed.getTime()) ? '' : parsed.toISOString().slice(0, 10);
    }
    if (Array.isArray(value) && value.length >= 3) {
      const year = Number(value[0]);
      const month = Number(value[1]);
      const day = Number(value[2]);
      if (!Number.isFinite(year) || !Number.isFinite(month) || !Number.isFinite(day)) {
        return '';
      }
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }
    if (typeof value === 'object') {
      const record = value as Record<string, unknown>;
      const year = Number(record['year']);
      const month = Number(record['monthValue'] ?? record['month']);
      const day = Number(record['dayOfMonth'] ?? record['day']);
      if (Number.isFinite(year) && Number.isFinite(month) && Number.isFinite(day)) {
        return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      }
    }
    return '';
  }

  private toPositiveId(value: unknown): number | undefined {
    const n = Number(value);
    return Number.isFinite(n) && n > 0 ? n : undefined;
  }

  private kycLabel(status: string): string {
    const key = status.toUpperCase();
    const map: Record<string, string> = {
      APPROVED: 'Approved',
      DRAFT: 'Draft',
      SUBMITTED: 'Submitted',
      UNDER_REVIEW: 'Under review',
    };
    return map[key] ?? key.split('_').join(' ');
  }

  private initialsFromName(name: string): string {
    const parts = name.split(/\s+/).filter(Boolean);
    if (!parts.length) {
      return '?';
    }
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    return `${parts[0][0] ?? ''}${parts[parts.length - 1][0] ?? ''}`.toUpperCase();
  }

  private assertSuccess(response: unknown): void {
    if (isApiFailureEnvelope(response)) {
      throw new Error(readApiFailureMessage(response, 'Request failed'));
    }
  }

  private unwrapEnvelope(response: unknown): Record<string, unknown> {
    const root = this.toObj(response);
    if (!root) {
      return {};
    }
    return this.toObj(root['data']) ?? this.toObj(root['body']) ?? this.toObj(root['payload']) ?? root;
  }

  private extractOrganizationRows(response: unknown): Record<string, unknown>[] {
    const envelope = this.unwrapEnvelope(response);
    const list = envelope['organizationDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(envelope['organizationDto']);
    return one ? [one] : [];
  }

  // ── Driver marketplace ─────────────────────────────────────────────────────

  /** GET /fleet/drivers/marketplace/search?term=… — search approved freelance drivers on the platform. */
  searchMarketplaceDrivers(query: string): Observable<MarketplaceDriverRow[]> {
    const url = query.trim()
      ? `${this.fleetBase}/drivers/marketplace/search?term=${encodeURIComponent(query.trim())}`
      : `${this.fleetBase}/drivers/marketplace/search`;
    return this.http.get<unknown>(url).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractMarketplaceDriverRows(resp).map((dto) => this.mapMarketplaceDriverRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /fleet/drivers/marketplace/{driverId}/hire — hire an approved freelance driver. */
  hireMarketplaceDriver(driverId: number): Observable<FleetDriverRow> {
    return this.http.post<unknown>(`${this.fleetBase}/drivers/marketplace/${driverId}/hire`, {}).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapDriverRow(this.extractSingleDriver(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  // ── Driver signup requests ─────────────────────────────────────────────────

  /** GET /fleet/drivers/signup-requests/freelance-marketplace — pending freelance applicants visible to all transporters. */
  listFreelanceSignupMarketplace(): Observable<DriverSignupRequestRow[]> {
    return this.http
      .get<unknown>(`${this.fleetBase}/drivers/signup-requests/freelance-marketplace`)
      .pipe(
        map((resp) => {
          this.assertSuccess(resp);
          return this.extractSignupRequestRows(resp).map((dto) => this.mapSignupRequestRow(dto));
        }),
        catchError((err) => throwError(() => this.toError(err))),
      );
  }

  /** GET /fleet/drivers/signup-requests/pending — list pending driver signup requests for the signed-in org. */
  listDriverSignupRequests(status?: DriverSignupRequestStatus): Observable<DriverSignupRequestRow[]> {
    const url = status
      ? `${this.fleetBase}/drivers/signup-requests/pending?status=${status}`
      : `${this.fleetBase}/drivers/signup-requests/pending`;
    return this.http.get<unknown>(url).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.extractSignupRequestRows(resp).map((dto) => this.mapSignupRequestRow(dto));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /fleet/drivers/signup-requests/{id}/approve */
  approveDriverSignupRequest(requestId: number): Observable<DriverSignupRequestRow> {
    return this.http.post<unknown>(`${this.fleetBase}/drivers/signup-requests/${requestId}/approve`, {}).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapSignupRequestRow(this.extractSingleSignupRequest(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  /** POST /fleet/drivers/signup-requests/{id}/reject */
  rejectDriverSignupRequest(requestId: number): Observable<DriverSignupRequestRow> {
    return this.http.post<unknown>(`${this.fleetBase}/drivers/signup-requests/${requestId}/reject`, {}).pipe(
      map((resp) => {
        this.assertSuccess(resp);
        return this.mapSignupRequestRow(this.extractSingleSignupRequest(resp));
      }),
      catchError((err) => throwError(() => this.toError(err))),
    );
  }

  private toDriverApiPayload(payload: CreateFleetDriverPayload | EditFleetDriverPayload): Record<string, unknown> {
    const body: Record<string, unknown> = {
      firstName: payload.firstName,
      lastName: payload.lastName,
      phoneNumber: payload.phoneNumber,
      licenseNumber: payload.licenseNumber,
      licenseClass: payload.licenseClass,
      nationalIdNumber: payload.nationalIdNumber,
      passportNumber: payload.passportNumber,
      addressLine1: payload.addressLine1,
      addressLine2: payload.addressLine2,
      addressCity: payload.addressCity,
      addressProvince: payload.addressProvince,
      addressPostalCode: payload.addressPostalCode,
      addressCountry: payload.addressCountry,
    };
    this.appendOptionalDate(body, 'nationalIdExpiryDate', payload.nationalIdExpiryDate);
    this.appendOptionalDate(body, 'passportExpiryDate', payload.passportExpiryDate);
    this.appendOptionalLong(body, 'nationalIdUploadId', payload.nationalIdUploadId);
    this.appendOptionalLong(body, 'passportUploadId', payload.passportUploadId);
    this.appendOptionalLong(body, 'licenseUploadId', payload.licenseUploadId);
    if (payload.userId != null) {
      body['userId'] = payload.userId;
    }
    if (payload.employmentType) {
      body['employmentType'] = payload.employmentType;
    }
    const email = String(payload.email ?? '').trim();
    if (email) {
      body['email'] = email;
    }
    if (payload.provisionPlatformAccess) {
      body['provisionPlatformAccess'] = true;
    }
    return body;
  }

  private appendOptionalDate(body: Record<string, unknown>, key: string, value?: string | null): void {
    const normalized = String(value ?? '').trim().slice(0, 10);
    if (normalized) {
      body[key] = normalized;
    }
  }

  private appendOptionalLong(body: Record<string, unknown>, key: string, value?: number | null): void {
    if (value != null && value > 0) {
      body[key] = value;
    }
  }

  private mapDriverRow(dto: Record<string, unknown>): FleetDriverRow {
    const firstName = String(dto['firstName'] ?? '').trim();
    const lastName = String(dto['lastName'] ?? '').trim();
    const fullName = `${firstName} ${lastName}`.trim() || 'Unnamed driver';
    const userIdRaw = dto['userId'];
    const optionalLong = (key: string): number | undefined => {
      const raw = dto[key];
      if (raw == null || String(raw).trim() === '') {
        return undefined;
      }
      const n = Number(raw);
      return Number.isFinite(n) && n > 0 ? n : undefined;
    };
    const optionalDate = (key: string): string | undefined => {
      const raw = String(dto[key] ?? '').trim();
      return raw ? raw.slice(0, 10) : undefined;
    };
    const optionalStr = (key: string): string | undefined => {
      const raw = String(dto[key] ?? '').trim();
      return raw || undefined;
    };
    const employmentType = normalizeEmploymentType(String(dto['employmentType'] ?? 'EMPLOYED'));
    const rosterSource: DriverRosterSource =
      String(dto['rosterSource'] ?? 'organization').toLowerCase() === 'transport_partner'
        ? 'transport_partner'
        : 'organization';
    return {
      id: Number(dto['id'] ?? 0),
      userId: userIdRaw != null && String(userIdRaw).trim() !== '' ? Number(userIdRaw) : undefined,
      employmentType,
      employmentLabel: employmentType === 'POOL' ? 'Driver pool' : 'Employed',
      rosterSource,
      homeOrganizationName: optionalStr('homeOrganizationName'),
      firstName,
      lastName,
      fullName,
      phoneNumber: String(dto['phoneNumber'] ?? '').trim() || '—',
      licenseNumber: String(dto['licenseNumber'] ?? '').trim() || '—',
      licenseClass: String(dto['licenseClass'] ?? '').trim() || '—',
      nationalIdNumber: optionalStr('nationalIdNumber'),
      nationalIdExpiryDate: optionalDate('nationalIdExpiryDate'),
      nationalIdUploadId: optionalLong('nationalIdUploadId'),
      passportNumber: optionalStr('passportNumber'),
      passportExpiryDate: optionalDate('passportExpiryDate'),
      passportUploadId: optionalLong('passportUploadId'),
      licenseUploadId: optionalLong('licenseUploadId'),
      addressLine1: optionalStr('addressLine1'),
      addressLine2: optionalStr('addressLine2'),
      addressCity: optionalStr('addressCity'),
      addressProvince: optionalStr('addressProvince'),
      addressPostalCode: optionalStr('addressPostalCode'),
      addressCountry: optionalStr('addressCountry'),
      initials: this.initialsFromName(fullName),
      accentHue: 210 + (hash(fullName) % 48),
    };
  }

  private extractSingleDriver(response: unknown): Record<string, unknown> {
    const envelope = this.unwrapEnvelope(response);
    const one = this.toObj(envelope['fleetDriverDto']);
    if (one) {
      return one;
    }
    const list = envelope['fleetDriverDtoList'];
    if (Array.isArray(list) && list.length) {
      const first = this.toObj(list[0]);
      if (first) {
        return first;
      }
    }
    return envelope;
  }

  private extractDriverRows(response: unknown): Record<string, unknown>[] {
    const envelope = this.unwrapEnvelope(response);
    const list = envelope['fleetDriverDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(envelope['fleetDriverDto']);
    return one ? [one] : [];
  }

  private toComplianceApiPayload(payload: CreateFleetCompliancePayload): Record<string, unknown> {
    const body: Record<string, unknown> = {
      subjectType: payload.subjectType.toUpperCase(),
      subjectId: payload.subjectId,
      complianceType: payload.complianceType.toUpperCase(),
      notes: payload.notes,
    };
    if (payload.fileUploadId != null) {
      body['fileUploadId'] = payload.fileUploadId;
    }
    if (payload.expiresAt) {
      body['expiresAt'] = payload.expiresAt;
    }
    return body;
  }

  private mapComplianceRow(dto: Record<string, unknown>): FleetComplianceRow {
    const subjectType = normalizeComplianceSubjectType(dto['subjectType']);
    const complianceType = normalizeComplianceType(dto['complianceType']);
    const expiresAtRaw = dto['expiresAt'] ?? dto['expires_at'];
    const expiresAt = expiresAtRaw ? String(expiresAtRaw) : undefined;
    const status = String(dto['status'] ?? 'ACTIVE').trim();
    return {
      id: Number(dto['id'] ?? 0),
      subjectType,
      subjectTypeLabel: subjectType === 'driver' ? 'Driver' : 'Asset',
      subjectId: Number(dto['subjectId'] ?? 0),
      subjectLabel: '',
      complianceType,
      complianceTypeLabel: complianceTypeLabel(complianceType),
      fileUploadId: this.toPositiveId(dto['fileUploadId']),
      expiresAt,
      expiresLabel: formatExpiryLabel(expiresAt),
      expiryStatus: computeExpiryStatus(expiresAt),
      status,
      statusLabel: complianceStatusLabel(status),
      notes: String(dto['notes'] ?? '').trim(),
    };
  }

  private extractMarketplaceDriverRows(response: unknown): Record<string, unknown>[] {
    const envelope = this.unwrapEnvelope(response);
    const list =
      envelope['fleetDriverDtoList'] ??
      envelope['marketplaceDriverDtoList'] ??
      envelope['driverDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(envelope['marketplaceDriverDto'] ?? envelope['driverDto']);
    return one ? [one] : [];
  }

  private mapMarketplaceDriverRow(dto: Record<string, unknown>): MarketplaceDriverRow {
    const firstName = String(dto['firstName'] ?? '').trim();
    const lastName = String(dto['lastName'] ?? '').trim();
    const fullName = `${firstName} ${lastName}`.trim() || 'Unknown driver';
    const availability = normalizeMarketplaceAvailability(dto['availability']);
    return {
      id: Number(dto['id'] ?? 0),
      firstName,
      lastName,
      fullName,
      phoneNumber: String(dto['phoneNumber'] ?? '').trim() || '—',
      licenseNumber: String(dto['licenseNumber'] ?? '').trim() || '—',
      licenseClass: String(dto['licenseClass'] ?? '').trim() || '—',
      availability,
      availabilityLabel: marketplaceAvailabilityLabel(availability),
      initials: this.initialsFromName(fullName),
      accentHue: 210 + (hash(fullName) % 48),
    };
  }

  private extractSingleSignupRequest(response: unknown): Record<string, unknown> {
    const envelope = this.unwrapEnvelope(response);
    const one = this.toObj(envelope['fleetDriverSignupRequestDto']);
    if (one) return one;
    const list = envelope['fleetDriverSignupRequestDtoList'];
    if (Array.isArray(list) && list.length) {
      const first = this.toObj(list[0]);
      if (first) return first;
    }
    return envelope;
  }

  private extractSignupRequestRows(response: unknown): Record<string, unknown>[] {
    const envelope = this.unwrapEnvelope(response);
    const list = envelope['fleetDriverSignupRequestDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(envelope['fleetDriverSignupRequestDto']);
    return one ? [one] : [];
  }

  private mapSignupRequestRow(dto: Record<string, unknown>): DriverSignupRequestRow {
    const firstName = String(dto['firstName'] ?? '').trim();
    const lastName = String(dto['lastName'] ?? '').trim();
    const fullName = `${firstName} ${lastName}`.trim() || 'Unknown driver';
    const status = normalizeSignupRequestStatus(dto['status']);
    const createdAt = String(dto['createdAt'] ?? '').trim().slice(0, 10);
    const signupType = String(dto['signupType'] ?? '').trim().toUpperCase();
    return {
      id: Number(dto['id'] ?? 0),
      firstName,
      lastName,
      fullName,
      email: String(dto['email'] ?? '').trim(),
      phoneNumber: String(dto['phoneNumber'] ?? '').trim() || '—',
      nationalIdNumber: String(dto['nationalIdNumber'] ?? '').trim() || '—',
      licenseNumber: String(dto['licenseNumber'] ?? '').trim() || '—',
      licenseClass: String(dto['licenseClass'] ?? '').trim() || '—',
      freelance: signupType === 'FREELANCE' || Boolean(dto['freelance'] ?? false),
      status,
      statusLabel: signupRequestStatusLabel(status),
      createdAt,
      createdAtLabel: createdAt || 'Unknown date',
      initials: this.initialsFromName(fullName),
      accentHue: 32 + (hash(fullName) % 55),
      nationalIdFrontUploadId: this.optionalUploadId(dto['nationalIdFrontUploadId']),
      nationalIdBackUploadId: this.optionalUploadId(dto['nationalIdBackUploadId']),
      licenseFrontUploadId: this.optionalUploadId(dto['licenseFrontUploadId']),
      licenseBackUploadId: this.optionalUploadId(dto['licenseBackUploadId']),
    };
  }

  private optionalUploadId(value: unknown): number | undefined {
    const id = Number(value ?? 0);
    return Number.isFinite(id) && id > 0 ? id : undefined;
  }

  private extractSingleCompliance(response: unknown): Record<string, unknown> {
    const envelope = this.unwrapEnvelope(response);
    const one = this.toObj(envelope['fleetComplianceRecordDto']);
    if (one) {
      return one;
    }
    const list = envelope['fleetComplianceRecordDtoList'];
    if (Array.isArray(list) && list.length) {
      const first = this.toObj(list[0]);
      if (first) {
        return first;
      }
    }
    return envelope;
  }

  private extractComplianceRows(response: unknown): Record<string, unknown>[] {
    const envelope = this.unwrapEnvelope(response);
    const list = envelope['fleetComplianceRecordDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.toObj(envelope['fleetComplianceRecordDto']);
    return one ? [one] : [];
  }

  private toObj(value: unknown): Record<string, unknown> | null {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }

  private fleetComplianceFileType(
    complianceType: FleetRegistrationDocumentPayload['complianceType'],
  ): string {
    const map: Record<FleetRegistrationDocumentPayload['complianceType'], string> = {
      VEHICLE_REGISTRATION: 'VEHICLE_REGISTRATION',
      ROAD_LICENSE: 'ROAD_LICENSE',
      ROADWORTHINESS: 'ROADWORTHINESS_CERTIFICATE',
      INSURANCE: 'INSURANCE_CERTIFICATE',
      GOODS_OPERATOR_LICENCE: 'GOODS_OPERATOR_LICENCE',
      PERMIT: 'OPERATING_PERMIT',
      HAZARDOUS_SUBSTANCES_PERMIT: 'HAZARDOUS_SUBSTANCES_PERMIT',
      FIRE_SAFETY_CLEARANCE: 'FIRE_SAFETY_CLEARANCE',
      LEASE_HIRE_AGREEMENT: 'LEASE_HIRE_AGREEMENT',
      LICENSE: 'DRIVER_LICENCE',
      DEFENSIVE_DRIVING_CERTIFICATE: 'DEFENSIVE_DRIVING_CERTIFICATE',
      DRIVER_MEDICAL_CERTIFICATE: 'DRIVER_MEDICAL_CERTIFICATE',
    };
    return map[complianceType] ?? 'OTHER';
  }

  private toError(err: HttpErrorResponse | Error): Error {
    if (err instanceof HttpErrorResponse) {
      const body = err.error;
      if (isApiFailureEnvelope(body)) {
        return new Error(readApiFailureMessage(body, 'Request failed'));
      }
      const parsed = this.toObj(body);
      const msgs = parsed?.['errorMessages'];
      if (Array.isArray(msgs) && msgs.length) {
        return new Error(msgs.map((m) => String(m)).join(' '));
      }
      if (err.status === 403) {
        return new Error('You do not have permission to manage fleet resources.');
      }
      if (err.status === 401) {
        return new Error(
          'Session expired or not signed in on the platform portal. Sign in again at http://localhost:4201 and ensure ldms-api-gateway (8091) is running.',
        );
      }
      if (err.status === 0) {
        return new Error('Cannot reach the API gateway. Start ldms-api-gateway on port 8091.');
      }
      const serverMessage = parsed?.['message'];
      if (typeof serverMessage === 'string' && serverMessage.trim()) {
        return new Error(serverMessage.trim());
      }
      if (err.status === 500) {
        return new Error(
          'Fleet API failed (HTTP 500). Rebuild and restart ldms-organization-management, then reload this page.',
        );
      }
      return new Error(err.message ?? 'Request failed');
    }
    return err;
  }
}

function hash(seed: string): number {
  let h = 0;
  for (let i = 0; i < seed.length; i += 1) {
    h = seed.charCodeAt(i) + ((h << 5) - h);
  }
  return Math.abs(h);
}

function vehicleStatusLabel(status: FleetVehicleStatus): string {
  const map: Record<FleetVehicleStatus, string> = {
    on_road: 'On corridor',
    yard: 'At yard',
    maintenance: 'In workshop',
    available: 'Ready to dispatch',
  };
  return map[status] ?? status;
}

function normalizeEmploymentType(raw: unknown): DriverEmploymentType {
  return String(raw ?? 'EMPLOYED').trim().toUpperCase() === 'POOL' ? 'POOL' : 'EMPLOYED';
}

function normalizeOwnershipType(raw: unknown): FleetVehicleOwnershipType {
  const value = String(raw ?? 'owned').trim().toLowerCase();
  return value === 'contracted' ? 'contracted' : 'owned';
}

function ownershipLabel(type: FleetVehicleOwnershipType, transporterName?: string): string {
  if (type === 'contracted') {
    return transporterName ? `Contracted · ${transporterName}` : 'Contracted asset';
  }
  return 'Own asset';
}

function normalizeComplianceSubjectType(raw: unknown): FleetComplianceSubjectType {
  return String(raw ?? 'asset').trim().toUpperCase() === 'DRIVER' ? 'driver' : 'asset';
}

function normalizeComplianceType(raw: unknown): FleetComplianceType {
  const value = String(raw ?? 'other').trim().toLowerCase();
  const allowed: FleetComplianceType[] = [
    'insurance',
    'license',
    'maintenance',
    'roadworthiness',
    'permit',
    'vehicle_registration',
    'road_license',
    'goods_operator_licence',
    'hazardous_substances_permit',
    'fire_safety_clearance',
    'lease_hire_agreement',
    'defensive_driving_certificate',
    'driver_medical_certificate',
    'other',
  ];
  return allowed.includes(value as FleetComplianceType) ? (value as FleetComplianceType) : 'other';
}

function complianceTypeLabel(type: FleetComplianceType): string {
  const map: Record<FleetComplianceType, string> = {
    insurance: 'Commercial insurance',
    license: "Driver's licence",
    maintenance: 'Maintenance record',
    roadworthiness: 'Certificate of fitness',
    permit: 'Vehicle operator disc',
    vehicle_registration: 'Vehicle registration book',
    road_license: 'Road licence disc',
    goods_operator_licence: "Goods operator's licence",
    hazardous_substances_permit: 'Hazardous substances permit',
    fire_safety_clearance: 'Fire safety clearance',
    lease_hire_agreement: 'Lease / hire agreement',
    defensive_driving_certificate: 'Defensive driving certificate',
    driver_medical_certificate: 'Driver medical certificate',
    other: 'Other',
  };
  return map[type] ?? type;
}

export function isCompliancePendingReview(status: string | undefined | null): boolean {
  return String(status ?? '').trim().toUpperCase() === 'PENDING';
}

function complianceStatusLabel(status: string): string {
  const key = status.toUpperCase();
  const map: Record<string, string> = {
    VALID: 'Valid',
    ACTIVE: 'Valid',
    EXPIRING_SOON: 'Expiring soon',
    EXPIRED: 'Expired',
    PENDING: 'Pending review',
    REVOKED: 'Rejected',
  };
  return map[key] ?? key.split('_').join(' ');
}

function formatExpiryLabel(expiresAt?: string): string {
  if (!expiresAt) {
    return 'No expiry date';
  }
  const date = expiresAt.slice(0, 10);
  return date || 'No expiry date';
}

function normalizeTrackingDeviceType(raw: unknown): TrackingDeviceType {
  const value = String(raw ?? 'MOBILE_PHONE').trim().toUpperCase();
  const allowed: TrackingDeviceType[] = ['MOBILE_PHONE', 'OBD_TELEMATICS', 'DEDICATED_GPS', 'FUEL_SENSOR', 'COMBO_UNIT'];
  return allowed.includes(value as TrackingDeviceType) ? (value as TrackingDeviceType) : 'MOBILE_PHONE';
}

function normalizeTrackingProvider(raw: unknown): TrackingIntegrationProvider {
  const value = String(raw ?? 'LDMS_MOBILE').trim().toUpperCase();
  const allowed: TrackingIntegrationProvider[] = [
    'LDMS_MOBILE', 'GENERIC_MQTT', 'TRACCAR', 'GEOTAB', 'CALAMP', 'WIALON', 'CUSTOM_HTTP',
  ];
  return allowed.includes(value as TrackingIntegrationProvider) ? (value as TrackingIntegrationProvider) : 'LDMS_MOBILE';
}

function normalizeInstallStatus(raw: unknown): TrackingInstallStatus {
  const value = String(raw ?? 'PENDING').trim().toUpperCase();
  const allowed: TrackingInstallStatus[] = ['ACTIVE', 'SUSPENDED', 'PENDING'];
  return allowed.includes(value as TrackingInstallStatus) ? (value as TrackingInstallStatus) : 'PENDING';
}

function trackingDeviceTypeLabel(type: TrackingDeviceType): string {
  const map: Record<TrackingDeviceType, string> = {
    MOBILE_PHONE: 'Mobile phone',
    OBD_TELEMATICS: 'OBD telematics',
    DEDICATED_GPS: 'Dedicated GPS',
    FUEL_SENSOR: 'Fuel sensor',
    COMBO_UNIT: 'Combo unit',
  };
  return map[type] ?? type;
}

function trackingProviderLabel(provider: TrackingIntegrationProvider): string {
  const map: Record<TrackingIntegrationProvider, string> = {
    LDMS_MOBILE: 'LDMS Mobile app',
    GENERIC_MQTT: 'Generic MQTT',
    TRACCAR: 'Traccar',
    GEOTAB: 'Geotab',
    CALAMP: 'CalAmp',
    WIALON: 'Wialon',
    CUSTOM_HTTP: 'Custom HTTP push',
  };
  return map[provider] ?? provider;
}

function trackingInstallStatusLabel(status: TrackingInstallStatus): string {
  const map: Record<TrackingInstallStatus, string> = {
    ACTIVE: 'Active',
    SUSPENDED: 'Suspended',
    PENDING: 'Pending',
  };
  return map[status] ?? status;
}

function normalizeMarketplaceAvailability(raw: unknown): MarketplaceDriverAvailability {
  const value = String(raw ?? 'AVAILABLE').trim().toUpperCase();
  const allowed: MarketplaceDriverAvailability[] = ['AVAILABLE', 'BUSY', 'INACTIVE'];
  return allowed.includes(value as MarketplaceDriverAvailability)
    ? (value as MarketplaceDriverAvailability)
    : 'AVAILABLE';
}

function marketplaceAvailabilityLabel(availability: MarketplaceDriverAvailability): string {
  const map: Record<MarketplaceDriverAvailability, string> = {
    AVAILABLE: 'Available',
    BUSY: 'On assignment',
    INACTIVE: 'Inactive',
  };
  return map[availability] ?? availability;
}

function normalizeSignupRequestStatus(raw: unknown): DriverSignupRequestStatus {
  const value = String(raw ?? 'PENDING').trim().toUpperCase();
  const allowed: DriverSignupRequestStatus[] = ['PENDING', 'APPROVED', 'REJECTED'];
  return allowed.includes(value as DriverSignupRequestStatus)
    ? (value as DriverSignupRequestStatus)
    : 'PENDING';
}

function signupRequestStatusLabel(status: DriverSignupRequestStatus): string {
  const map: Record<DriverSignupRequestStatus, string> = {
    PENDING: 'Pending review',
    APPROVED: 'Approved',
    REJECTED: 'Rejected',
  };
  return map[status] ?? status;
}

function computeExpiryStatus(expiresAt?: string): FleetComplianceRow['expiryStatus'] {
  if (!expiresAt) {
    return 'none';
  }
  const expiry = new Date(expiresAt.slice(0, 10));
  if (Number.isNaN(expiry.getTime())) {
    return 'none';
  }
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  expiry.setHours(0, 0, 0, 0);
  const diffDays = Math.ceil((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  if (diffDays < 0) {
    return 'expired';
  }
  if (diffDays <= 30) {
    return 'expiring';
  }
  return 'ok';
}
