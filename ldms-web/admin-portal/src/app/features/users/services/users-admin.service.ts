import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, from, map, of, switchMap, throwError } from 'rxjs';
import { extractFileUploadDtoFromResponse } from '../../../core/utils/file-upload-dto-extract.util';
import { ldmsApiUrl, ldmsServiceUrl } from '../../../core/utils/api-url.util';
import { collectUploadIdsFromJsonTree } from '../../../shared/utils/collect-upload-ids';
import { resolveFilePreview } from '../../../shared/utils/file-upload-preview';
import { LxExportFormat, exportFormatToApiParam } from '../../../shared/utils/lx-export.util';

export interface UsersQuery {
  page: number;
  size: number;
  searchQuery: string;
  columnFilters: Record<string, string>;
  /** When set, restricts results to users in this primary user group (ldms-user-management filter). */
  userGroupId?: number | null;
  /** When set, restricts results to users linked to this organisation. */
  organizationId?: number | null;
  /** When set, restricts results to users linked to this branch. */
  branchId?: number | null;
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
  /** True when email is not verified and the account was created more than 24 hours ago. */
  canResendVerificationEmail: boolean;
  role: string;
  /** When set, row actions can deep-link to this group's role assignment screen. */
  userGroupId: number | null;
  accountType: string;
  status: string;
  statusLabel: string;
  createdAtLabel: string;
  updatedAtLabel: string;
  /** Organisation signup KYC approver eligibility (admin users without organisation only). */
  kycApproverEligibleLabel: string;
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
  private static readonly EXPORT_PAGE_SIZE = 10_000;

  /** Admin portal: {@code backoffice} (JWT only). {@code frontend} enforces per-endpoint {@code @PreAuthorize} roles. */
  private readonly base = ldmsApiUrl('/ldms-user-management/v1/backoffice');
  private readonly fileUploadBase = ldmsApiUrl('/ldms-file-upload-service/v1/backoffice/file-upload');

  constructor(private readonly http: HttpClient) {}

  exportUsers(q: UsersQuery, format: LxExportFormat): Observable<Blob> {
    const body = this.buildUsersFilterBody({
      ...q,
      page: 0,
      size: UsersAdminService.EXPORT_PAGE_SIZE,
    });
    return this.postExport('user', body, format);
  }

  exportUserRoles(q: UsersQuery, format: LxExportFormat): Observable<Blob> {
    const body = this.buildUserRolesFilterBody({
      ...q,
      page: 0,
      size: UsersAdminService.EXPORT_PAGE_SIZE,
    });
    return this.postExport('user-role', body, format);
  }

  exportUserGroups(q: UsersQuery, format: LxExportFormat): Observable<Blob> {
    const body = this.buildUserGroupsFilterBody({
      ...q,
      page: 0,
      size: UsersAdminService.EXPORT_PAGE_SIZE,
    });
    return this.postExport('user-group', body, format);
  }

  exportUserTypes(q: UsersQuery, format: LxExportFormat): Observable<Blob> {
    const body = this.buildUserTypesFilterBody({
      ...q,
      page: 0,
      size: UsersAdminService.EXPORT_PAGE_SIZE,
    });
    return this.postExport('user-type', body, format);
  }

  /**
   * Users linked to an organisation (`organization_id` on user row). Prefer this over paged filters for org detail.
   */
  queryUsersForOrganization(organizationId: number): Observable<{ rows: UserListRow[]; totalElements: number }> {
    if (!Number.isFinite(organizationId) || organizationId < 1) {
      return of({ rows: [], totalElements: 0 });
    }
    return this.http.get<unknown>(`${this.base}/user/find-by-organization-id/${organizationId}`).pipe(
      map((resp) => {
        const obj = this.toObj(this.parsePossiblyStringifiedJson(resp));
        if (obj && obj['success'] === false) {
          return { rows: [], totalElements: 0 };
        }
        const list = this.extractUserDtoList(resp);
        const safeRows = list.filter(
          (r): r is Record<string, unknown> => r !== null && typeof r === 'object' && !Array.isArray(r),
        );
        const rows = safeRows.map((r) => this.mapUserRow(r));
        return { rows, totalElements: rows.length };
      }),
      catchError((err) => this.emptyUsersPageOnNotFound(err)),
    );
  }

  queryUsers(q: UsersQuery): Observable<{ rows: UserListRow[]; totalElements: number }> {
    const body = this.buildUsersFilterBody(q);
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
    const body = this.buildUserRolesFilterBody(q);
    return this.http.post<unknown>(`${this.base}/user-role/find-by-multiple-filters`, body).pipe(
      map((resp) => this.extractPagedResult(resp, 'userRoleDtoPage')),
      catchError((err) => this.emptyPageOnNotFound(err)),
    );
  }

  queryUserGroups(q: UsersQuery): Observable<{ rows: Record<string, unknown>[]; totalElements: number }> {
    const body = this.buildUserGroupsFilterBody(q);
    return this.http
      .post<unknown>(`${this.base}/user-group/find-by-multiple-filters?_t=${Date.now()}`, body)
      .pipe(
      map((resp) =>
        this.extractPagedResultWithAlternateKeys(resp, 'userGroupDtoPage', [
          'userGroupPage',
          'user_group_dto_page',
          'page',
        ]),
      ),
      catchError((err) => this.emptyPageOnNotFound(err)),
    );
  }

  queryUserTypes(q: UsersQuery): Observable<{ rows: Record<string, unknown>[]; totalElements: number }> {
    const body = this.buildUserTypesFilterBody(q);
    return this.http.post<unknown>(`${this.base}/user-type/find-by-multiple-filters`, body).pipe(
      map((resp) =>
        this.extractPagedResultWithAlternateKeys(resp, 'userTypeDtoPage', ['userTypePage', 'user_type_dto_page', 'page']),
      ),
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

  /**
   * Like {@link extractPagedResult} but tries extra page property names per resource. Required when gateways add a
   * `data` object that does not hold the Spring `Page`, while `*DtoPage` remains a sibling on the outer envelope.
   */
  private extractPagedResultWithAlternateKeys(
    response: unknown,
    primaryDtoPageKey: string,
    alternateKeys: string[],
  ): { rows: Record<string, unknown>[]; totalElements: number } {
    const empty = { rows: [] as Record<string, unknown>[], totalElements: 0 };
    const parsed = this.parsePossiblyStringifiedJson(response);
    const obj = this.toObj(parsed);
    if (!obj) {
      return empty;
    }
    const pageKeys = [primaryDtoPageKey, ...alternateKeys.filter((k) => k && k !== primaryDtoPageKey)];
    const candidates = [
      obj,
      this.toObj(this.parsePossiblyStringifiedJson(obj['data'])),
      this.toObj(this.parsePossiblyStringifiedJson(obj['body'])),
      this.toObj(this.parsePossiblyStringifiedJson(obj['payload'])),
    ].filter(Boolean) as Record<string, unknown>[];
    for (const src of candidates) {
      for (const key of pageKeys) {
        const page = this.unwrapPageRecord(src[key]);
        if (page && Array.isArray(page['content'])) {
          return {
            rows: page['content'] as Record<string, unknown>[],
            totalElements: Number(page['totalElements'] ?? (page['content'] as unknown[]).length),
          };
        }
      }
      const listKey = primaryDtoPageKey.replace(/Page$/, 'List');
      const list = src[listKey];
      if (Array.isArray(list)) {
        return { rows: list as Record<string, unknown>[], totalElements: list.length };
      }
      for (const [key, val] of Object.entries(src)) {
        if (key.toLowerCase().endsWith('dtopage')) {
          const page = this.unwrapPageRecord(val);
          if (page && Array.isArray(page['content'])) {
            return {
              rows: page['content'] as Record<string, unknown>[],
              totalElements: Number(page['totalElements'] ?? (page['content'] as unknown[]).length),
            };
          }
        }
        if (key.toLowerCase().endsWith('dtolist') && Array.isArray(val)) {
          return { rows: val as Record<string, unknown>[], totalElements: val.length };
        }
      }
    }
    const discovered = this.findPageObject(obj);
    if (discovered && Array.isArray(discovered['content'])) {
      return {
        rows: discovered['content'] as Record<string, unknown>[],
        totalElements: Number(discovered['totalElements'] ?? (discovered['content'] as unknown[]).length),
      };
    }
    return empty;
  }

  getUserGroupById(id: number): Observable<Record<string, unknown> | null> {
    return this.http
      .get<unknown>(`${this.base}/user-group/find-by-id/${id}`, {
        params: { _t: String(Date.now()) },
      })
      .pipe(
        map((resp) => this.extractSingleDto(resp, 'userGroupDto')),
        catchError((err) => this.emptyOnNotFound(err)),
      );
  }

  getUserProfileBundle(userId: number): Observable<UserProfileBundle> {
    return this.http.get<unknown>(`${this.base}/user/find-by-id/${userId}`).pipe(
      map((resp) => this.mapUserResponseToProfileBundle(resp)),
      catchError((err) => this.emptyProfileBundleOnNotFound(err)),
    );
  }

  /** Signed-in user's full profile ({@code GET /backoffice/user/me}). */
  getMyAccountProfileBundle(): Observable<UserProfileBundle> {
    return this.http.get<unknown>(`${this.base}/user/me`).pipe(
      map((resp) => this.mapUserResponseToProfileBundle(resp)),
      catchError((err) => this.emptyProfileBundleOnNotFound(err)),
    );
  }

  /** Backoffice lookup by login username (permitAll — no admin role required). */
  getUserProfileBundleByUsername(username: string): Observable<UserProfileBundle> {
    const trimmed = username.trim();
    if (!trimmed) {
      return of({
        user: null,
        account: null,
        security: null,
        address: null,
        password: null,
      });
    }
    return this.http
      .get<unknown>(`${this.base}/user/find-by-username/${encodeURIComponent(trimmed)}`)
      .pipe(
        map((resp) => this.mapUserResponseToProfileBundle(resp)),
        catchError((err) => this.emptyProfileBundleOnNotFound(err)),
      );
  }

  /** Fills account/security when nested DTOs are absent on the user payload. */
  enrichUserProfileBundle(bundle: UserProfileBundle, userId?: number): Observable<UserProfileBundle> {
    const uid = Number(userId ?? bundle.user?.['id'] ?? 0);
    if (!Number.isFinite(uid) || uid <= 0) {
      return of(bundle);
    }
    const accountFromUser = this.extractNestedDto(bundle.user, ['userAccountDto', 'userAccount']);
    const needAccount = !bundle.account && !accountFromUser;
    const needSecurity = !bundle.security && !this.extractNestedDto(bundle.user, ['userSecurityDto', 'userSecurity']);
    if (!needAccount && !needSecurity) {
      return of({
        ...bundle,
        account: bundle.account ?? accountFromUser,
        security:
          bundle.security ?? this.extractNestedDto(bundle.user, ['userSecurityDto', 'userSecurity']),
        address: bundle.address ?? this.extractNestedDto(bundle.user, ['addressDto', 'address']),
      });
    }
    return forkJoin({
      account: needAccount ? this.findUserAccountByUserId(uid) : of(accountFromUser ?? bundle.account),
      security: needSecurity
        ? this.findUserSecurityByUserId(uid)
        : of(bundle.security ?? this.extractNestedDto(bundle.user, ['userSecurityDto', 'userSecurity'])),
    }).pipe(
      map(({ account, security }) => ({
        ...bundle,
        account: bundle.account ?? accountFromUser ?? account,
        security: bundle.security ?? security,
        address: bundle.address ?? this.extractNestedDto(bundle.user, ['addressDto', 'address']),
      })),
      catchError(() =>
        of({
          ...bundle,
          account: bundle.account ?? accountFromUser,
          security:
            bundle.security ?? this.extractNestedDto(bundle.user, ['userSecurityDto', 'userSecurity']),
          address: bundle.address ?? this.extractNestedDto(bundle.user, ['addressDto', 'address']),
        }),
      ),
    );
  }

  /** Resolves the account row for My Account / profile edit (embedded DTO or lookup by user id). */
  resolveAccountRecord(
    user: Record<string, unknown> | null,
    userId: number,
  ): Observable<Record<string, unknown> | null> {
    const embedded = this.extractNestedDto(user, ['userAccountDto', 'userAccount']);
    if (embedded && Number(embedded['id'] ?? 0) > 0) {
      return of(embedded);
    }
    if (!Number.isFinite(userId) || userId <= 0) {
      return of(null);
    }
    return this.findUserAccountByUserId(userId);
  }

  /** Resolves the address row from the profile bundle or nested user DTO. */
  resolveAddressRecord(user: Record<string, unknown> | null, address: Record<string, unknown> | null): Record<string, unknown> | null {
    if (address && Number(address['id'] ?? 0) > 0) {
      return address;
    }
    const embedded = this.extractNestedDto(user, ['addressDto', 'address']);
    return embedded && Number(embedded['id'] ?? 0) > 0 ? embedded : null;
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
    securityQuestion_2: string;
    securityAnswer_2: string;
    twoFactorAuthSecret: string;
    isTwoFactorEnabled: boolean;
  }): Observable<unknown> {
    return this.http.put(`${this.base}/user-security/update`, payload);
  }

  createUserSecurity(payload: {
    userId: number;
    securityQuestion_1: string;
    securityAnswer_1: string;
    securityQuestion_2: string;
    securityAnswer_2: string;
    twoFactorAuthSecret: string;
    isTwoFactorEnabled: boolean;
  }): Observable<unknown> {
    return this.http.post(`${this.base}/user-security/create`, payload);
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

  private mapUserResponseToProfileBundle(response: unknown): UserProfileBundle {
    const user = this.extractUserDtoFromFindByIdResponse(response);
    return {
      user,
      account: this.extractNestedDto(user, ['userAccountDto', 'userAccount']),
      security: this.extractNestedDto(user, ['userSecurityDto', 'userSecurity']),
      address: this.extractNestedDto(user, ['addressDto', 'address']),
      password: this.extractNestedDto(user, ['userPasswordDto', 'userPassword']),
    };
  }

  private extractNestedDto(
    parent: Record<string, unknown> | null | undefined,
    keys: string[],
  ): Record<string, unknown> | null {
    if (!parent) {
      return null;
    }
    for (const key of keys) {
      const hit = this.asRecord(parent[key]);
      if (hit) {
        return hit;
      }
    }
    return null;
  }

  /** Resolves `UserDto` from `UserResponse` across common gateway / naming variants. */
  private extractUserDtoFromFindByIdResponse(response: unknown): Record<string, unknown> | null {
    const parsed = this.parsePossiblyStringifiedJson(response);
    const envelope = this.toObj(parsed);
    if (envelope && envelope['success'] === false) {
      return null;
    }
    for (const key of ['userDto', 'user', 'UserDto'] as const) {
      const hit = this.extractSingleDto(response, key);
      if (hit) {
        return hit;
      }
    }
    if (envelope && this.looksLikeUserRecord(envelope)) {
      return envelope;
    }
    const candidates = [
      envelope,
      this.toObj(envelope?.['data']),
      this.toObj(envelope?.['body']),
      this.toObj(envelope?.['payload']),
    ].filter(Boolean) as Record<string, unknown>[];
    for (const wrapped of candidates) {
      if (this.looksLikeUserRecord(wrapped)) {
        return wrapped;
      }
    }
    return null;
  }

  private looksLikeUserRecord(value: Record<string, unknown>): boolean {
    return (
      value['id'] != null &&
      (value['firstName'] != null ||
        value['lastName'] != null ||
        value['username'] != null ||
        value['email'] != null)
    );
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
      map((resp) => extractFileUploadDtoFromResponse(resp)),
      catchError(() => of(null)),
    );
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
    addressLine1?: string;
    addressLine2?: string;
    postalCode?: string;
    suburbId?: number;
    geoCoordinatesId?: number;
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
    this.appendFormValue(form, 'userAddressDetails.line1', payload.addressLine1);
    this.appendFormValue(form, 'userAddressDetails.line2', payload.addressLine2);
    this.appendFormValue(form, 'userAddressDetails.postalCode', payload.postalCode);
    this.appendFormValue(form, 'userAddressDetails.suburbId', payload.suburbId);
    this.appendFormValue(form, 'userAddressDetails.geoCoordinatesId', payload.geoCoordinatesId);
    return this.http.put(`${this.base}/user/update`, form);
  }

  /**
   * After organisation overview contact fields change, push name/email/phone onto the linked portal user
   * so org directory and user-management stay aligned.
   */
  syncOrganizationContactPersonUser(
    userId: number,
    fields: {
      firstName?: string;
      lastName?: string;
      email?: string;
      phoneNumber?: string;
    },
  ): Observable<unknown> {
    if (!Number.isFinite(userId) || userId <= 0) {
      return of(null);
    }
    return this.getUserProfileBundle(userId).pipe(
      switchMap((bundle) => {
        const u = bundle.user;
        if (!u) {
          return throwError(() => new Error('Contact person user not found.'));
        }
        const firstName = (fields.firstName ?? u['firstName'] ?? '').toString().trim();
        const lastName = (fields.lastName ?? u['lastName'] ?? '').toString().trim();
        const previousEmail = String(u['email'] ?? '').trim();
        const email = (fields.email ?? u['email'] ?? '').toString().trim();
        const phoneNumber = (fields.phoneNumber ?? u['phoneNumber'] ?? '').toString().trim();
        const username =
          email && email.toLowerCase() !== previousEmail.toLowerCase()
            ? email
            : String(u['username'] ?? email).trim();
        let gender = String(u['gender'] ?? '').trim().toUpperCase();
        if (!gender || gender === 'OTHER') {
          gender = 'PREFER_NOT_TO_SAY';
        }
        const dateOfBirth = this.apiDateToInputString(u['dateOfBirth']) || '1990-01-01';
        const nationalIdUploadId = Number(u['nationalIdUploadId'] ?? 0);
        const passportUploadId = Number(u['passportUploadId'] ?? 0);
        return this.updateUser({
          id: userId,
          username,
          email,
          firstName,
          lastName,
          gender,
          phoneNumber,
          dateOfBirth,
          nationalIdNumber: String(u['nationalIdNumber'] ?? '').trim() || undefined,
          nationalIdUploadId:
            Number.isFinite(nationalIdUploadId) && nationalIdUploadId > 0 ? nationalIdUploadId : undefined,
          passportNumber: String(u['passportNumber'] ?? '').trim() || undefined,
          passportUploadId: Number.isFinite(passportUploadId) && passportUploadId > 0 ? passportUploadId : undefined,
        });
      }),
    );
  }

  /** Sets organisation KYC approver eligibility (admin users without organisation only). */
  setOrganizationKycApprover(userId: number, enabled: boolean): Observable<unknown> {
    const params = new HttpParams().set('enabled', String(enabled));
    return this.http.put(`${this.base}/user/${userId}/organization-kyc-approver`, null, { params });
  }

  deleteUser(id: number): Observable<unknown> {
    return this.http.delete(`${this.base}/user/delete-by-id/${id}`);
  }

  /** Resends the email verification link (unverified users created more than 24 hours ago). */
  resendVerificationEmail(email: string): Observable<unknown> {
    const trimmed = email.trim();
    const params = new HttpParams().set('email', trimmed);
    return this.http.post(`${this.base}/user/resend-verification-link`, null, { params });
  }

  /** Whether admin may resend verification for this user record. */
  canResendVerificationEmail(emailVerified: unknown, createdAt: unknown): boolean {
    if (emailVerified === true) {
      return false;
    }
    const created = this.parseApiDateTime(createdAt);
    if (!created) {
      return false;
    }
    const hoursMs = 24 * 60 * 60 * 1000;
    return Date.now() - created.getTime() >= hoursMs;
  }

  parseApiDateTime(value: unknown): Date | null {
    if (value == null || value === '') {
      return null;
    }
    if (typeof value === 'string') {
      const d = new Date(value.trim());
      return Number.isNaN(d.getTime()) ? null : d;
    }
    if (Array.isArray(value) && value.length >= 3) {
      const y = Number(value[0]);
      const m = Number(value[1]);
      const day = Number(value[2]);
      const h = value.length > 3 ? Number(value[3]) : 0;
      const min = value.length > 4 ? Number(value[4]) : 0;
      const sec = value.length > 5 ? Number(value[5]) : 0;
      if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(day)) {
        return null;
      }
      const d = new Date(y, m - 1, day, h, min, sec);
      return Number.isNaN(d.getTime()) ? null : d;
    }
    return null;
  }

  /** `yyyy-MM-dd` for user update payloads. */
  private apiDateToInputString(value: unknown): string {
    const d = this.parseApiDateTime(value);
    if (!d) {
      return '';
    }
    return d.toISOString().slice(0, 10);
  }

  /** True when the API envelope reports failure (often HTTP 200 with success=false). */
  isUserMutationFailure(resp: unknown): boolean {
    if (resp === null || typeof resp !== 'object') {
      return false;
    }
    const r = resp as Record<string, unknown>;
    if (r['success'] === false || r['isSuccess'] === false) {
      return true;
    }
    const statusCode = r['statusCode'];
    return typeof statusCode === 'number' && statusCode >= 400;
  }

  formatUserMutationError(resp: unknown, fallback: string): string {
    if (resp !== null && typeof resp === 'object') {
      const r = resp as Record<string, unknown>;
      const messages = r['errorMessages'];
      if (Array.isArray(messages) && messages.length > 0) {
        return messages.map((m) => String(m)).join(' ');
      }
      if (typeof r['message'] === 'string' && r['message'].trim()) {
        return r['message'].trim();
      }
    }
    return fallback;
  }

  /** User-facing message from a mutation response or HTTP error body. */
  formatUserMutationMessage(resp: unknown, fallback: string): string {
    return this.formatUserMutationError(resp, fallback);
  }

  createUserGroup(payload: { name: string; description: string }): Observable<unknown> {
    return this.http.post(`${this.base}/user-group/create`, payload);
  }

  updateUserGroup(payload: { id: number; name: string; description: string }): Observable<unknown> {
    return this.http.put(`${this.base}/user-group/update`, payload);
  }

  deleteUserGroup(id: number): Observable<unknown> {
    return this.http.delete(`${this.base}/user-group/delete-by-id/${id}`);
  }

  assignUserRolesToUserGroup(userGroupId: number, userRoleIds: number[]): Observable<unknown> {
    return this.http.post(`${this.base}/user-group/assign-user-roles-to-user-group`, {
      userGroupId,
      userRoleIds,
    });
  }

  /** Unlinks catalog roles from this group only; roles remain for other groups. */
  removeUserRolesFromUserGroup(userGroupId: number, userRoleIds: number[]): Observable<unknown> {
    return this.http.post<unknown>(`${this.base}/user-group/remove-user-roles-from-user-group`, {
      userGroupId,
      userRoleIds,
    });
  }

  /** Sets the user's primary user group (server replaces any previous assignment). */
  addUserToUserGroup(userId: number, userGroupId: number): Observable<unknown> {
    return this.http.post(`${this.base}/user-group/add-user-group-to-user`, { userId, userGroupId });
  }

  /**
   * Assigns users to a group as their primary group. Users previously in another group are moved;
   * the response includes refreshed member counts for every affected group.
   */
  addUsersToUserGroup(userGroupId: number, userIds: number[]): Observable<unknown> {
    return this.http.post(`${this.base}/user-group/add-users-to-user-group`, { userGroupId, userIds });
  }

  /** Unlinks user(s) from this group (clears the user's primary group when it matches). */
  removeUsersFromUserGroup(userGroupId: number, userIds: number[]): Observable<unknown> {
    return this.http.post(`${this.base}/user-group/remove-users-from-user-group`, { userGroupId, userIds });
  }

  /** Reads {@code userGroupDto} / {@code userGroupDtoList} member counts from a mutation response. */
  extractUserGroupMemberCounts(resp: unknown): Record<number, { users: number; roles: number }> {
    const counts: Record<number, { users: number; roles: number }> = {};
    const parsed = this.parsePossiblyStringifiedJson(resp);
    const roots: Record<string, unknown>[] = [];
    const rootObj = this.asRecord(parsed);
    if (rootObj) {
      roots.push(rootObj);
      const data = this.asRecord(this.parsePossiblyStringifiedJson(rootObj['data']));
      if (data) {
        roots.push(data);
      }
    }
    for (const root of roots) {
      const single = this.asRecord(root['userGroupDto']);
      if (single) {
        this.putUserGroupMemberCount(counts, single);
      }
      const list = root['userGroupDtoList'];
      if (Array.isArray(list)) {
        for (const item of list) {
          const dto = this.asRecord(item);
          if (dto) {
            this.putUserGroupMemberCount(counts, dto);
          }
        }
      }
    }
    return counts;
  }

  private putUserGroupMemberCount(
    counts: Record<number, { users: number; roles: number }>,
    dto: Record<string, unknown>,
  ): void {
    const id = Number(dto['id'] ?? 0);
    if (!Number.isFinite(id) || id < 1) {
      return;
    }
    const usersRaw = dto['userMemberCount'] ?? dto['user_member_count'];
    const rolesRaw = dto['userRoleMemberCount'] ?? dto['user_role_member_count'];
    const usersParsed = Number(usersRaw);
    const rolesParsed = Number(rolesRaw);
    counts[id] = {
      users: Number.isFinite(usersParsed) && usersParsed >= 0 ? Math.trunc(usersParsed) : 0,
      roles: Number.isFinite(rolesParsed) && rolesParsed >= 0 ? Math.trunc(rolesParsed) : 0,
    };
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
    organizationKycApprover?: boolean;
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
    if (payload.organizationKycApprover) {
      form.append('organizationKycApprover', 'true');
    }
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
      // Always send Q2 key to make the field explicitly editable (add/update/clear).
      form.append('userSecurityDetails.securityQuestion_2', secQ2);
      form.append('userSecurityDetails.securityAnswer_1', secA1);
      form.append('userSecurityDetails.securityAnswer_2', secA2);
      form.append('userSecurityDetails.twoFactorAuthSecret', secSecret);
      form.append('userSecurityDetails.isTwoFactorEnabled', payload.isTwoFactorEnabled ? 'true' : 'false');
    }
    return this.http.post(`${this.base}/user/create`, form);
  }

  queryOrganizationsForSelect(): Observable<IdLabelOption[]> {
    const url = ldmsServiceUrl('organization-management', 'organization', 'find-by-multiple-filters', 'backoffice');
    return this.http
      .post<unknown>(url, {
        page: 0,
        size: 500,
        searchValue: '',
        name: '',
      })
      .pipe(
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
    const url = ldmsServiceUrl('organization-management', 'organization', String(organizationId), 'backoffice');
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

  /** Maps a raw user DTO (e.g. from find-by-id) to a table row. */
  mapUserRowFromRecord(row: Record<string, unknown>): UserListRow {
    return this.mapUserRow(row);
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
        canResendVerificationEmail: false,
        role: '—',
        userGroupId: null,
        accountType: '—',
        status: 'pending',
        statusLabel: 'Unknown',
        createdAtLabel: '—',
        updatedAtLabel: '—',
        kycApproverEligibleLabel: '—',
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
    const canResendVerificationEmail = this.canResendVerificationEmail(emailVerified, row['createdAt']);
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
      canResendVerificationEmail,
      role: String(role ?? '—'),
      userGroupId,
      accountType: String(accountType ?? '—'),
      status: statusRaw,
      statusLabel: this.readableStatus(statusRaw),
      createdAtLabel: this.formatIsoDateTimeForDisplay(row['createdAt']),
      updatedAtLabel: this.formatIsoDateTimeForDisplay(row['updatedAt']),
      kycApproverEligibleLabel: this.formatKycApproverEligibleLabel(row),
    };
  }

  private formatKycApproverEligibleLabel(row: Record<string, unknown>): string {
    const orgId = Number(row['organizationId'] ?? 0);
    if (Number.isFinite(orgId) && orgId > 0) {
      return 'Org user';
    }
    return this.isTruthyOrganizationKycApprover(row['organizationKycApprover']) ? 'Eligible' : 'Not eligible';
  }

  private isTruthyOrganizationKycApprover(raw: unknown): boolean {
    if (raw === true) {
      return true;
    }
    if (typeof raw === 'string') {
      return raw.trim().toLowerCase() === 'true';
    }
    return false;
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

  private extractUserDtoList(response: unknown): Record<string, unknown>[] {
    const parsed = this.parsePossiblyStringifiedJson(response);
    const obj = this.toObj(parsed);
    if (!obj) {
      return [];
    }
    const candidates = [
      obj,
      this.toObj(this.parsePossiblyStringifiedJson(obj['data'])),
      this.toObj(this.parsePossiblyStringifiedJson(obj['body'])),
    ].filter(Boolean) as Record<string, unknown>[];
    for (const src of candidates) {
      const list = src['userDtoList'];
      if (Array.isArray(list)) {
        return list.filter(
          (r): r is Record<string, unknown> => r !== null && typeof r === 'object' && !Array.isArray(r),
        );
      }
    }
    return [];
  }

  private extractPagedResult(response: unknown, dtoPageKey: string): { rows: Record<string, unknown>[]; totalElements: number } {
    const parsed = this.parsePossiblyStringifiedJson(response);
    const obj = this.toObj(parsed);
    if (!obj) {
      return { rows: [], totalElements: 0 };
    }
    const candidates = [
      obj,
      this.toObj(this.parsePossiblyStringifiedJson(obj['data'])),
      this.toObj(this.parsePossiblyStringifiedJson(obj['body'])),
      this.toObj(this.parsePossiblyStringifiedJson(obj['payload'])),
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
    const t = String(raw ?? '').trim();
    if (!t) {
      return '';
    }
    const upper = t.toUpperCase();
    if (upper === 'ACTIVE' || upper === 'INACTIVE' || upper === 'DELETED' || upper === 'SUSPENDED') {
      return upper;
    }
    const lower = t.toLowerCase();
    if (lower === 'active') {
      return 'ACTIVE';
    }
    if (lower === 'inactive') {
      return 'INACTIVE';
    }
    if (lower === 'deleted') {
      return 'DELETED';
    }
    if (lower === 'suspended') {
      return 'SUSPENDED';
    }
    return '';
  }

  private readableStatus(raw: string): string {
    if (raw === 'active') return 'Active';
    if (raw === 'inactive') return 'Inactive';
    if (raw === 'deleted') return 'Deleted';
    if (raw === 'suspended') return 'Suspended';
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

  private buildUsersFilterBody(q: UsersQuery): Record<string, unknown> {
    const firstName = this.normalizedFilter(q.columnFilters['firstName']);
    const lastName = this.normalizedFilter(q.columnFilters['lastName']);
    const username = this.normalizedFilter(q.columnFilters['username']);
    const email = this.normalizedFilter(q.columnFilters['email']);
    const phoneNumber = this.normalizedFilter(q.columnFilters['phoneNumber']);
    const nationalIdNumber = this.normalizedFilter(q.columnFilters['nationalIdNumber']);
    const passportNumber = this.normalizedFilter(q.columnFilters['passportNumber']);
    const entityStatus = this.statusToEntityStatus(q.columnFilters['statusLabel']);
    const gid = q.userGroupId;
    const userGroupId =
      gid != null && Number.isFinite(gid) && gid > 0 ? Math.trunc(gid) : null;
    const oid = q.organizationId;
    const organizationId =
      oid != null && Number.isFinite(oid) && oid > 0 ? Math.trunc(oid) : null;
    const bid = q.branchId;
    const branchId = bid != null && Number.isFinite(bid) && bid > 0 ? Math.trunc(bid) : null;
    return {
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
      ...(userGroupId != null ? { userGroupId } : {}),
      ...(organizationId != null ? { organizationId } : {}),
      ...(branchId != null ? { branchId } : {}),
    };
  }

  private buildUserRolesFilterBody(q: UsersQuery): Record<string, unknown> {
    const role = this.normalizedFilter(q.columnFilters['role']);
    const description = this.normalizedFilter(q.columnFilters['description']);
    return {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery.trim(),
      ...(role ? { role } : {}),
      ...(description ? { description } : {}),
    };
  }

  private buildUserGroupsFilterBody(q: UsersQuery): Record<string, unknown> {
    const name = this.normalizedFilter(q.columnFilters['name']);
    const description = this.normalizedFilter(q.columnFilters['description']);
    return {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery.trim(),
      ...(name ? { name } : {}),
      ...(description ? { description } : {}),
    };
  }

  private buildUserTypesFilterBody(q: UsersQuery): Record<string, unknown> {
    const userTypeName = this.normalizedFilter(q.columnFilters['userTypeName']);
    const description = this.normalizedFilter(q.columnFilters['description']);
    return {
      page: q.page,
      size: q.size,
      searchValue: q.searchQuery.trim(),
      ...(userTypeName ? { userTypeName } : {}),
      ...(description ? { description } : {}),
    };
  }

  private postExport(
    resource: string,
    body: Record<string, unknown>,
    format: LxExportFormat,
  ): Observable<Blob> {
    return this.http
      .post(`${this.base}/${resource}/export`, body, {
        params: new HttpParams().set('format', exportFormatToApiParam(format)),
        responseType: 'blob',
      })
      .pipe(switchMap((blob) => this.ensureExportBlob(blob)));
  }

  private ensureExportBlob(blob: Blob): Observable<Blob> {
    const type = (blob.type ?? '').toLowerCase();
    if (
      type.includes('csv') ||
      type.includes('spreadsheet') ||
      type.includes('pdf') ||
      type.includes('octet-stream') ||
      type.includes('ms-excel')
    ) {
      return of(blob);
    }
    return from(blob.text()).pipe(
      switchMap((text) => {
        const trimmed = text.trim();
        if (
          trimmed.startsWith('{') ||
          /^failed/i.test(trimmed) ||
          trimmed.toLowerCase().includes('failed to export')
        ) {
          let message = trimmed.slice(0, 240);
          try {
            const parsed = JSON.parse(trimmed) as { message?: string };
            if (parsed.message?.trim()) {
              message = parsed.message.trim();
            }
          } catch {
            /* keep slice */
          }
          return throwError(() => new Error(message));
        }
        return of(new Blob([text], { type: blob.type || 'application/octet-stream' }));
      }),
    );
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
    if (err instanceof HttpErrorResponse && [0, 401, 403, 404].includes(err.status)) {
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
