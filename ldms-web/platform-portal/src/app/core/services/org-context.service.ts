import { Injectable } from '@angular/core';
import { AuthStateService } from './auth-state.service';
import type { OrganizationClassification } from '../models/auth.model';
import type { UsersQuery } from '../../features/users/services/users-portal.service';

/**
 * Organisation scope for platform portal — all mutations and queries are restricted
 * to the company and classification the signed-in user belongs to.
 */
@Injectable({ providedIn: 'root' })
export class OrgContextService {
  constructor(private readonly authState: AuthStateService) {}

  get organizationId(): number | null {
    const raw = this.authState.currentUser?.organizationId;
    const id = Number(raw ?? 0);
    return Number.isFinite(id) && id > 0 ? id : null;
  }

  get organizationClassification(): OrganizationClassification | null {
    const raw = this.authState.currentUser?.orgClassification;
    return raw ?? null;
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

  /** Ensures list/filter payloads include the signed-in organisation id. */
  scopedFilterBody(body: Record<string, unknown>): Record<string, unknown> {
    const organizationId = this.organizationId;
    if (organizationId == null) {
      return body;
    }
    return { ...body, organizationId };
  }

  /**
   * Ensures mutation payloads include organisation id and classification from the session.
   * Used by platform portal services so records never leak across tenants.
   */
  withOrgScope<T extends object>(payload: T): T & {
    organizationId: number;
    organizationClassification: OrganizationClassification;
  } {
    return {
      ...payload,
      organizationId: this.requireOrganizationId(),
      organizationClassification: this.requireOrganizationClassification(),
    };
  }

  /** Supplier-scoped inventory payloads use {@code supplierId} instead of {@code organizationId}. */
  withSupplierScope<T extends object>(payload: T): T & { supplierId: number } {
    return {
      ...payload,
      supplierId: this.requireOrganizationId(),
    };
  }

  requireOrganizationId(): number {
    const organizationId = this.organizationId;
    if (organizationId == null) {
      throw new Error('No organisation in session.');
    }
    return organizationId;
  }

  requireOrganizationClassification(): OrganizationClassification {
    const organizationClassification = this.organizationClassification;
    if (!organizationClassification) {
      throw new Error('No organisation classification in session.');
    }
    return organizationClassification;
  }
}
