import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, map, of, switchMap, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { collectUploadIdsFromJsonTree } from '../../../shared/utils/collect-upload-ids';
import { resolveFilePreview } from '../../../shared/utils/file-upload-preview';

export interface UsersQuery {
  page: number;
  size: number;
  searchQuery: string;
  columnFilters: Record<string, string>;
}

export interface UserListRow {
  id: number;
  username: string;
  email: string;
  name: string;
  phoneNumber: string;
  gender: string;
  nationalIdNumber: string;
  /** ISO from API, formatted for display */
  dateOfBirthLabel: string;
  emailVerifiedLabel: string;
  role: string;
  /** When set, row actions can deep-link to this group's role assignment screen. */
  userGroupId: number | null;
  accountType: string;
  status: string;
  statusLabel: string;
  createdAtLabel: string;
  updatedAtLabel: string;
}

export interface UserProfileBundle {
  user: Record<string, unknown> | null;
  account: Record<string, unknown> | null;
  security: Record<string, unknown> | null;
  address: Record<string, unknown> | null;
  password: Record<string, unknown> | null;
}

/** Row for user-owned uploads (national ID, passport, etc.) from file-upload service. */
export interface UserFileUploadSummary {
  id: number;
  originalFileName: string;
  contentType: string;
  fileType: string;
  fileSizeInBytes?: number;
  createdAt: string;
  entityStatus: string;
  /** Inline image preview when list/find returned base64 image bytes (capped size). */
  previewImageUrl?: string | null;
  /** Raw PDF data URL when `fileContent` was present (sanitize before iframe/embed). */
  previewPdfDataUrl?: string | null;
  /** PDF row — show PDF tile in table (click row opens viewer). */
  hasPdfPreview?: boolean;
}

export interface IdLabelOption {
  id: number;
  label: string;
}

@Injectable({
  providedIn: 'root',
})
export class UsersAdminService {
  private readonly base = `${environment.apiUrl}/ldms-user-management/v1/${environment.apiSurface}`;
  private readonly fileUploadBase = `${environment.apiUrl}/ldms-file-upload-service/v1/${environment.apiSurface}/file-upload`;

  constructor(private readonly http: HttpClient) {}

  queryUsers(q: UsersQuery): Observable<{ rows: UserListRow[]; totalElements: number }> {
    const firstName = this.normalizedFilter(q.columnFilters['firstName']);
    const lastName = this.normalizedFilter(q.columnFilters['lastName']);
    const username = this.normalizedFilter(q.columnFilters['username']);
    const email = this.normalizedFilter(q.columnFilters['email']);
    const phoneNumber = this.normalizedFilter(q.columnFilters['phoneNumber']);
    const nationalIdNumber = this.normalizedFilter(q.columnFilters['nationalIdNumber']);
    const passportNumber = this.normalizedFilter(q.columnFilters['passportNumber']);
    const entityStatus = this.statusToEntityStatus(q.columnFilters['statusLabel']);
    const body: Record<string, unknown> = {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery.trim(),
      ...(firstName ? { firstName } : {}),
      ...(lastName ? { lastName } : {}),
      ...(username ? { username } : {}),
      ...(email ? { email } : {}),
      ...(phoneNumber ? { phoneNumber } : {}),
      ...(nationalIdNumber ? { nationalIdNumber } : {}),
      ...(passportNumber ? { passportNumber } : {}),
      ...(entityStatus ? { entityStatus } : {}),
      gender: [],
    };
    return this.http.post<unknown>(`${this.base}/user/find-by-multiple-filters`, body).pipe(
      map((resp) => {
        const page = this.extractPagedResult(resp, 'userDtoPage');
        const safeRows = page.rows.filter(
          (r): r is Record<string, unknown> => r !== null && typeof r === 'object' && !Array.isArray(r),
        );
        return {
          rows: safeRows.map((r) => this.mapUserRow(r)),
          totalElements: page.totalElements,
        };
      }),
      catchError((err) => this.emptyUsersPageOnNotFound(err)),
    );
  }

  queryUserRoles(q: UsersQuery): Observable<{ rows: Record<string, unknown>[]; totalElements: number }> {
    const role = this.normalizedFilter(q.columnFilters['role']);
    const description = this.normalizedFilter(q.columnFilters['description']);
    const body = {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery.trim(),
      ...(role ? { role } : {}),
      ...(description ? { description } : {}),
    };
    return this.http.post<unknown>(`${this.base}/user-role/find-by-multiple-filters`, body).pipe(
      map((resp) => this.extractPagedResult(resp, 'userRoleDtoPage')),
      catchError((err) => this.emptyPageOnNotFound(err)),
    );
  }

  queryUserGroups(q: UsersQuery): Observable<{ rows: Record<string, unknown>[]; totalElements: number }> {
    const name = this.normalizedFilter(q.columnFilters['name']);
    const description = this.normalizedFilter(q.columnFilters['description']);
    const body = {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery.trim(),
      ...(name ? { name } : {}),
      ...(description ? { description } : {}),
    };
    return this.http.post<unknown>(`${this.base}/user-group/find-by-multiple-filters`, body).pipe(
      map((resp) => this.extractPagedResult(resp, 'userGroupDtoPage')),
      catchError((err) => this.emptyPageOnNotFound(err)),
    );
  }

  queryUserTypes(q: UsersQuery): Observable<{ rows: Record<string, unknown>[]; totalElements: number }> {
    const userTypeName = this.normalizedFilter(q.columnFilters['userTypeName']);
    const description = this.normalizedFilter(q.columnFilters['description']);
    const body = {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery.trim(),
      ...(userTypeName ? { userTypeName } : {}),
      ...(description ? { description } : {}),
    };
    return this.http.post<unknown>(`${this.base}/user-type/find-by-multiple-filters`, body).pipe(
      map((resp) => {
        const parsed = this.parsePossiblyStringifiedJson(resp);
        const obj = this.toObj(parsed);
        const root = obj ? this.toObj(obj['data']) ?? obj : null;
        const explicitPage = root
          ? this.toObj(root['userTypeDtoPage']) ??
            this.toObj(root['userTypePage']) ??
            this.toObj(root['page'])
          : null;
        const discoveredPage = root ? this.findPageObject(root) : null;
        const page = explicitPage ?? discoveredPage;
        const content = page && Array.isArray(page['content']) ? (page['content'] as Record<string, unknown>[]) : [];
        return {
          rows: content,
          totalElements: Number((page?.['totalElements'] as number | undefined) ?? content.length),
        };
      }),
      catchError((err) => this.emptyPageOnNotFound(err)),
    );
  }

  private findPageObject(src: Record<string, unknown>): Record<string, unknown> | null {
    if (Array.isArray(src['content'])) {
      return src;
    }
    for (const value of Object.values(src)) {
      const nested = this.toObj(value);
      if (!nested) continue;
      const found = this.findPageObject(nested);
      if (found) return found;
    }
    return null;
  }

  getUserGroupById(id: number): Observable<Record<string, unknown> | null> {
    return this.http.get<unknown>(`${this.base}/user-group/find-by-id/${id}`).pipe(
      map((resp) => this.extractSingleDto(resp, 'userGroupDto')),
      catchError((err) => this.emptyOnNotFound(err)),
    );
  }

  getUserProfileBundle(userId: number): Observable<UserProfileBundle> {
    return this.http.get<unknown>(`${this.base}/user/find-by-id/${userId}`).pipe(
      map((resp) => {
        const user = this.extractUserDtoFromFindByIdResponse(resp);
        return {
          user,
          account: this.asRecord(user?.['userAccountDto']) ?? null,
          security: this.asRecord(user?.['userSecurityDto']) ?? null,
          address: this.asRecord(user?.['addressDto']) ?? null,
          password: this.asRecord(user?.['userPasswordDto']) ?? null,
        };
      }),
      catchError((err) => this.emptyProfileBundleOnNotFound(err)),
    );
  }

  /**
   * Lists uploads by owner, then merges every `*UploadId` found anywhere on the profile bundle
   * (user, account, address, security, password) so linked files still appear when owner linkage
   * or API shape differs. Matches platform-wide `nationalIdUploadId`, `passportUploadId`, etc.
   */
  listUserFileUploadsForProfile(userId: number, bundle: UserProfileBundle | null): Observable<UserFileUploadSummary[]> {
    return this.listUserFileUploads(userId).pipe(
      switchMap((rows) => {
        const linkedIds = this.collectLinkedUploadIdsFromProfile(bundle);
        const missing = linkedIds.filter((id) => !rows.some((r) => r.id === id));
        const mergedList$ = !missing.length
          ? of(this.sortUploadSummaries(rows))
          : forkJoin(missing.map((id) => this.getFileUploadById(id).pipe(catchError(() => of(null))))).pipe(
              map((extras) => {
                const merged = new Map<number, UserFileUploadSummary>();
                for (const r of rows) {
                  merged.set(r.id, r);
                }
                for (const dto of extras) {
                  const rec = dto ? this.asRecord(dto) : null;
                  const row = rec ? this.fileUploadDtoToSummary(rec) : null;
                  if (row) {
                    merged.set(row.id, row);
                  }
                }
                return this.sortUploadSummaries([...merged.values()]);
              }),
            );
        return mergedList$.pipe(
          switchMap((sorted) => this.enrichProfileLinkedUploadsWithFindById(sorted, bundle?.user)),
        );
      }),
    );
  }

  /** Admin/system password change: `userId` + new `password`; `oldPassword` omitted for reset-style updates. */
  changeUserPasswordForUser(userId: number, password: string): Observable<unknown> {
    return this.http.put(`${this.base}/user-password/change-password`, {
      userId,
      password,
    });
  }

  updateUserAddress(payload: {
    id: number;
    locationAddressId?: number;
    line1: string;
    line2?: string;
    postalCode: string;
    suburbId: number;
    geoCoordinatesId?: number;
  }): Observable<unknown> {
    return this.http.put(`${this.base}/user-address/update`, payload);
  }

  updateUserAccount(payload: {
    id: number;
    userId: number;
    phoneNumber?: string;
    accountNumber?: string;
    isAccountLocked?: boolean;
  }): Observable<unknown> {
    return this.http.put(`${this.base}/user-account/update`, payload);
  }

  updateUserSecurity(payload: {
    id: number;
    userId: number;
    securityQuestion_1: string;
    securityAnswer_1: string;
    securityAnswer_2: string;
    twoFactorAuthSecret: string;
    isTwoFactorEnabled: boolean;
    securityQuestion_2?: string;
  }): Observable<unknown> {
    return this.http.put(`${this.base}/user-security/update`, payload);
  }

  updateUserPreferences(payload: {
    id: number;
    userId: number;
    preferredLanguage: string;
    timezone: string;
  }): Observable<unknown> {
    return this.http.put(`${this.base}/user-preferences/update`, payload);
  }

  /**
   * `find-by-owner` often omits `fileContent`, so image/PDF previews are empty even when the row
   * exists. Re-fetch by id for national ID / passport ids on the user so thumbnails match
   * `GET .../file-upload/find-by-id/{id}` (same as admin curl).
   */
  private enrichProfileLinkedUploadsWithFindById(
    rows: UserFileUploadSummary[],
    user: Record<string, unknown> | null | undefined,
  ): Observable<UserFileUploadSummary[]> {
    const ids = new Set<number>();
    for (const key of ['nationalIdUploadId', 'passportUploadId'] as const) {
      const n = Number(user?.[key] ?? 0);
      if (Number.isFinite(n) && n > 0) {
        ids.add(n);
      }
    }
    if (!ids.size) {
      return of(rows);
    }
    const idList = [...ids];
    return forkJoin(idList.map((id) => this.getFileUploadById(id).pipe(catchError(() => of(null))))).pipe(
      map((dtos) => {
        const byId = new Map(rows.map((r) => [r.id, r]));
        for (let i = 0; i < idList.length; i++) {
          const rec = dtos[i] ? this.asRecord(dtos[i]) : null;
          const row = rec ? this.fileUploadDtoToSummary(rec) : null;
          if (row) {
            byId.set(row.id, row);
          }
        }
        return this.sortUploadSummaries([...byId.values()]);
      }),
    );
  }

  private collectLinkedUploadIdsFromProfile(bundle: UserProfileBundle | null): number[] {
    if (!bundle) {
      return [];
    }
    const parts: unknown[] = [
      bundle.user,
      bundle.account,
      bundle.address,
      bundle.security,
      bundle.password,
    ].filter((p) => p != null);
    const fromRoots = parts.flatMap((p) => collectUploadIdsFromJsonTree(p));
    return [...new Set(fromRoots)];
  }

  private sortUploadSummaries(rows: UserFileUploadSummary[]): UserFileUploadSummary[] {
    return [...rows].sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)));
  }

  private mapFileUploadDtoList(response: unknown): UserFileUploadSummary[] {
    const list = this.findFileUploadDtoArray(response);
    if (!list.length) {
      return [];
    }
    return list
      .map((row) => this.asRecord(row))
      .map((r) => (r ? this.fileUploadDtoToSummary(r) : null))
      .filter((r): r is UserFileUploadSummary => r != null);
  }

  private findFileUploadDtoArray(response: unknown): unknown[] {
    const parsed = this.parsePossiblyStringifiedJson(response);
    if (Array.isArray(parsed)) {
      return parsed;
    }
    const obj = this.toObj(parsed);
    if (!obj) {
      return [];
    }
    const tryKeys = (root: Record<string, unknown>): unknown[] | null => {
      for (const key of ['fileUploadDtoList', 'fileUploadDtos', 'fileUploads', 'documents']) {
        const v = root[key];
        if (Array.isArray(v)) {
          return v;
        }
      }
      return null;
    };
    const direct = tryKeys(obj);
    if (direct) {
      return direct;
    }
    for (const wrapKey of ['data', 'body', 'payload'] as const) {
      const inner = obj[wrapKey];
      const innerParsed =
        typeof inner === 'string' ? this.parsePossiblyStringifiedJson(inner.trim()) : inner;
      const innerObj = this.toObj(innerParsed);
      if (innerObj) {
        const hit = tryKeys(innerObj);
        if (hit) {
          return hit;
        }
      }
    }
    const singleton = this.asRecord(obj['fileUploadDto']);
    if (singleton && singleton['id'] != null) {
      return [singleton];
    }
    return [];
  }

  private fileTypeToLabel(v: unknown): string {
    if (v == null || v === '') {
      return '';
    }
    if (typeof v === 'string') {
      return v.trim();
    }
    if (typeof v === 'object' && v !== null && 'name' in (v as object)) {
      return String((v as { name?: unknown }).name ?? '').trim();
    }
    return String(v).trim();
  }

  private fileUploadDtoToSummary(r: Record<string, unknown>): UserFileUploadSummary | null {
    const id = Number(r['id'] ?? 0);
    if (!Number.isFinite(id) || id <= 0) {
      return null;
    }
    let original = String(r['originalFileName'] ?? '').trim();
    const stored = String(r['storedFileName'] ?? '').trim();
    if (!original && stored) {
      original = stored;
    }
    const fileType = this.fileTypeToLabel(r['fileType']);
    if (!original) {
      original = fileType ? fileType.replace(/_/g, ' ') : `Document #${id}`;
    }
    const preview = resolveFilePreview(r, { maxBase64Chars: 900_000 });
    const previewImageUrl = preview?.kind === 'image' ? preview.dataUrl : null;
    const previewPdfDataUrl = preview?.kind === 'pdf' ? preview.dataUrl : null;
    const ctLower = String(r['contentType'] ?? '').trim().toLowerCase();
    const nameLower = String(r['originalFileName'] ?? '').trim().toLowerCase();
    const hasPdfPreview =
      preview?.kind === 'pdf' || ctLower.includes('pdf') || nameLower.endsWith('.pdf');
    return {
      id,
      originalFileName: original,
      contentType: String(r['contentType'] ?? '').trim(),
      fileType,
      fileSizeInBytes: r['fileSizeInBytes'] != null ? Number(r['fileSizeInBytes']) : undefined,
      createdAt: r['createdAt'] != null ? String(r['createdAt']) : '',
      entityStatus: r['entityStatus'] != null ? String(r['entityStatus']) : '',
      previewImageUrl,
      previewPdfDataUrl,
      hasPdfPreview,
    };
  }

  /** Resolves `UserDto` from `UserResponse` across common gateway / naming variants. */
  private extractUserDtoFromFindByIdResponse(response: unknown): Record<string, unknown> | null {
    for (const key of ['userDto', 'user', 'UserDto'] as const) {
      const hit = this.extractSingleDto(response, key);
      if (hit) {
        return hit;
      }
    }
    return null;
  }

  findUserAccountByUserId(userId: number): Observable<Record<string, unknown> | null> {
    const body = { page: 0, size: 1, searchValue: '', phoneNumber: '', accountNumber: '', userId };
    return this.http.post<unknown>(`${this.base}/user-account/find-by-multiple-filters`, body).pipe(
      map((resp) => this.extractPagedResult(resp, 'userAccountDtoPage').rows[0] ?? null),
      catchError((err) => this.emptyOnNotFound(err)),
    );
  }

  findUserSecurityByUserId(userId: number): Observable<Record<string, unknown> | null> {
    const body = {
      page: 0,
      size: 1,
      searchValue: '',
      securityQuestion_1: '',
      securityAnswer_1: '',
      securityQuestion_2: '',
      securityAnswer_2: '',
      twoFactorAuthSecret: '',
      isTwoFactorEnabled: null,
      userId,
    };
    return this.http.post<unknown>(`${this.base}/user-security/find-by-multiple-filters`, body).pipe(
      map((resp) => this.extractPagedResult(resp, 'userSecurityDtoPage').rows[0] ?? null),
      catchError((err) => this.emptyOnNotFound(err)),
    );
  }

  /** Full `UserSecurityDto` for edit dialogs (includes TOTP secret when API returns it). */
  getUserSecurityById(id: number): Observable<Record<string, unknown> | null> {
    return this.http.get<unknown>(`${this.base}/user-security/find-by-id/${id}`).pipe(
      map((resp) => this.extractSingleDto(resp, 'userSecurityDto')),
      catchError(() => of(null)),
    );
  }

  /** Files stored against this user (`OwnerType.USER`). */
  listUserFileUploads(userId: number): Observable<UserFileUploadSummary[]> {
    return this.http
      .get<unknown>(`${this.fileUploadBase}/find-by-owner`, {
        params: { ownerType: 'USER', ownerId: String(userId) },
      })
      .pipe(
        map((resp) => this.mapFileUploadDtoList(resp)),
        catchError(() => of([] as UserFileUploadSummary[])),
      );
  }

  /** Full metadata for one upload (may include base64 `fileContent` for previews). */
  getFileUploadById(id: number): Observable<Record<string, unknown> | null> {
    return this.http.get<unknown>(`${this.fileUploadBase}/find-by-id/${id}`).pipe(
      map((resp) => this.extractFileUploadDtoFromResponse(resp)),
      catchError(() => of(null)),
    );
  }

  /** Resolves `FileUploadDto` from file-upload service / gateway envelopes. */
  private extractFileUploadDtoFromResponse(response: unknown): Record<string, unknown> | null {
    for (const key of ['fileUploadDto', 'FileUploadDto'] as const) {
      const hit = this.extractSingleDto(response, key);
      if (hit) {
        return hit;
      }
    }
    const obj = this.toObj(this.parsePossiblyStringifiedJson(response));
    if (!obj) {
      return null;
    }
    const list = obj['fileUploadDtoList'];
    if (Array.isArray(list) && list.length) {
      const first = this.asRecord(list[0]);
      if (first && first['id'] != null) {
        return first;
      }
    }
    const candidates = [obj, this.toObj(obj['data']), this.toObj(obj['body']), this.toObj(obj['payload'])].filter(
      Boolean,
    ) as Record<string, unknown>[];
    for (const c of candidates) {
      if (c['id'] == null) {
        continue;
      }
      if (c['originalFileName'] != null || c['storedFileName'] != null || c['contentType'] != null || c['fileType'] != null) {
        return c;
      }
    }
    return null;
  }

  /**
   * Partial profile update (`EditUserRequest` multipart). Omit file fields when not replacing uploads.
   */
  updateUser(payload: {
    id: number;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    gender: string;
    phoneNumber: string;
    dateOfBirth: string;
    nationalIdNumber?: string;
    nationalIdExpiryDate?: string;
    nationalIdUpload?: File;
    /** Keep existing file-upload row when not replacing the national ID scan. */
    nationalIdUploadId?: number;
    passportNumber?: string;
    passportExpiryDate?: string;
    passportUpload?: File;
    passportUploadId?: number;
  }): Observable<unknown> {
    const form = new FormData();
    this.appendFormValue(form, 'id', payload.id);
    this.appendFormValue(form, 'username', payload.username);
    this.appendFormValue(form, 'email', payload.email);
    this.appendFormValue(form, 'firstName', payload.firstName);
    this.appendFormValue(form, 'lastName', payload.lastName);
    this.appendFormValue(form, 'gender', payload.gender);
    this.appendFormValue(form, 'phoneNumber', payload.phoneNumber);
    this.appendFormValue(form, 'dateOfBirth', payload.dateOfBirth);
    this.appendFormValue(form, 'nationalIdNumber', payload.nationalIdNumber);
    this.appendFormValue(form, 'nationalIdExpiryDate', payload.nationalIdExpiryDate);
    this.appendFormFile(form, 'nationalIdUpload', payload.nationalIdUpload);
    this.appendFormValue(form, 'nationalIdUploadId', payload.nationalIdUploadId);
    this.appendFormValue(form, 'passportNumber', payload.passportNumber);
    this.appendFormValue(form, 'passportExpiryDate', payload.passportExpiryDate);
    this.appendFormFile(form, 'passportUpload', payload.passportUpload);
    this.appendFormValue(form, 'passportUploadId', payload.passportUploadId);
    return this.http.put(`${this.base}/user/update`, form);
  }

  createUserGroup(payload: { name: string; description: string }): Observable<unknown> {
    return this.http.post(`${this.base}/user-group/create`, payload);
  }

  updateUserGroup(payload: { id: number; name: string; description: string }): Observable<unknown> {
    return this.http.put(`${this.base}/user-group/update`, payload);
  }

  deleteUserGroup(id: number): Observable<unknown> {
    return this.http.delete(`${this.base}/user-group/delete/${id}`);
  }

  assignUserRolesToUserGroup(userGroupId: number, userRoleIds: number[]): Observable<unknown> {
    return this.http.post(`${this.base}/user-group/assign-user-roles-to-user-group`, {
      userGroupId,
      userRoleIds,
    });
  }

  createUserRole(payload: { role: string; description: string }): Observable<unknown> {
    return this.http.post(`${this.base}/user-role/create`, payload);
  }

  updateUserRole(payload: { id: number; role: string; description: string }): Observable<unknown> {
    return this.http.put(`${this.base}/user-role/update`, payload);
  }

  deleteUserRole(id: number): Observable<unknown> {
    return this.http.delete(`${this.base}/user-role/delete-by-id/${id}`);
  }

  createUserType(payload: { userTypeName: string; description: string }): Observable<unknown> {
    return this.http.post(`${this.base}/user-type/create`, payload);
  }

  updateUserType(payload: { id: number; userTypeName: string; description: string }): Observable<unknown> {
    return this.http.put(`${this.base}/user-type/update`, payload);
  }

  deleteUserType(id: number): Observable<unknown> {
    return this.http.delete(`${this.base}/user-type/delete-by-id/${id}`);
  }

  createUser(payload: {
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    gender: string;
    dateOfBirth: string;
    phoneNumber: string;
    password: string;
    organizationId?: number;
    branchId?: number;
    /** Location-service address id — only when integrating with an existing address; do not send suburb id here. */
    locationId?: number;
    nationalIdNumber?: string;
    nationalIdExpiryDate?: string;
    nationalIdUpload?: File;
    nationalIdUploadId?: number;
    passportNumber?: string;
    passportExpiryDate?: string;
    passportUpload?: File;
    passportUploadId?: number;
    userTypeName: string;
    userTypeDescription: string;
    addressLine1?: string;
    addressLine2?: string;
    postalCode?: string;
    suburbId?: number;
    /** Optional: existing geo row in Location Service. */
    geoCoordinatesId?: number;
    /** Optional: decimal degrees when not using geoCoordinatesId. */
    geoLatitude?: number;
    geoLongitude?: number;
    preferredLanguage?: string;
    timezone?: string;
    securityQuestion1?: string;
    securityAnswer1?: string;
    securityQuestion2?: string;
    securityAnswer2?: string;
    twoFactorAuthSecret?: string;
    isTwoFactorEnabled?: boolean;
  }): Observable<unknown> {
    const form = new FormData();
    this.appendFormValue(form, 'username', payload.username);
    this.appendFormValue(form, 'email', payload.email);
    this.appendFormValue(form, 'firstName', payload.firstName);
    this.appendFormValue(form, 'lastName', payload.lastName);
    this.appendFormValue(form, 'gender', payload.gender);
    this.appendFormValue(form, 'dateOfBirth', payload.dateOfBirth);
    this.appendFormValue(form, 'phoneNumber', payload.phoneNumber);
    this.appendFormValue(form, 'password', payload.password);
    this.appendFormValue(form, 'organizationId', payload.organizationId);
    this.appendFormValue(form, 'branchId', payload.branchId);
    this.appendFormValue(form, 'locationId', payload.locationId);
    this.appendFormValue(form, 'nationalIdNumber', payload.nationalIdNumber);
    this.appendFormValue(form, 'nationalIdExpiryDate', payload.nationalIdExpiryDate);
    this.appendFormValue(form, 'nationalIdUploadId', payload.nationalIdUploadId);
    this.appendFormFile(form, 'nationalIdUpload', payload.nationalIdUpload);
    this.appendFormValue(form, 'passportNumber', payload.passportNumber);
    this.appendFormValue(form, 'passportExpiryDate', payload.passportExpiryDate);
    this.appendFormValue(form, 'passportUploadId', payload.passportUploadId);
    this.appendFormFile(form, 'passportUpload', payload.passportUpload);
    this.appendFormValue(form, 'userTypeDetails.userTypeName', payload.userTypeName);
    this.appendFormValue(form, 'userTypeDetails.description', payload.userTypeDescription);
    this.appendFormValue(form, 'userAddressDetails.line1', payload.addressLine1);
    this.appendFormValue(form, 'userAddressDetails.line2', payload.addressLine2);
    this.appendFormValue(form, 'userAddressDetails.postalCode', payload.postalCode);
    this.appendFormValue(form, 'userAddressDetails.suburbId', payload.suburbId);
    this.appendFormValue(form, 'userAddressDetails.geoCoordinatesId', payload.geoCoordinatesId);
    this.appendFormValue(form, 'userAddressDetails.latitude', payload.geoLatitude);
    this.appendFormValue(form, 'userAddressDetails.longitude', payload.geoLongitude);
    // Preferences: only send when complete — partial binding would fail CreateUserPreferences validation.
    const lang = payload.preferredLanguage?.trim();
    const tz = payload.timezone?.trim();
    if (lang && tz) {
      form.append('userPreferencesDetails.preferredLanguage', lang);
      form.append('userPreferencesDetails.timezone', tz);
    }
    // Security: backend requires Q1, A1, A2, and non-empty twoFactorAuthSecret if UserSecurityDetails is present.
    // Sending only isTwoFactorEnabled creates a non-null stub and causes 400 — send all or nothing.
    const secQ1 = payload.securityQuestion1?.trim() ?? '';
    const secQ2 = payload.securityQuestion2?.trim() ?? '';
    const secA1 = payload.securityAnswer1?.trim() ?? '';
    const secA2 = payload.securityAnswer2?.trim() ?? '';
    const secSecret = payload.twoFactorAuthSecret?.trim() ?? '';
    if (secQ1 && secA1 && secA2 && secSecret) {
      form.append('userSecurityDetails.securityQuestion_1', secQ1);
      if (secQ2) form.append('userSecurityDetails.securityQuestion_2', secQ2);
      form.append('userSecurityDetails.securityAnswer_1', secA1);
      form.append('userSecurityDetails.securityAnswer_2', secA2);
      form.append('userSecurityDetails.twoFactorAuthSecret', secSecret);
      form.append('userSecurityDetails.isTwoFactorEnabled', payload.isTwoFactorEnabled ? 'true' : 'false');
    }
    return this.http.post(`${this.base}/user/create`, form);
  }

  queryOrganizationsForSelect(): Observable<IdLabelOption[]> {
    const url = `${environment.apiUrl}/ldms-organization-management/v1/backoffice/organization/kyc/queue?page=0&size=500`;
    return this.http.get<unknown>(url).pipe(
      map((resp) => {
        const rows = this.extractOrganizationRows(resp);
        return rows
          .map((r) => {
            const id = Number(r['id'] ?? 0);
            const name = String(r['name'] ?? '').trim();
            return Number.isFinite(id) && id > 0 && name ? { id, label: name } : null;
          })
          .filter((o): o is IdLabelOption => !!o);
      }),
      catchError(() => of([])),
    );
  }

  queryBranchesForOrganization(organizationId: number): Observable<IdLabelOption[]> {
    if (!Number.isFinite(organizationId) || organizationId < 1) return of([]);
    const url = `${environment.apiUrl}/ldms-organization-management/v1/backoffice/organization/${organizationId}`;
    return this.http.get<unknown>(url).pipe(
      map((resp) => {
        const org = this.extractSingleDto(resp, 'organizationDto');
        const branches = Array.isArray(org?.['branchDtoList']) ? (org?.['branchDtoList'] as unknown[]) : [];
        return branches
          .map((b) => {
            const row = this.asRecord(b);
            const id = Number(row?.['id'] ?? 0);
            const name = String(row?.['branchName'] ?? '').trim();
            return Number.isFinite(id) && id > 0 && name ? { id, label: name } : null;
          })
          .filter((o): o is IdLabelOption => !!o);
      }),
      catchError(() => of([])),
    );
  }

  private mapUserRow(row: Record<string, unknown>): UserListRow {
    if (!row) {
      return {
        id: 0,
        username: '',
        email: '',
        name: '—',
        phoneNumber: '',
        gender: '',
        nationalIdNumber: '',
        dateOfBirthLabel: '—',
        emailVerifiedLabel: '—',
        role: '—',
        userGroupId: null,
        accountType: '—',
        status: 'pending',
        statusLabel: 'Unknown',
        createdAtLabel: '—',
        updatedAtLabel: '—',
      };
    }
    const firstName = String(row['firstName'] ?? '').trim();
    const lastName = String(row['lastName'] ?? '').trim();
    const fullName = `${firstName} ${lastName}`.trim();
    const groupDto = this.asRecord(row['userGroupDto']);
    const role = groupDto?.['name'] ?? '—';
    const groupIdRaw = groupDto?.['id'];
    const groupIdParsed = Number(groupIdRaw);
    const userGroupId = Number.isFinite(groupIdParsed) && groupIdParsed > 0 ? groupIdParsed : null;
    const accountType = this.asRecord(row['userTypeDto'])?.['userTypeName'] ?? '—';
    const statusRaw = String(row['entityStatus'] ?? 'ACTIVE').toLowerCase();
    const emailVerified = row['emailVerified'];
    const emailVerifiedLabel =
      emailVerified === true ? 'Yes' : emailVerified === false ? 'No' : '—';
    return {
      id: Number(row['id'] ?? 0),
      username: String(row['username'] ?? '').trim(),
      email: String(row['email'] ?? ''),
      name: fullName || String(row['username'] ?? ''),
      phoneNumber: String(row['phoneNumber'] ?? '').trim(),
      gender: this.formatGenderLabel(row['gender']),
      nationalIdNumber: String(row['nationalIdNumber'] ?? '').trim() || '—',
      dateOfBirthLabel: this.formatIsoDateForDisplay(row['dateOfBirth']),
      emailVerifiedLabel,
      role: String(role ?? '—'),
      userGroupId,
      accountType: String(accountType ?? '—'),
      status: statusRaw,
      statusLabel: this.readableStatus(statusRaw),
      createdAtLabel: this.formatIsoDateTimeForDisplay(row['createdAt']),
      updatedAtLabel: this.formatIsoDateTimeForDisplay(row['updatedAt']),
    };
  }

  private formatGenderLabel(raw: unknown): string {
    const g = String(raw ?? '').trim().toUpperCase();
    if (!g) return '—';
    if (g === 'MALE') return 'Male';
    if (g === 'FEMALE') return 'Female';
    if (g === 'OTHER') return 'Other';
    return g.charAt(0) + g.slice(1).toLowerCase();
  }

  private formatIsoDateForDisplay(value: unknown): string {
    if (value == null || value === '') return '—';
    const d = new Date(String(value));
    if (Number.isNaN(d.getTime())) return '—';
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  private formatIsoDateTimeForDisplay(value: unknown): string {
    if (value == null || value === '') return '—';
    const d = new Date(String(value));
    if (Number.isNaN(d.getTime())) return '—';
    return d.toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private extractOrganizationRows(response: unknown): Record<string, unknown>[] {
    const obj = this.toObj(this.parsePossiblyStringifiedJson(response));
    if (!obj) return [];
    const root = this.toObj(obj['data']) ?? obj;
    const page = this.unwrapPageRecord(root['organizationDtoPage']);
    if (page && Array.isArray(page['content'])) {
      return page['content'] as Record<string, unknown>[];
    }
    const list = root['organizationDtoList'];
    if (Array.isArray(list)) {
      return list.filter((r): r is Record<string, unknown> => !!this.toObj(r));
    }
    const one = this.asRecord(root['organizationDto']);
    return one ? [one] : [];
  }

  private extractPagedResult(response: unknown, dtoPageKey: string): { rows: Record<string, unknown>[]; totalElements: number } {
    const parsed = this.parsePossiblyStringifiedJson(response);
    const obj = this.toObj(parsed);
    if (!obj) {
      return { rows: [], totalElements: 0 };
    }
    const candidates = [
      obj,
      this.toObj(obj['data']),
      this.toObj(obj['body']),
      this.toObj(obj['payload']),
    ].filter(Boolean) as Record<string, unknown>[];
    for (const src of candidates) {
      const page = this.unwrapPageRecord(src[dtoPageKey]);
      if (page && Array.isArray(page['content'])) {
        return {
          rows: (page['content'] as Record<string, unknown>[]),
          totalElements: Number(page['totalElements'] ?? (page['content'] as unknown[]).length),
        };
      }
    }
    const discovered = this.findPageObject(obj);
    if (discovered && Array.isArray(discovered['content'])) {
      return {
        rows: discovered['content'] as Record<string, unknown>[],
        totalElements: Number(discovered['totalElements'] ?? (discovered['content'] as unknown[]).length),
      };
    }
    return { rows: [], totalElements: 0 };
  }

  /** Spring Page JSON or a stringified Page object from gateways. */
  private unwrapPageRecord(value: unknown): Record<string, unknown> | null {
    if (value === null || value === undefined) {
      return null;
    }
    if (typeof value === 'string') {
      const inner = this.parsePossiblyStringifiedJson(value.trim());
      return this.toObj(inner);
    }
    return this.toObj(value);
  }

  /**
   * Resolves a single DTO from API/gateway envelopes ({@code data}, {@code body}, stringified JSON, etc.).
   */
  private extractSingleDto(response: unknown, dtoKey: string): Record<string, unknown> | null {
    const parsed = this.parsePossiblyStringifiedJson(response);
    const obj = this.toObj(parsed);
    if (!obj) {
      return null;
    }
    const candidates = [
      obj,
      this.toObj(obj['data']),
      this.toObj(obj['body']),
      this.toObj(obj['payload']),
    ].filter(Boolean) as Record<string, unknown>[];
    for (const wrapped of candidates) {
      const hit = this.unwrapDtoValue(wrapped[dtoKey]);
      if (hit) {
        return hit;
      }
    }
    return null;
  }

  private unwrapDtoValue(value: unknown): Record<string, unknown> | null {
    const direct = this.asRecord(value);
    if (direct) {
      return direct;
    }
    if (typeof value === 'string') {
      const inner = this.toObj(this.parsePossiblyStringifiedJson(value.trim()));
      if (inner) {
        return inner;
      }
    }
    return null;
  }

  private statusToEntityStatus(raw: string | undefined): string {
    const t = String(raw ?? '').trim().toUpperCase();
    if (t === 'ACTIVE' || t === 'INACTIVE' || t === 'DELETED') {
      return t;
    }
    return '';
  }

  private readableStatus(raw: string): string {
    if (raw === 'active') return 'Active';
    if (raw === 'inactive') return 'Inactive';
    if (raw === 'deleted') return 'Deleted';
    return 'Pending';
  }

  private normalizedFilter(raw: string | undefined): string {
    return String(raw ?? '').trim();
  }

  private appendFormValue(form: FormData, key: string, value: unknown): void {
    if (value === null || value === undefined) {
      return;
    }
    if (typeof value === 'boolean') {
      form.append(key, value ? 'true' : 'false');
      return;
    }
    const str = String(value).trim();
    if (!str) {
      return;
    }
    form.append(key, str);
  }

  private appendFormFile(form: FormData, key: string, file: File | undefined): void {
    if (!file) return;
    form.append(key, file, file.name);
  }

  private toObj(value: unknown): Record<string, unknown> | null {
    return value !== null && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }

  private asRecord(value: unknown): Record<string, unknown> | null {
    return this.toObj(value);
  }

  private parsePossiblyStringifiedJson(value: unknown): unknown {
    if (typeof value !== 'string') {
      return value;
    }
    const trimmed = value.trim();
    if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
      return value;
    }
    try {
      return JSON.parse(trimmed);
    } catch {
      return value;
    }
  }

  private emptyOnNotFound(err: unknown): Observable<null> {
    if (err instanceof HttpErrorResponse && err.status === 404) {
      return of(null);
    }
    return throwError(() => err);
  }

  private emptyPageOnNotFound(
    err: unknown,
  ): Observable<{ rows: Record<string, unknown>[]; totalElements: number }> {
    if (err instanceof HttpErrorResponse && err.status === 404) {
      return of({ rows: [], totalElements: 0 });
    }
    return throwError(() => err);
  }

  private emptyUsersPageOnNotFound(err: unknown): Observable<{ rows: UserListRow[]; totalElements: number }> {
    if (err instanceof HttpErrorResponse && err.status === 404) {
      return of({ rows: [], totalElements: 0 });
    }
    return throwError(() => err);
  }

  private emptyProfileBundleOnNotFound(err: unknown): Observable<UserProfileBundle> {
    if (err instanceof HttpErrorResponse && err.status === 404) {
      return of({
        user: null,
        account: null,
        security: null,
        address: null,
        password: null,
      });
    }
    return throwError(() => err);
  }
}
