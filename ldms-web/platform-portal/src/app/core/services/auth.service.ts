import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, forkJoin, map, Observable, of, switchMap, tap, throwError, timeout } from 'rxjs';
import { UserProfileService } from './user-profile.service';
import { environment } from '../../../environments/environment';
import { CurrentUser, OrganizationClassification } from '../models/auth.model';
import {
  AuthTokenResponse,
  extractAccessToken,
  isAuthSuccess,
} from '../models/auth-api.model';
import { ldmsApiUrl } from '../utils/api-url.util';
import { currentUserFromJwt, decodeJwtPayload } from '../utils/jwt.util';
import type { UserProfileSummary } from './user-profile.service';
import { AuthStateService } from './auth-state.service';
import { OrganizationService, OrganizationSummary } from './organization.service';
import { StorageService } from './storage.service';
import { portalHomeRoute } from '../utils/portal-navigation.util';

@Injectable({ providedIn: 'root' })
export class AuthService {
  /** Via API Gateway: {@code http://localhost:8091/ldms-authentication/v1/auth} (not :4201). */
  private readonly authBase = ldmsApiUrl('/ldms-authentication/v1/auth');
  /** Avoid indefinite spinner when profile/org APIs are slow or unavailable. */
  private static readonly SESSION_ENRICHMENT_TIMEOUT_MS = 12_000;

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
        switchMap((res) =>
          this.completeLogin(this.requireAccessToken(res), res.mustChangeCredentials === true),
        ),
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
    return this.loadSessionUser(token, decodeJwtPayload(token)?.mustChangeCredentials === true).pipe(
      tap((user) => this.authState.setCurrentUser(user)),
      map(() => undefined),
    );
  }

  bootstrapFromStorage(): void {
    void this.initializeSession().subscribe();
  }

  /** Route commands for the signed-in workspace (classification-aware sidebar + dashboard). */
  postLoginRoute(): string[] {
    if (this.authState.currentUser?.mustChangeCredentials) {
      return ['/auth/setup-credentials'];
    }
    return portalHomeRoute(this.authState.currentUser);
  }

  logout(): void {
    this.storage.clearSession();
    this.authState.setCurrentUser(null);
  }

  private completeLogin(token: string, mustChangeCredentials = false): Observable<void> {
    this.storage.setToken(token);
    const jwtUser = currentUserFromJwt(token);
    if (jwtUser?.roles?.length) {
      this.storage.setUser({ roles: jwtUser.roles });
    }
    return this.loadSessionUser(token, mustChangeCredentials).pipe(
      tap((user) => this.authState.setCurrentUser(user)),
      map(() => void 0),
    );
  }

  private loadSessionUser(token: string, mustChangeCredentials = false): Observable<CurrentUser> {
    const enrichTimeout = AuthService.SESSION_ENRICHMENT_TIMEOUT_MS;
    const profile$ = this.userProfile.fetchCurrentUser().pipe(
      timeout(enrichTimeout),
      catchError(() => of(null)),
    );
    const org$ = this.organizationService.getMy().pipe(
      timeout(enrichTimeout),
      catchError(() => of(null)),
    );
    return forkJoin({ profile: profile$, org: org$ }).pipe(
      map(({ profile, org }) =>
        this.mergeSession(token, org, profile, mustChangeCredentials),
      ),
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
    mustChangeCredentials = false,
  ): CurrentUser {
    let user = this.mergeOrgContext(token, org);
    if (mustChangeCredentials || user.mustChangeCredentials) {
      user = { ...user, mustChangeCredentials: true };
    }
    if (profile) {
      const firstName =
        profile.firstName ||
        profile.displayName.split(/\s+/)[0] ||
        (user.firstName ?? '').trim() ||
        '';
      const roleLabel = profile.roleLabel?.trim() || user.roleLabel;
      const profileOrgId =
        profile.organizationId != null && profile.organizationId > 0
          ? String(profile.organizationId)
          : '';
      const profileUserId =
        profile.id != null && profile.id > 0 ? String(profile.id) : user.userId;
      user = {
        ...user,
        userId: profileUserId || user.userId,
        email: profile.email || user.email,
        firstName,
        lastName: profile.lastName,
        roleLabel,
        organizationId: profileOrgId || user.organizationId,
        displayName: this.buildDisplayName(firstName, profile.lastName, profile.displayName),
        welcomeMessage: this.buildWelcomeMessage(firstName, profile.lastName, profile.displayName),
        mustChangeCredentials:
          mustChangeCredentials || profile.mustChangeCredentials === true || user.mustChangeCredentials === true,
      };
      this.storage.setUser({
        username: profile.username,
        firstName,
        lastName: profile.lastName,
        email: profile.email,
        name: user.displayName,
        roleLabel,
        roles: user.roles,
      });
    } else {
      const local = (user.email ?? '').split('@')[0];
      const firstName = (user.firstName ?? '').trim() || local;
      user = {
        ...user,
        firstName,
        displayName: this.buildDisplayName(firstName, user.lastName ?? '', local),
        welcomeMessage: this.buildWelcomeMessage(firstName, user.lastName ?? '', local),
      };
      this.storage.setUser({ roles: user.roles });
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
