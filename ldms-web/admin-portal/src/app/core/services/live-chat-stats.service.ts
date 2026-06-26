import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  AdminSupportTicket,
  HelpSupportAdminService,
} from '../../features/help/services/help-support-admin.service';

const SEEN_STORAGE_KEY = 'lx-livechat-seen';

/**
 * Surfaces support tickets with new live-chat activity for the topbar notification bell.
 *
 * The list endpoint exposes each ticket's {@code modifiedAt} but not per-message authorship, so a
 * "new message" is detected as the ticket's {@code modifiedAt} advancing past the last value the
 * handler has seen. Seen markers are persisted in localStorage so they survive reloads. Closed and
 * resolved tickets are excluded.
 */
@Injectable({ providedIn: 'root' })
export class LiveChatStatsService {
  private readonly ticketsSubject = new BehaviorSubject<AdminSupportTicket[]>([]);
  readonly tickets$ = this.ticketsSubject.asObservable();

  constructor(private readonly helpApi: HelpSupportAdminService) {}

  get snapshot(): AdminSupportTicket[] {
    return this.ticketsSubject.value;
  }

  refresh(): Observable<AdminSupportTicket[]> {
    return this.helpApi.fetchAllTickets().pipe(
      map((tickets) => this.computeUnread(tickets)),
      catchError(() => {
        this.ticketsSubject.next([]);
        return of([] as AdminSupportTicket[]);
      }),
    );
  }

  /** Marks a ticket's current activity as seen so it drops off the bell. */
  markSeen(ticketId: number): void {
    const ticket = this.snapshot.find((t) => t.id === ticketId);
    const seen = this.readSeen();
    seen[ticketId] = ticket?.modifiedAt || ticket?.createdAt || new Date().toISOString();
    this.writeSeen(seen);
    this.ticketsSubject.next(this.snapshot.filter((t) => t.id !== ticketId));
  }

  clear(): void {
    this.ticketsSubject.next([]);
  }

  private computeUnread(tickets: AdminSupportTicket[]): AdminSupportTicket[] {
    const seen = this.readSeen();
    const unread = tickets.filter((ticket) => {
      const status = (ticket.status ?? '').toUpperCase();
      if (status === 'CLOSED' || status === 'RESOLVED') {
        return false;
      }
      const lastActivity = ticket.modifiedAt || ticket.createdAt || '';
      const lastSeen = seen[ticket.id];
      // First time we observe the ticket counts as new activity for the handler.
      return !lastSeen || new Date(lastActivity).getTime() > new Date(lastSeen).getTime();
    });
    this.ticketsSubject.next(unread);
    return unread;
  }

  private readSeen(): Record<number, string> {
    try {
      const raw = localStorage.getItem(SEEN_STORAGE_KEY);
      return raw ? (JSON.parse(raw) as Record<number, string>) : {};
    } catch {
      return {};
    }
  }

  private writeSeen(seen: Record<number, string>): void {
    try {
      localStorage.setItem(SEEN_STORAGE_KEY, JSON.stringify(seen));
    } catch {
      // Storage unavailable (private mode/quota) — markers simply won't persist across reloads.
    }
  }
}
