import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, forkJoin, map, Observable, of, switchMap, tap, throwError } from 'rxjs';
import { UserProfileService } from './user-profile.service';
import { environment } from '../../../environments/environment';
import { CurrentUser, OrganizationClassification } from '../models/auth.model';
import {
  AuthTokenResponse,
  extractAccessToken,
  isAuthSuccess,
} from '../models/auth-api.model';
import { ldmsApiUrl } from '../utils/api-url.util';
import { currentUserFromJwt } from '../utils/jwt.util';
import type { UserProfileSummary } from './user-profile.service';
import { AuthStateService } from './auth-state.service';
import { OrganizationService, OrganizationSummary } from './organization.service';
import { StorageService } from './storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  /** Via API Gateway: {@code http://localhost:8091/ldms-authentication/v1/auth} (not :4201). */
  private readonly authBase = ldmsApiUrl('/ldms-authentication/v1/auth');

  constructor(
    private readonly http: HttpClient,
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly organizationService: OrganizationService,
    private readonly userProfile: UserProfileService,
  ) {}

  loginWithGoogleIdToken(idToken: string): Observable<void> {
    if (environment.useMocks) {
      return throwError(() => new Error('Google sign-in is not available in demo mode.'));
    }
    return this.http
      .post<AuthTokenResponse>(`${this.authBase}/google-id-token`, { idToken })
      .pipe(
        switchMap((res) => this.completeLogin(this.requireAccessToken(res))),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  /** @param usernameOrEmail LDMS username or registered email address */
  login(usernameOrEmail: string, password: string): Observable<void> {
    if (environment.useMocks) {
      return throwError(
        () => new Error('Demo login is disabled. Set useMocks to false in environment.ts.'),
      );
    }
    const loginId = usernameOrEmail.trim();
    return this.http
      .post<AuthTokenResponse>(`${this.authBase}/request-access-token`, {
        username: loginId,
        password,
      })
      .pipe(
        switchMap((res) => this.completeLogin(this.requireAccessToken(res))),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  initializeSession(): Observable<void> {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      this.authState.setCurrentUser(null);
      return of(undefined);
    }
    return this.loadSessionUser(token).pipe(
      tap((user) => this.authState.setCurrentUser(user)),
      map(() => undefined),
    );
  }

  bootstrapFromStorage(): void {
    void this.initializeSession().subscribe();
  }

  logout(): void {
    this.storage.clearSession();
    this.authState.setCurrentUser(null);
  }

  private completeLogin(token: string): Observable<void> {
    this.storage.setToken(token);
    return this.loadSessionUser(token).pipe(
      tap((user) => this.authState.setCurrentUser(user)),
      map(() => void 0),
    );
  }

  private loadSessionUser(token: string): Observable<CurrentUser> {
    const profile$ = this.userProfile.fetchCurrentUser();
    const org$ = this.organizationService.getMy().pipe(catchError(() => of(null)));
    return forkJoin({ profile: profile$, org: org$ }).pipe(
      map(({ profile, org }) => this.mergeSession(token, org, profile)),
    );
  }

  private mergeOrgContext(token: string, org: OrganizationSummary | null): CurrentUser {
    const base = currentUserFromJwt(token) ?? {
      userId: '',
      organizationId: '',
      orgClassification: 'SUPPLIER' as OrganizationClassification,
      orgName: '',
      roles: [],
    };
    if (!org) {
      return base;
    }
    return {
      ...base,
      organizationId: String(org.id ?? base.organizationId),
      orgClassification: org.organizationClassification ?? base.orgClassification,
      orgName: org.name ?? base.orgName,
    };
  }

  private mergeSession(
    token: string,
    org: OrganizationSummary | null,
    profile: UserProfileSummary | null,
  ): CurrentUser {
    let user = this.mergeOrgContext(token, org);
    if (profile) {
      const firstName =
        profile.firstName ||
        profile.displayName.split(/\s+/)[0] ||
        (user.firstName ?? '').trim() ||
        '';
      user = {
        ...user,
        email: profile.email || user.email,
        firstName,
        lastName: profile.lastName,
        displayName: this.buildDisplayName(firstName, profile.lastName, profile.displayName),
        welcomeMessage: this.buildWelcomeMessage(firstName, profile.lastName, profile.displayName),
      };
    } else {
      const local = (user.email ?? '').split('@')[0];
      const firstName = (user.firstName ?? '').trim() || local;
      user = {
        ...user,
        firstName,
        displayName: this.buildDisplayName(firstName, user.lastName ?? '', local),
        welcomeMessage: this.buildWelcomeMessage(firstName, user.lastName ?? '', local),
      };
    }
    return user;
  }

  private buildWelcomeMessage(firstName: string, _lastName: string, fallback?: string): string {
    const name = firstName.trim();
    if (name && name !== 'User') {
      return `Welcome ${name}`;
    }
    const fallbackFirst = (fallback ?? '').trim().split(/\s+/)[0];
    if (fallbackFirst) {
      return `Welcome ${fallbackFirst}`;
    }
    return 'Welcome';
  }

  private buildDisplayName(firstName: string, lastName: string, fallback?: string): string {
    const fullName = [firstName, lastName].filter((part) => part.trim().length > 0).join(' ').trim();
    if (fullName) {
      return fullName;
    }
    const name = (fallback ?? '').trim();
    return name || 'User';
  }

  private requireAccessToken(res: AuthTokenResponse): string {
    if (!isAuthSuccess(res)) {
      const msg =
        res.errorMessages?.filter(Boolean).join(' ') ||
        res.message ||
        'Authentication failed';
      throw new Error(msg);
    }
    const token = extractAccessToken(res);
    if (!token) {
      throw new Error('No access token in response');
    }
    return token;
  }

  private messageFromHttp(err: HttpErrorResponse): string {
    const body = err.error as AuthTokenResponse | undefined;
    if (body?.errorMessages?.length) {
      return body.errorMessages.join(' ');
    }
    if (body?.message) {
      return body.message;
    }
    if (err.status === 0) {
      return 'Cannot reach the API gateway. Start ldms-api-gateway on port 8091.';
    }
    if (err.status === 503) {
      return (
        body?.message ??
        'Authentication service is not running. Start ldms-authentication on port 8083 (and user-management on 8086), then retry.'
      );
    }
    if (err.status === 502 || err.status === 504) {
      return 'Gateway could not reach the authentication service. Ensure ldms-authentication is running on port 8083.';
    }
    return err.message ?? 'Authentication failed';
  }
}
