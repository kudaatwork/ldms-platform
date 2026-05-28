import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import {
  KycQueueSummary,
  OrganizationsAdminService,
} from '../../features/organizations/services/organizations-admin.service';

/** Shared KYC queue counts for sidebar badge, topbar notifications, and dashboard KPI. */
@Injectable({ providedIn: 'root' })
export class KycQueueStatsService {
  private readonly summarySubject = new BehaviorSubject<KycQueueSummary | null>(null);
  readonly summary$ = this.summarySubject.asObservable();

  constructor(private readonly organizations: OrganizationsAdminService) {}

  get snapshot(): KycQueueSummary | null {
    return this.summarySubject.value;
  }

  refresh(): Observable<KycQueueSummary | null> {
    return this.organizations.fetchKycQueueSummary().pipe(
      tap((summary) => this.summarySubject.next(summary)),
      catchError(() => {
        this.summarySubject.next(null);
        return of(null);
      }),
    );
  }
}
