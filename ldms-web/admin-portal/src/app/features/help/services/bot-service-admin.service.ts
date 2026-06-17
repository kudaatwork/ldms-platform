import { Injectable } from '@angular/core';
import { Observable, delay, map, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  BOT_CONVERSATION_SEEDS,
  BotConversationSession,
  BotSessionStatus,
} from './bot-service-mock.data';

@Injectable({ providedIn: 'root' })
export class BotServiceAdminService {
  private sessions = [...BOT_CONVERSATION_SEEDS];

  listSessions(): Observable<BotConversationSession[]> {
    if (environment.useMocks) {
      return of(this.sessions).pipe(delay(180));
    }
    // Future: GET /ldms-messaging-inbound/v1/backoffice/bot-session/list
    return of(this.sessions).pipe(delay(220));
  }

  findSession(sessionId: string): Observable<BotConversationSession | null> {
    return this.listSessions().pipe(
      map((rows) => rows.find((r) => r.sessionId === sessionId) ?? null),
    );
  }

  countByStatus(status: BotSessionStatus): Observable<number> {
    return this.listSessions().pipe(map((rows) => rows.filter((r) => r.status === status).length));
  }
}
