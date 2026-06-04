import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { decodeJwtPayload } from '../utils/jwt.util';
import { ldmsApiUrl } from '../utils/api-url.util';
import { StorageService, StoredUser } from './storage.service';

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  /** Gateway → ldms-user-management backoffice (admin CRUD). */
  private readonly userBase = ldmsApiUrl('/ldms-user-management/v1/backoffice/user');

  constructor(
    private readonly http: HttpClient,
    private readonly storage: StorageService,
  ) {}

  /** Signed-in user profile — prefers {@code /me}, falls back to {@code find-by-username}. */
  fetchCurrentUser(): Observable<StoredUser | null> {
    return this.http.get<unknown>(`${this.userBase}/me`).pipe(
      map((resp) => this.mapToStoredUser(resp)),
      switchMap((profile) => {
        if (profile) {
          return of(profile);
        }
        return this.fetchByUsernameFromToken();
      }),
      catchError(() => this.fetchByUsernameFromToken()),
    );
  }

  fetchByUsername(username: string): Observable<StoredUser | null> {
    const trimmed = username.trim();
    if (!trimmed) {
      return of(null);
    }
    return this.http
      .get<unknown>(`${this.userBase}/find-by-username/${encodeURIComponent(trimmed)}`)
      .pipe(
        map((resp) => this.mapToStoredUser(resp)),
        catchError(() => of(null)),
      );
  }

  private fetchByUsernameFromToken(): Observable<StoredUser | null> {
    const username = String(decodeJwtPayload(this.storage.getToken() ?? '')?.sub ?? '').trim();
    if (!username) {
      return of(null);
    }
    return this.fetchByUsername(username);
  }

  private mapToStoredUser(response: unknown): StoredUser | null {
    if (this.isFailureEnvelope(response)) {
      return null;
    }
    const user = this.extractUserDto(response);
    if (!user) {
      return null;
    }

    const lastName = String(user['lastName'] ?? '').trim();
    const username = String(user['username'] ?? '').trim();
    const email = String(user['email'] ?? '').trim();
    const firstName =
      String(user['firstName'] ?? '').trim() ||
      this.firstToken(user['name']) ||
      this.firstToken(user['displayName']);
    const group = this.toRecord(user['userGroupDto']);
    const userType = this.toRecord(user['userTypeDto']);
    const roleLabel = String(group?.['name'] ?? userType?.['userTypeName'] ?? 'User').trim();
    const displayName = `${firstName} ${lastName}`.trim() || username || email || 'User';

    return {
      id: user['id'] != null ? Number(user['id']) : undefined,
      username,
      firstName,
      lastName,
      name: displayName,
      email: email || username,
      roleLabel,
      roles: this.extractGroupRoles(user),
      organizationKycApprover: user['organizationKycApprover'] === true,
      operationalIssueHandler: user['operationalIssueHandler'] === true,
    };
  }

  private isFailureEnvelope(response: unknown): boolean {
    const envelope = this.toRecord(response);
    if (!envelope) {
      return false;
    }
    return envelope['success'] === false || envelope['isSuccess'] === false;
  }

  /** Permission codes assigned to the user's group (authoritative for admin-portal menu RBAC). */
  private extractGroupRoles(user: Record<string, unknown>): string[] {
    const group = this.toRecord(user['userGroupDto']);
    if (!group) {
      return [];
    }
    const raw = group['userRoleDtoSet'] ?? group['userRoles'] ?? group['userRoleDtos'];
    if (!Array.isArray(raw)) {
      return [];
    }
    const codes: string[] = [];
    for (const item of raw) {
      const row = this.toRecord(item);
      const role = String(row?.['role'] ?? row?.['roleCode'] ?? '').trim();
      if (role) {
        codes.push(role);
      }
    }
    return codes;
  }

  private extractUserDto(response: unknown): Record<string, unknown> | null {
    const parsed = this.parsePossiblyStringifiedJson(response);
    const envelope = this.toRecord(parsed);
    if (envelope && this.isFailureEnvelope(envelope)) {
      return null;
    }
    if (envelope && this.looksLikeUserRecord(envelope)) {
      return envelope;
    }
    for (const key of ['userDto', 'user', 'UserDto'] as const) {
      const hit = this.extractSingleDto(parsed, key);
      if (hit) {
        return hit;
      }
    }
    const candidates = [
      envelope,
      this.toRecord(envelope?.['data']),
      this.toRecord(envelope?.['body']),
      this.toRecord(envelope?.['payload']),
    ].filter(Boolean) as Record<string, unknown>[];
    for (const wrapped of candidates) {
      if (wrapped && this.looksLikeUserRecord(wrapped)) {
        return wrapped;
      }
    }
    return null;
  }

  private extractSingleDto(response: unknown, dtoKey: string): Record<string, unknown> | null {
    const obj = this.toRecord(this.parsePossiblyStringifiedJson(response));
    if (!obj) {
      return null;
    }
    const candidates = [
      obj,
      this.toRecord(obj['data']),
      this.toRecord(obj['body']),
      this.toRecord(obj['payload']),
    ].filter(Boolean) as Record<string, unknown>[];
    for (const wrapped of candidates) {
      const hit = this.toRecord(wrapped[dtoKey]);
      if (hit) {
        return hit;
      }
    }
    return null;
  }

  private parsePossiblyStringifiedJson(value: unknown): unknown {
    if (typeof value === 'string') {
      try {
        return JSON.parse(value) as unknown;
      } catch {
        return value;
      }
    }
    return value;
  }

  private toRecord(value: unknown): Record<string, unknown> | null {
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
      return null;
    }
    return value as Record<string, unknown>;
  }

  private firstToken(value: unknown): string {
    const text = String(value ?? '').trim();
    if (!text) {
      return '';
    }
    return text.split(/\s+/)[0] ?? '';
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
}
