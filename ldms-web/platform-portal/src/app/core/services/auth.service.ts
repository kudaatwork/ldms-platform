import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  catchError,
  finalize,
  forkJoin,
  map,
  Observable,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
  timeout,
} from 'rxjs';
import { UserProfileService } from './user-profile.service';
import { environment } from '../../../environments/environment';
import { CurrentUser, OrganizationClassification } from '../models/auth.model';
import {
  AuthTokenResponse,
  AuthLoginOutcome,
  extractAccessToken,
  isAuthSuccess,
} from '../models/auth-api.model';
import { ldmsApiUrl } from '../utils/api-url.util';
import { currentUserFromJwt, decodeJwtPayload } from '../utils/jwt.util';
import type { UserProfileSummary } from './user-profile.service';
import { AuthStateService } from './auth-state.service';
import { OrganizationService, OrganizationSummary } from './organization.service';
import { StorageService } from './storage.service';
import { DuplexTradingModeService } from './duplex-trading-mode.service';
import { SessionExpiryService } from './session-expiry.service';
import { TokenRefreshService } from './token-refresh.service';
import { portalHomeRoute } from '../utils/portal-navigation.util';
import { isJwtExpired } from '../utils/jwt.util';

@Injectable({ providedIn: 'root' })
export class AuthService {
  /** Via API Gateway: {@code http://localhost:8091/ldms-authentication/v1/auth} (not :4201). */
  private readonly authBase = ldmsApiUrl('/ldms-authentication/v1/auth');
  /** Background profile/org fetch — must not block sign-in navigation. */
  private static readonly SESSION_ENRICHMENT_TIMEOUT_MS = 3_000;

  private sessionInit$?: Observable<void>;
  private sessionEnrichment$?: Observable<CurrentUser | null>;

  constructor(
    private readonly http: HttpClient,
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly organizationService: OrganizationService,
    private readonly userProfile: UserProfileService,
    private readonly duplexTradingMode: DuplexTradingModeService,
    private readonly sessionExpiry: SessionExpiryService,
    private readonly tokenRefresh: TokenRefreshService,
  ) {}

  loginWithGoogleIdToken(idToken: string): Observable<void> {
    if (environment.useMocks) {
      return throwError(() => new Error('Google sign-in is not available in demo mode.'));
    }
    return this.http
      .post<AuthTokenResponse>(`${this.authBase}/google-id-token`, { idToken })
      .pipe(
        switchMap((res) => this.completeLogin(this.requireAccessToken(res), false, res)),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  /** @param usernameOrEmail LDMS username or registered email address */
  login(usernameOrEmail: string, password: string): Observable<AuthLoginOutcome> {
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
        switchMap((res) => {
          if (!isAuthSuccess(res)) {
            const msg =
              res.errorMessages?.filter(Boolean).join(' ') ||
              res.message ||
              'Authentication failed';
            return throwError(() => new Error(msg));
          }
          if (res.requiresTwoFactor === true) {
            const mfaChallengeToken = String(res.mfaChallengeToken ?? '').trim();
            if (!mfaChallengeToken) {
              return throwError(
                () => new Error('Two-factor authentication is required but no challenge token was returned.'),
              );
            }
            return of({
              kind: 'two_factor_required' as const,
              mfaChallengeToken,
              message: res.message,
              twoFactorMethod: String(res.twoFactorMethod ?? 'SMS').trim(),
            });
          }
          return this.completeLogin(
            this.requireAccessToken(res),
            res.mustChangeCredentials === true,
            res,
            loginId,
          ).pipe(
            map(() => ({ kind: 'authenticated' as const })),
          );
        }),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  verifyTwoFactor(mfaChallengeToken: string, otp: string): Observable<void> {
    return this.http
      .post<AuthTokenResponse>(`${this.authBase}/verify-two-factor`, {
        mfaChallengeToken: mfaChallengeToken.trim(),
        otp: otp.trim(),
      })
      .pipe(
        switchMap((res) =>
          this.completeLogin(this.requireAccessToken(res), res.mustChangeCredentials === true, res).pipe(
            map(() => void 0),
          ),
        ),
        catchError((err: HttpErrorResponse) =>
          throwError(() => new Error(this.messageFromHttp(err))),
        ),
      );
  }

  initializeSession(): Observable<void> {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
      this.clearSessionCache();
      this.sessionExpiry.clearWatch();
      this.authState.setCurrentUser(null);
      return of(undefined);
    }

    const refreshIfNeeded$ =
      isJwtExpired(token) && this.storage.getRefreshToken()
        ? this.tokenRefresh.refreshAccessToken().pipe(
            catchError(() => {
              this.storage.clearRefreshSession();
              return of(null);
            }),
          )
        : of(token);

    return refreshIfNeeded$.pipe(
      switchMap((refreshed) => {
        if (refreshed === null) {
          return of(undefined);
        }
        const current = this.storage.getToken();
        if (!current || isJwtExpired(current)) {
          return of(undefined);
        }
        this.sessionExpiry.watchToken(current);
        if (!this.sessionInit$) {
          const mustChangeCredentials = decodeJwtPayload(current)?.mustChangeCredentials === true;
          this.applyInitialSessionUser(current, mustChangeCredentials);
          const jwtUser = currentUserFromJwt(current);
          const jwtHasOrgContext = !!String(jwtUser?.orgClassification ?? '').trim();
          if (jwtHasOrgContext) {
            this.sessionInit$ = of(undefined).pipe(
              tap(() => this.enqueueSessionEnrichment(current, mustChangeCredentials)),
              shareReplay(1),
              finalize(() => {
                this.sessionInit$ = undefined;
              }),
            );
          } else {
            this.sessionInit$ = this.loadSessionUser(current, mustChangeCredentials).pipe(
              tap((user) => this.authState.setCurrentUser(user)),
              map(() => undefined),
              shareReplay(1),
              finalize(() => {
                this.sessionInit$ = undefined;
              }),
            );
          }
        }
        return this.sessionInit$;
      }),
    );
  }

  bootstrapFromStorage(): void {
    const token = this.storage.getToken();
    if (token && !token.startsWith('mock.')) {
      if (isJwtExpired(token) && !this.storage.getRefreshToken()) {
        this.sessionExpiry.scheduleHandleSessionExpired('expired');
        return;
      }
      if (!isJwtExpired(token)) {
        this.sessionExpiry.watchToken(token);
        this.primeUserFromJwt(token);
      }
    }
    void this.initializeSession().subscribe();
  }

  /** Route commands for the signed-in workspace (classification-aware sidebar + dashboard). */
  postLoginRoute(): string[] {
    if (this.authState.currentUser?.mustChangeCredentials) {
      return ['/auth/setup-credentials'];
    }
    return portalHomeRoute(this.authState.currentUser, this.duplexTradingMode.activeMode);
  }

  logout(): void {
    this.clearSessionCache();
    this.sessionExpiry.clearWatch();
    this.storage.clearSession();
    this.authState.setCurrentUser(null);
  }

  private completeLogin(
    token: string,
    mustChangeCredentials = false,
    authResponse?: AuthTokenResponse,
    loginId?: string,
  ): Observable<void> {
    this.clearSessionCache();
    this.storage.clearRefreshSession();
    this.storage.setToken(token);
    if (authResponse) {
      this.tokenRefresh.persistSessionCredentials(authResponse, token, loginId);
    }
    this.applyInitialSessionUser(token, mustChangeCredentials);
    this.sessionExpiry.watchToken(token);
    this.enqueueSessionEnrichment(token, mustChangeCredentials);
    return of(undefined);
  }

  private loadSessionUser(token: string, mustChangeCredentials = false): Observable<CurrentUser> {
    const enrichTimeout = AuthService.SESSION_ENRICHMENT_TIMEOUT_MS;
    const profile$ = this.userProfile.fetchCurrentUser().pipe(
      timeout(enrichTimeout),
      catchError(() => of(null)),
    );
    const org$ = this.shouldFetchOrganization(token)
      ? this.organizationService.getMy().pipe(
          timeout(enrichTimeout),
          catchError(() => of(null)),
        )
      : of(null);
    return forkJoin({ profile: profile$, org: org$ }).pipe(
      map(({ profile, org }) =>
        this.mergeSession(token, org, profile, mustChangeCredentials),
      ),
    );
  }

  private primeUserFromJwt(token: string): void {
    this.applyInitialSessionUser(
      token,
      decodeJwtPayload(token)?.mustChangeCredentials === true,
    );
  }

  /** Immediate session from JWT so sign-in and guards never wait on profile/org HTTP. */
  private applyInitialSessionUser(token: string, mustChangeCredentials = false): void {
    let user = currentUserFromJwt(token);
    if (!user) {
      return;
    }
    if (mustChangeCredentials) {
      user = { ...user, mustChangeCredentials: true };
    }
    const local = (user.email ?? '').split('@')[0];
    const firstName = (user.firstName ?? '').trim() || local || 'User';
    user = {
      ...user,
      firstName,
      displayName: this.buildDisplayName(firstName, user.lastName ?? '', local),
      welcomeMessage: this.buildWelcomeMessage(firstName, user.lastName ?? '', local),
    };
    this.authState.setCurrentUser(user);
    if (user.roles?.length) {
      this.storage.setUser({ roles: user.roles });
    }
  }

  private enqueueSessionEnrichment(token: string, mustChangeCredentials = false): void {
    if (this.sessionEnrichment$) {
      return;
    }
    this.sessionEnrichment$ = this.loadSessionUser(token, mustChangeCredentials).pipe(
      tap((user) => {
        if (user) {
          this.duplexTradingMode.syncFromUser(user);
        }
        this.authState.setCurrentUser(user);
      }),
      catchError(() => of(null)),
      shareReplay(1),
      finalize(() => {
        this.sessionEnrichment$ = undefined;
      }),
    );
    this.sessionEnrichment$.subscribe();
  }

  private shouldFetchOrganization(token: string): boolean {
    const jwt = currentUserFromJwt(token);
    if (!jwt) {
      return true;
    }
    const hasId = !!String(jwt.organizationId ?? '').trim();
    const hasClass = !!String(jwt.orgClassification ?? '').trim();
    const hasName = !!String(jwt.orgName ?? '').trim();
    return !(hasId && hasClass && hasName);
  }

  private clearSessionCache(): void {
    this.sessionInit$ = undefined;
    this.sessionEnrichment$ = undefined;
  }

  private mergeOrgContext(token: string, org: OrganizationSummary | null): CurrentUser {
    const base = currentUserFromJwt(token) ?? {
      userId: '',
      organizationId: '',
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
      duplexMode: org.duplexMode ?? base.duplexMode,
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
        procurementApprover: profile.procurementApprover === true || user.procurementApprover === true,
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
