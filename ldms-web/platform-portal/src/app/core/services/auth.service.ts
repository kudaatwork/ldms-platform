import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CurrentUser } from '../models/auth.model';
import { AuthStateService } from './auth-state.service';
import { StorageService } from './storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private readonly http: HttpClient,
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
  ) {}

  login(email: string, password: string): Observable<void> {
    return this.http
      .post<{ token: string }>(`${environment.apiUrl}/api/v1/frontend/auth/login`, { email, password })
      .pipe(
        tap((res) => this.storage.setToken(res.token)),
        tap((res) => this.authState.setCurrentUser(this.decodeToken(res.token))),
        map(() => void 0),
      );
  }

  bootstrapFromStorage(): void {
    const token = this.storage.getToken();
    this.authState.setCurrentUser(token ? this.decodeToken(token) : null);
  }

  logout(): void {
    this.storage.clearSession();
    this.authState.setCurrentUser(null);
  }

  private decodeToken(token: string): CurrentUser {
    const chunks = token.split('.');
    if (chunks.length < 2) {
      throw new Error('Invalid JWT token');
    }
    const payload = JSON.parse(atob(chunks[1] ?? ''));
    return {
      orgClassification: payload.orgClassification,
      organizationId: String(payload.organizationId),
      orgName: payload.orgName,
      userId: String(payload.userId),
      roles: Array.isArray(payload.roles) ? payload.roles : [],
      email: payload.email,
    };
  }
}
