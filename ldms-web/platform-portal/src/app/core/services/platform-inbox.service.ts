import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { ldmsServiceUrl } from '../utils/api-url.util';
import { isApiFailureEnvelope, readApiFailureMessage } from '../utils/api-paged-response.util';

export interface PlatformInboxNotification {
  id: number;
  userId?: number;
  organizationId?: number;
  eventKey?: string;
  title: string;
  body: string;
  actionRoute?: string;
  entityType?: string;
  entityId?: number;
  sourceEventId?: string;
  createdAt?: string;
  unread?: boolean;
}

interface PlatformInboxResponse {
  success?: boolean;
  isSuccess?: boolean;
  notificationDtoList?: PlatformInboxNotification[];
}

@Injectable({ providedIn: 'root' })
export class PlatformInboxService {
  private readonly base = ldmsServiceUrl('notifications', 'platform-user-notification', undefined, 'frontend');

  constructor(private readonly http: HttpClient) {}

  fetchInbox(): Observable<PlatformInboxNotification[]> {
    return this.http.get<PlatformInboxResponse>(`${this.base}/inbox`).pipe(
      map((response) => {
        if (isApiFailureEnvelope(response)) {
          throw new Error(readApiFailureMessage(response, 'Failed to load notifications'));
        }
        if (response?.success === false || response?.isSuccess === false) {
          throw new Error(readApiFailureMessage(response, 'Failed to load notifications'));
        }
        return response?.notificationDtoList ?? [];
      }),
      catchError(() => of([])),
    );
  }

  dismiss(id: number): Observable<void> {
    return this.http.post<PlatformInboxResponse>(`${this.base}/dismiss/${id}`, {}).pipe(
      map(() => undefined),
      catchError(() => of(undefined)),
    );
  }

  dismissAll(): Observable<void> {
    return this.http.post<PlatformInboxResponse>(`${this.base}/dismiss-all`, {}).pipe(
      map(() => undefined),
      catchError(() => of(undefined)),
    );
  }
}
