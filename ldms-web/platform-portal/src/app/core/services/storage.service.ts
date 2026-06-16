import { Injectable } from '@angular/core';
import { decodeJwtPayload, normalizeJwtRoles } from '../utils/jwt.util';

const USER_KEY = 'platform-portal.user';

export interface StoredUser {
  id?: number;
  username?: string;
  name?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  roles?: string[];
  roleLabel?: string;
}

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly tokenKey = 'platform-portal.token';
  private readonly refreshTokenKey = 'platform-portal.refresh-token';
  private readonly sessionUsernameKey = 'platform-portal.session-username';

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token.trim());
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(this.refreshTokenKey, token.trim());
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  setSessionUsername(username: string): void {
    localStorage.setItem(this.sessionUsernameKey, username.trim());
  }

  getSessionUsername(): string | null {
    return localStorage.getItem(this.sessionUsernameKey);
  }

  setUser(user: StoredUser): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  getUser(): StoredUser | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as StoredUser;
    } catch {
      return null;
    }
  }

  getRoles(): string[] {
    const fromUser = this.getUser()?.roles;
    if (fromUser?.length) {
      return fromUser;
    }
    const token = this.getToken();
    if (!token) {
      return [];
    }
    return normalizeJwtRoles(decodeJwtPayload(token)?.roles);
  }

  clearSession(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.sessionUsernameKey);
    localStorage.removeItem(USER_KEY);
  }

  /** Drops rotated refresh credentials without clearing the access token. */
  clearRefreshSession(): void {
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.sessionUsernameKey);
  }
}
