import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, timeout } from 'rxjs/operators';
import { decodeJwtPayload, normalizeJwtRoles } from '../utils/jwt.util';
import { StorageService, StoredUser } from './storage.service';
import { UserProfileService } from './user-profile.service';

/** Shell chrome (sidebar, topbar, dashboard greeting). */
export interface ShellUserView {
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  initials: string;
  /** e.g. "Welcome John" */
  welcomeMessage: string;
  displayName: string;
}

@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private static readonly PROFILE_TIMEOUT_MS = 12_000;

  private readonly subject = new BehaviorSubject<ShellUserView | null>(null);
  private refreshInFlight: Observable<ShellUserView | null> | null = null;
  readonly user$ = this.subject.asObservable();

  constructor(
    private readonly storage: StorageService,
    private readonly userProfile: UserProfileService,
  ) {}

  get snapshot(): ShellUserView | null {
    return this.subject.value;
  }

  syncFromStorage(): void {
    const stored = this.storage.getUser();
    if (!stored) {
      return;
    }
    const view = this.toView(stored);
    if (this.viewsEqual(view, this.subject.value)) {
      return;
    }
    this.subject.next(view);
  }

  clear(): void {
    this.subject.next(null);
  }

  /** Loads the signed-in user via gateway {@code /ldms-user-management/v1/backoffice/user/me}. */
  refreshFromApi(): Observable<ShellUserView | null> {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock-token-')) {
      this.syncFromStorage();
      return of(this.subject.value);
    }

    const payload = decodeJwtPayload(token);
    const username = String(payload?.sub ?? '').trim();
    if (!username) {
      return of(null);
    }

    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }

    const jwtRoles = normalizeJwtRoles(payload?.roles);
    this.refreshInFlight = this.userProfile.fetchCurrentUser().pipe(
      timeout(CurrentUserService.PROFILE_TIMEOUT_MS),
      catchError(() => of(null)),
      map((profile) => {
        const groupRoles = profile?.roles ?? [];
        const merged = profile
          ? {
              ...profile,
              // User-group roles from /me are authoritative; JWT is fallback when profile omits them.
              roles: groupRoles.length > 0 ? groupRoles : jwtRoles,
            }
          : this.buildStoredFromJwt(token, jwtRoles);
        this.storage.setUser(merged);
        const view = this.toView(merged);
        if (!this.viewsEqual(view, this.subject.value)) {
          this.subject.next(view);
        }
        return view;
      }),
      shareReplay({ bufferSize: 1, refCount: true }),
      finalize(() => {
        this.refreshInFlight = null;
      }),
    );
    return this.refreshInFlight;
  }

  private viewsEqual(a: ShellUserView | null, b: ShellUserView | null): boolean {
    if (a === b) {
      return true;
    }
    if (!a || !b) {
      return false;
    }
    return (
      a.firstName === b.firstName &&
      a.lastName === b.lastName &&
      a.email === b.email &&
      a.role === b.role &&
      a.displayName === b.displayName &&
      a.welcomeMessage === b.welcomeMessage &&
      a.initials === b.initials
    );
  }

  private buildStoredFromJwt(token: string, jwtRoles: string[]): StoredUser {
    const payload = decodeJwtPayload(token);
    const username = String(payload?.sub ?? '').trim();
    const email = String(payload?.email ?? username).trim();
    const localPart = email.includes('@') ? email.split('@')[0] : username;
    const jwtFirst = String(payload?.firstName ?? '').trim();
    const jwtLast = String(payload?.lastName ?? '').trim();
    const firstName = jwtFirst;
    const name = [jwtFirst, jwtLast].filter(Boolean).join(' ').trim() || jwtFirst || localPart || 'User';
    return {
      username,
      name,
      firstName,
      lastName: jwtLast,
      email: email || username,
      roleLabel: 'User',
      roles: jwtRoles,
      organizationKycApprover: payload?.organizationKycApprover === true,
      operationalIssueHandler: payload?.operationalIssueHandler === true,
    };
  }

  private toView(user: StoredUser): ShellUserView {
    const email = (user.email ?? user.username ?? '').trim();
    const lastName = (user.lastName ?? '').trim();
    const firstName = this.resolveFirstName(user.firstName, user.name, email);
    return {
      firstName,
      lastName,
      email,
      role: user.roleLabel?.trim() || 'User',
      initials: this.initials(firstName, lastName, user.name),
      welcomeMessage: this.buildWelcomeMessage(firstName),
      displayName: this.buildDisplayName(firstName, lastName, user.name),
    };
  }

  private resolveFirstName(
    storedFirst?: string | null,
    fullName?: string | null,
    email?: string | null,
  ): string {
    const fromProfile = String(storedFirst ?? '').trim();
    const emailLocal = email?.includes('@') ? email.split('@')[0].toLowerCase() : '';
    if (fromProfile && (!emailLocal || fromProfile.toLowerCase() !== emailLocal)) {
      return fromProfile;
    }
    const fromName = String(fullName ?? '')
      .trim()
      .split(/\s+/)[0];
    if (fromName && fromName.toLowerCase() !== 'user') {
      return fromName;
    }
    if (fromProfile) {
      return fromProfile;
    }
    return '';
  }

  private buildWelcomeMessage(firstName: string): string {
    const name = firstName.trim();
    if (name && name.toLowerCase() !== 'user') {
      return `Welcome ${name}`;
    }
    return 'Welcome';
  }

  private buildDisplayName(firstName: string, lastName: string, fallbackName?: string): string {
    const fullName = [firstName, lastName].filter((part) => part.trim().length > 0).join(' ').trim();
    if (fullName) {
      return fullName;
    }
    const fallback = (fallbackName ?? '').trim();
    return fallback || 'User';
  }

  private initials(firstName: string, lastName: string, fallbackName: string): string {
    const f = firstName.charAt(0);
    const l = lastName.charAt(0);
    if (f && l) {
      return `${f}${l}`.toUpperCase();
    }
    if (f) {
      return f.toUpperCase();
    }
    const parts = fallbackName.trim().split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase();
    }
    return (parts[0]?.charAt(0) ?? 'U').toUpperCase();
  }
}
