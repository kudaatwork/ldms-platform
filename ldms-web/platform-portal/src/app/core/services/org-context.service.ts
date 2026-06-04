import { Injectable } from '@angular/core';
import { AuthStateService } from './auth-state.service';
import type { UsersQuery } from '../../features/users/services/users-portal.service';

/**
 * Organisation scope for platform portal — all user-management queries are restricted
 * to the company the signed-in user belongs to.
 */
@Injectable({ providedIn: 'root' })
export class OrgContextService {
  constructor(private readonly authState: AuthStateService) {}

  get organizationId(): number | null {
    const raw = this.authState.currentUser?.organizationId;
    const id = Number(raw ?? 0);
    return Number.isFinite(id) && id > 0 ? id : null;
  }

  get organizationName(): string {
    return this.authState.currentUser?.orgName?.trim() ?? '';
  }

  /** Applies organisation filter; returns null when the session has no organisation. */
  scopedUsersQuery(q: UsersQuery): UsersQuery | null {
    const organizationId = this.organizationId;
    if (organizationId == null) {
      return null;
    }
    return {
      ...q,
      organizationId,
      columnFilters: { ...q.columnFilters },
    };
  }
}
