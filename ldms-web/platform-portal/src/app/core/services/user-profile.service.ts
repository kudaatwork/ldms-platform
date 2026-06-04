import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { resolveUserRoleLabel } from '../utils/field-display.util';
import { ldmsServiceUrl } from '../utils/api-url.util';

export interface UserProfileSummary {
  id?: number;
  firstName: string;
  lastName: string;
  email: string;
  username: string;
  displayName: string;
  /** User group or user type — for shell chrome, not JWT permission codes. */
  roleLabel: string;
  mustChangeCredentials?: boolean;
  organizationId?: number;
}

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  private readonly userBase = ldmsServiceUrl('user-management', 'user');

  constructor(private readonly http: HttpClient) {}

  /** Signed-in user profile (no admin lookup role required). */
  fetchCurrentUser(): Observable<UserProfileSummary | null> {
    return this.http.get<unknown>(`${this.userBase}/me`).pipe(
      map((resp) => this.mapProfile(resp)),
      catchError(() => of(null)),
    );
  }

  fetchByUsername(username: string): Observable<UserProfileSummary | null> {
    const trimmed = username.trim();
    if (!trimmed) {
      return of(null);
    }
    return this.http
      .get<unknown>(`${this.userBase}/find-by-username/${encodeURIComponent(trimmed)}`)
      .pipe(
        map((resp) => this.mapProfile(resp)),
        catchError(() => of(null)),
      );
  }

  private mapProfile(response: unknown): UserProfileSummary | null {
    const envelope = this.toRecord(response);
    if (envelope && envelope['success'] === false) {
      return null;
    }
    const user = this.extractUserDto(response);
    if (!user) {
      return null;
    }
    const firstName = String(user['firstName'] ?? '').trim();
    const lastName = String(user['lastName'] ?? '').trim();
    const username = String(user['username'] ?? '').trim();
    const email = String(user['email'] ?? '').trim();
    const displayName = `${firstName} ${lastName}`.trim() || username || email;
    const mustChangeCredentials = user['mustChangeCredentials'] === true;
    const roleLabel = resolveUserRoleLabel(user) || 'User';
    const orgRaw = Number(user['organizationId'] ?? 0);
    const organizationId = Number.isFinite(orgRaw) && orgRaw > 0 ? Math.trunc(orgRaw) : undefined;
    const idRaw = Number(user['id'] ?? 0);
    const id = Number.isFinite(idRaw) && idRaw > 0 ? Math.trunc(idRaw) : undefined;
    return {
      id,
      firstName,
      lastName,
      email,
      username,
      displayName,
      roleLabel,
      mustChangeCredentials,
      organizationId,
    };
  }

  private extractUserDto(response: unknown): Record<string, unknown> | null {
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
}
