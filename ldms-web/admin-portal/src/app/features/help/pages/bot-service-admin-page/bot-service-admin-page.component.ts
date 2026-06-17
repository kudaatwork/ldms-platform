import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Subject, finalize, takeUntil, timer } from 'rxjs';
import {
  BotConversationSession,
  BotSessionStatus,
} from '../../services/bot-service-mock.data';
import { BotServiceAdminService } from '../../services/bot-service-admin.service';

type StatusFilter = 'ALL' | BotSessionStatus;

@Component({
  selector: 'app-bot-service-admin-page',
  templateUrl: './bot-service-admin-page.component.html',
  styleUrl: './bot-service-admin-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class BotServiceAdminPageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  loading = true;
  error = '';
  sessions: BotConversationSession[] = [];
  selected: BotConversationSession | null = null;
  search = signal('');
  statusFilter = signal<StatusFilter>('ALL');

  readonly statusFilters: Array<{ id: StatusFilter; label: string }> = [
    { id: 'ALL', label: 'All' },
    { id: 'ACTIVE', label: 'Active' },
    { id: 'WAITING', label: 'Waiting' },
    { id: 'ESCALATED', label: 'Escalated' },
    { id: 'RESOLVED', label: 'Resolved' },
  ];

  constructor(
    private readonly botService: BotServiceAdminService,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Bot service | LX Admin');
    this.reload();
    timer(5000, 5000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.loading && !this.error) {
          this.reload(false);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  reload(showSpinner = true): void {
    if (showSpinner) {
      this.loading = true;
    }
    this.error = '';
    this.botService
      .listSessions()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (rows) => {
          this.sessions = rows;
          if (this.selected) {
            this.selected = rows.find((r) => r.sessionId === this.selected!.sessionId) ?? rows[0] ?? null;
          } else if (rows.length) {
            this.selected = rows[0];
          }
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Could not load bot conversations. Retry in a moment.';
          this.cdr.markForCheck();
        },
      });
  }

  selectSession(row: BotConversationSession): void {
    this.selected = row;
    this.cdr.markForCheck();
  }

  setStatusFilter(id: StatusFilter): void {
    this.statusFilter.set(id);
    this.cdr.markForCheck();
  }

  get filteredSessions(): BotConversationSession[] {
    const q = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    return this.sessions.filter((s) => {
      if (status !== 'ALL' && s.status !== status) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        s.userDisplayName.toLowerCase().includes(q) ||
        s.organizationName.toLowerCase().includes(q) ||
        s.topic.toLowerCase().includes(q) ||
        s.userPhone.includes(q)
      );
    });
  }

  get activeCount(): number {
    return this.sessions.filter((s) => s.status === 'ACTIVE').length;
  }

  get waitingCount(): number {
    return this.sessions.filter((s) => s.status === 'WAITING').length;
  }

  get escalatedCount(): number {
    return this.sessions.filter((s) => s.status === 'ESCALATED').length;
  }

  channelIcon(channel: string): string {
    switch (channel) {
      case 'WHATSAPP':
        return 'chat';
      case 'SMS':
        return 'sms';
      default:
        return 'language';
    }
  }

  statusClass(status: BotSessionStatus): string {
    return `bot-status--${status.toLowerCase()}`;
  }

  trackBySessionId(_i: number, row: BotConversationSession): string {
    return row.sessionId;
  }

  trackByMessageId(_i: number, row: { id: string }): string {
    return row.id;
  }

  formatTime(iso: string): string {
    try {
      return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return iso;
    }
  }
}
