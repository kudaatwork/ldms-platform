import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ldmsApiUrl } from '../utils/api-url.util';
import { StoredUser } from './storage.service';

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  /** Gateway → ldms-user-management backoffice (same surface as users admin APIs). */
  private readonly userBase = ldmsApiUrl('/ldms-user-management/v1/backoffice/user');

  constructor(private readonly http: HttpClient) {}

  /** Signed-in user profile (no admin lookup role required). */
  fetchCurrentUser(): Observable<StoredUser | null> {
    return this.http.get<unknown>(`${this.userBase}/me`).pipe(
      map((resp) => this.mapToStoredUser(resp)),
      catchError(() => of(null)),
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

  private mapToStoredUser(response: unknown): StoredUser | null {
    const envelope = this.toRecord(response);
    if (envelope && envelope['success'] === false) {
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
      roles: [],
      organizationKycApprover: user['organizationKycApprover'] === true,
    };
  }

  private extractUserDto(response: unknown): Record<string, unknown> | null {
    const direct = this.toRecord(response);
    if (direct && this.looksLikeUserRecord(direct)) {
      return direct;
    }
    for (const key of ['userDto', 'user', 'UserDto'] as const) {
      const hit = this.extractSingleDto(response, key);
      if (hit) {
        return hit;
      }
    }
    return null;
  }

  private extractSingleDto(response: unknown, dtoKey: string): Record<string, unknown> | null {
    const obj = this.toRecord(response);
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
