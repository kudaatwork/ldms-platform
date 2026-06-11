import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import {
  isApiFailureEnvelope,
  readApiFailureMessage,
} from '../../../core/utils/api-paged-response.util';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
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
  CreateFleetVehiclePayload,
  EditFleetCompliancePayload,
  EditFleetDriverPayload,
  EditFleetVehiclePayload,
  FleetComplianceRow,
  FleetComplianceSubjectType,
  FleetComplianceType,
  FleetDriverRow,
  FleetVehicleOwnershipType,
  FleetVehicleRow,
  FleetVehicleStatus,
  FleetVehicleType,
  RegisterTransporterPayload,
  TransporterEditDetail,
  TransporterPartnerRow,
} from '../models/fleet.model';
import { presentTransporterContract } from '../utils/transporter-contract.util';

@Injectable({ providedIn: 'root' })
export class FleetPortalService {
  /** Transporter contracts remain in organization-management. */
  private readonly orgBase = ldmsServiceUrl('organization-management', 'organization', undefined, 'frontend');
  /** Physical assets, drivers, and compliance live in fleet-management. */
  private readonly fleetBase = ldmsServiceUrl('fleet-management', 'fleet', undefined, 'frontend');
  /** File-upload service — frontend surface. */
  private readonly fileUploadBase = ldmsServiceUrl('file-upload-service', 'file-upload', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  // ── Own fleet (fleet-management assets) ───────────────────────────────────

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
  uploadFleetAssetDocument(assetId: number, file: File): Observable<number> {
    const form = new FormData();
    form.append('files', file, file.name);
    form.append(
      'fileUploadRequest',
      JSON.stringify({
        ownerType: 'FLEET_ASSET',
        ownerId: assetId,
        filesMetadata: [{ fileType: 'DOCUMENT' }],
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

  // ── Private helpers ────────────────────────────────────────────────────────

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
    if (ownershipType === 'contracted' && payload.contractedTransporterOrganizationId != null) {
      body['contractedTransporterOrganizationId'] = payload.contractedTransporterOrganizationId;
    }
    if (ownershipType === 'contracted' && payload.contractScope) {
      body['contractScope'] = payload.contractScope.toUpperCase();
    }
    if (ownershipType === 'contracted' && payload.contractScope === 'job' && payload.jobReference) {
      body['jobReference'] = payload.jobReference;
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
      utilizationPct: Math.min(100, Math.max(0, Number(dto['utilizationPct'] ?? dto['utilization_pct'] ?? 0))),
      lastTripLabel,
      driverName: String(dto['driverName'] ?? dto['driver_name'] ?? '—').trim() || '—',
      accentHue: 168 + (seed % 42),
    };
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
      contractStartDate: String(dto['contractStartDate'] ?? '').trim() || undefined,
      contractEndDate: String(dto['contractEndDate'] ?? '').trim() || undefined,
    };
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

  private toDriverApiPayload(payload: CreateFleetDriverPayload | EditFleetDriverPayload): Record<string, unknown> {
    const body: Record<string, unknown> = {
      firstName: payload.firstName,
      lastName: payload.lastName,
      phoneNumber: payload.phoneNumber,
      licenseNumber: payload.licenseNumber,
      licenseClass: payload.licenseClass,
    };
    if (payload.userId != null) {
      body['userId'] = payload.userId;
    }
    return body;
  }

  private mapDriverRow(dto: Record<string, unknown>): FleetDriverRow {
    const firstName = String(dto['firstName'] ?? '').trim();
    const lastName = String(dto['lastName'] ?? '').trim();
    const fullName = `${firstName} ${lastName}`.trim() || 'Unnamed driver';
    const userIdRaw = dto['userId'];
    return {
      id: Number(dto['id'] ?? 0),
      userId: userIdRaw != null && String(userIdRaw).trim() !== '' ? Number(userIdRaw) : undefined,
      firstName,
      lastName,
      fullName,
      phoneNumber: String(dto['phoneNumber'] ?? '').trim() || '—',
      licenseNumber: String(dto['licenseNumber'] ?? '').trim() || '—',
      licenseClass: String(dto['licenseClass'] ?? '').trim() || '—',
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
    'other',
  ];
  return allowed.includes(value as FleetComplianceType) ? (value as FleetComplianceType) : 'other';
}

function complianceTypeLabel(type: FleetComplianceType): string {
  const map: Record<FleetComplianceType, string> = {
    insurance: 'Insurance',
    license: 'License',
    maintenance: 'Maintenance',
    roadworthiness: 'Roadworthiness',
    permit: 'Permit',
    other: 'Other',
  };
  return map[type] ?? type;
}

function complianceStatusLabel(status: string): string {
  const key = status.toUpperCase();
  const map: Record<string, string> = {
    ACTIVE: 'Active',
    EXPIRED: 'Expired',
    PENDING: 'Pending review',
    REVOKED: 'Revoked',
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
