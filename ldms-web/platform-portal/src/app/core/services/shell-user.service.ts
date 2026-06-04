import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, timeout } from 'rxjs/operators';
import { shellRoleSummary } from '../utils/field-display.util';
import { decodeJwtPayload } from '../utils/jwt.util';
import { StorageService, StoredUser } from './storage.service';
import { UserProfileService } from './user-profile.service';
import { AuthStateService } from './auth-state.service';

/** Shell chrome (sidebar, my account hero). Mirrors admin {@link CurrentUserService}. */
export interface ShellUserView {
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  initials: string;
  welcomeMessage: string;
  displayName: string;
}

@Injectable({ providedIn: 'root' })
export class ShellUserService {
  private static readonly PROFILE_TIMEOUT_MS = 12_000;

  private readonly subject = new BehaviorSubject<ShellUserView | null>(null);
  private refreshInFlight: Observable<ShellUserView | null> | null = null;
  readonly user$ = this.subject.asObservable();

  constructor(
    private readonly storage: StorageService,
    private readonly userProfile: UserProfileService,
    private readonly authState: AuthStateService,
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

  refreshFromApi(): Observable<ShellUserView | null> {
    const token = this.storage.getToken();
    if (!token || token.startsWith('mock.')) {
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

    this.refreshInFlight = this.userProfile.fetchCurrentUser().pipe(
      timeout(ShellUserService.PROFILE_TIMEOUT_MS),
      catchError(() => of(null)),
      map((profile) => {
        const firstName = profile?.firstName ?? '';
        const lastName = profile?.lastName ?? '';
        const email = profile?.email ?? this.authState.currentUser?.email ?? '';
        const displayName =
          profile?.displayName ||
          `${firstName} ${lastName}`.trim() ||
          username;
        const roleLabel =
          profile?.roleLabel?.trim() ||
          this.authState.currentUser?.roleLabel?.trim() ||
          'User';
        if (profile) {
          this.storage.setUser({
            username,
            firstName,
            lastName,
            email,
            name: displayName,
            roleLabel,
          });
          const session = this.authState.currentUser;
          if (session && session.roleLabel !== roleLabel) {
            this.authState.setCurrentUser({ ...session, roleLabel });
          }
        }
        const view: ShellUserView = {
          firstName,
          lastName,
          email,
          role: shellRoleSummary(roleLabel, this.authState.currentUser?.orgClassification),
          initials: this.initialsFrom(firstName, lastName, displayName),
          welcomeMessage: `Welcome, ${firstName || displayName}`,
          displayName,
        };
        this.subject.next(view);
        return view;
      }),
      finalize(() => {
        this.refreshInFlight = null;
      }),
      shareReplay(1),
    );

    return this.refreshInFlight;
  }

  private toView(stored: StoredUser): ShellUserView {
    const firstName = String(stored.firstName ?? '').trim();
    const lastName = String(stored.lastName ?? '').trim();
    const displayName = `${firstName} ${lastName}`.trim() || String(stored.username ?? '');
    return {
      firstName,
      lastName,
      email: String(stored.email ?? ''),
      role: shellRoleSummary(stored.roleLabel, undefined) || 'User',
      initials: this.initialsFrom(firstName, lastName, displayName),
      welcomeMessage: `Welcome, ${firstName || displayName}`,
      displayName,
    };
  }

  private initialsFrom(first: string, last: string, fallback: string): string {
    const f = first.charAt(0);
    const l = last.charAt(0);
    if (f && l) {
      return `${f}${l}`.toUpperCase();
    }
    if (fallback.length >= 2) {
      return fallback.slice(0, 2).toUpperCase();
    }
    return 'U';
  }

  private viewsEqual(a: ShellUserView | null, b: ShellUserView | null): boolean {
    if (a === b) {
      return true;
    }
    if (!a || !b) {
      return false;
    }
    return a.email === b.email && a.displayName === b.displayName && a.role === b.role;
  }
}
