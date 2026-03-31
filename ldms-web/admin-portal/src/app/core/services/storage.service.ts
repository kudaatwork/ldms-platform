import { Injectable } from '@angular/core';

const TOKEN_KEY = 'ldms_admin_token';
const USER_KEY = 'ldms_admin_user';

export interface StoredUser {
  name: string;
  email?: string;
  roles?: string[];
}

@Injectable({ providedIn: 'root' })
export class StorageService {
  getItem(key: string): string | null {
    if (typeof localStorage === 'undefined') {
      return null;
    }
    return localStorage.getItem(key);
  }

  setItem(key: string, value: string): void {
    localStorage.setItem(key, value);
  }

  removeItem(key: string): void {
    localStorage.removeItem(key);
  }

  getToken(): string | null {
    return this.getItem(TOKEN_KEY);
  }

  setToken(token: string): void {
    this.setItem(TOKEN_KEY, token);
  }

  clearToken(): void {
    this.removeItem(TOKEN_KEY);
  }

  getUser(): StoredUser | null {
    const raw = this.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as StoredUser;
    } catch {
      return null;
    }
  }

  setUser(user: StoredUser): void {
    this.setItem(USER_KEY, JSON.stringify(user));
  }

  clearUser(): void {
    this.removeItem(USER_KEY);
  }

  getRoles(): string[] {
    return this.getUser()?.roles ?? [];
  }

  clearSession(): void {
    this.clearToken();
    this.clearUser();
  }
}
