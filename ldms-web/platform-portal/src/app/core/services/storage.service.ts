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

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token.trim());
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
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
    localStorage.removeItem(USER_KEY);
  }
}
