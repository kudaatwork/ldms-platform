import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  signal,
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Subject, catchError, finalize, forkJoin, of, takeUntil, timer } from 'rxjs';
import {
  BotAnalyticsAdminService,
  BotAnalyticsSummary,
} from '../../services/bot-analytics-admin.service';
import {
  BotConversationSession,
  BotMessage,
  BotSessionStatus,
} from '../../services/bot-service-mock.data';
import { BotServiceAdminService } from '../../services/bot-service-admin.service';

type StatusFilter = 'ALL' | BotSessionStatus;
type PageView = 'live' | 'topics';

export interface TopicInsightRow {
  topic: string;
  count: number;
  pct: number;
}

export interface MessageDateGroup {
  dateKey: string;
  label: string;
  messages: BotMessage[];
}

@Component({
  selector: 'app-bot-service-admin-page',
  templateUrl: './bot-service-admin-page.component.html',
  styleUrl: './bot-service-admin-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class BotServiceAdminPageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  readonly avatarPalette = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#3b82f6', '#14b8a6'];

  @ViewChild('chatThread') chatThreadRef?: ElementRef<HTMLElement>;

  loading = true;
  analyticsLoading = true;
  error = '';
  sessions: BotConversationSession[] = [];
  selected: BotConversationSession | null = null;
  analytics: BotAnalyticsSummary | null = null;
  analyticsDays = 30;

  search = signal('');
  statusFilter = signal<StatusFilter>('ALL');
  topicFilter = signal('');
  pageView = signal<PageView>('live');

  readonly statusFilters: Array<{ id: StatusFilter; label: string }> = [
    { id: 'ALL', label: 'All' },
    { id: 'ACTIVE', label: 'Active' },
    { id: 'WAITING', label: 'Waiting' },
    { id: 'ESCALATED', label: 'Escalated' },
    { id: 'RESOLVED', label: 'Resolved' },
  ];

  readonly pageViews: Array<{ id: PageView; label: string; icon: string }> = [
    { id: 'live', label: 'Live chat', icon: 'forum' },
    { id: 'topics', label: 'Topic insights', icon: 'insights' },
  ];

  constructor(
    private readonly botService: BotServiceAdminService,
    private readonly analyticsService: BotAnalyticsAdminService,
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
    forkJoin({
      sessions: this.botService.listSessions(),
      analytics: this.analyticsService.getSummary(this.analyticsDays).pipe(
        catchError(() => of(null)),
      ),
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.analyticsLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: ({ sessions, analytics }) => {
          this.sessions = sessions;
          this.analytics = analytics;
          if (this.selected) {
            this.selected =
              sessions.find((r) => r.sessionId === this.selected!.sessionId) ?? sessions[0] ?? null;
          } else if (sessions.length) {
            this.selected = sessions[0];
          }
          this.scrollChatToBottom();
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Could not load bot conversations. Retry in a moment.';
          this.cdr.markForCheck();
        },
      });
  }

  setPageView(view: PageView): void {
    this.pageView.set(view);
    this.cdr.markForCheck();
  }

  setAnalyticsDays(days: number): void {
    this.analyticsDays = days;
    this.analyticsLoading = true;
    this.analyticsService
      .getSummary(days)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.analyticsLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (s) => {
          this.analytics = s;
          this.cdr.markForCheck();
        },
      });
  }

  selectSession(row: BotConversationSession): void {
    this.selected = row;
    this.pageView.set('live');
    this.scrollChatToBottom();
    this.cdr.markForCheck();
  }

  setStatusFilter(id: StatusFilter): void {
    this.statusFilter.set(id);
    this.cdr.markForCheck();
  }

  filterByTopic(topic: string): void {
    const current = this.topicFilter();
    this.topicFilter.set(current === topic ? '' : topic);
    this.pageView.set('live');
    this.cdr.markForCheck();
  }

  clearTopicFilter(): void {
    this.topicFilter.set('');
    this.cdr.markForCheck();
  }

  get filteredSessions(): BotConversationSession[] {
    const q = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    const topic = this.topicFilter().trim().toLowerCase();
    return this.sessions.filter((s) => {
      if (status !== 'ALL' && s.status !== status) {
        return false;
      }
      if (topic && !(s.topic ?? '').toLowerCase().includes(topic)) {
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

  get topicInsights(): TopicInsightRow[] {
    const api = this.analytics?.topTopics ?? [];
    if (api.length) {
      const max = Math.max(...api.map((t) => t.count), 1);
      return api.slice(0, 10).map((t) => ({
        topic: t.topic,
        count: t.count,
        pct: Math.round((t.count / max) * 100),
      }));
    }
    const map = new Map<string, number>();
    for (const s of this.sessions) {
      const t = (s.topic || 'General inquiry').trim();
      map.set(t, (map.get(t) ?? 0) + 1);
    }
    const rows = Array.from(map.entries())
      .map(([topic, count]) => ({ topic, count, pct: 0 }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 10);
    const max = rows[0]?.count ?? 1;
    return rows.map((r) => ({ ...r, pct: Math.round((r.count / max) * 100) }));
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

  get resolvedCount(): number {
    return this.sessions.filter((s) => s.status === 'RESOLVED').length;
  }

  messageGroups(chat: BotConversationSession | null): MessageDateGroup[] {
    if (!chat?.messages?.length) {
      return [];
    }
    const map = new Map<string, BotMessage[]>();
    for (const m of chat.messages) {
      const key = this.dateKey(m.sentAt);
      const bucket = map.get(key) ?? [];
      bucket.push(m);
      map.set(key, bucket);
    }
    return Array.from(map.entries()).map(([dateKey, messages]) => ({
      dateKey,
      label: this.dateLabel(dateKey),
      messages,
    }));
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

  channelLabel(channel: string): string {
    switch (channel) {
      case 'WHATSAPP':
        return 'WhatsApp';
      case 'SMS':
        return 'SMS';
      default:
        return 'Web';
    }
  }

  statusClass(status: BotSessionStatus): string {
    return `bot-status--${status.toLowerCase()}`;
  }

  avatarInitials(name: string): string {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (!parts.length) {
      return '?';
    }
    if (parts.length === 1) {
      return parts[0].charAt(0).toUpperCase();
    }
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }

  avatarHue(name: string): number {
    return name.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0) % this.avatarPalette.length;
  }

  lastMessagePreview(session: BotConversationSession): string {
    const msgs = session.messages ?? [];
    const last = msgs[msgs.length - 1];
    if (!last) {
      return session.topic || 'New conversation';
    }
    const prefix = last.role === 'user' ? '' : 'Bot: ';
    const body = last.body.replace(/\s+/g, ' ').trim();
    const clipped = body.length > 72 ? `${body.slice(0, 69)}…` : body;
    return `${prefix}${clipped}`;
  }

  sessionsForTopic(topic: string): number {
    const needle = topic.toLowerCase();
    return this.sessions.filter((s) => (s.topic ?? '').toLowerCase().includes(needle)).length;
  }

  maxChannelCount(): number {
    return (this.analytics?.channelBreakdown ?? []).reduce((max, r) => Math.max(max, r.count), 1);
  }

  channelBarWidth(count: number): number {
    const max = this.maxChannelCount();
    return max > 0 ? Math.round((count / max) * 100) : 0;
  }

  topicBarWidth(count: number): number {
    const max = this.topicInsights[0]?.count ?? 1;
    return max > 0 ? Math.round((count / max) * 100) : 0;
  }

  maxDailySessions(): number {
    return (this.analytics?.sessionsByDay ?? []).reduce((max, r) => Math.max(max, r.count), 1);
  }

  dailyBarWidth(count: number): number {
    const max = this.maxDailySessions();
    return max > 0 ? Math.round((count / max) * 100) : 0;
  }

  formatScore(score?: number | null): string {
    if (score == null) {
      return '—';
    }
    return score.toFixed(1);
  }

  trackBySessionId(_i: number, row: BotConversationSession): string {
    return row.sessionId;
  }

  trackByMessageId(_i: number, row: { id: string }): string {
    return row.id;
  }

  trackByDateKey(_i: number, row: MessageDateGroup): string {
    return row.dateKey;
  }

  formatTime(iso: string): string {
    try {
      return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return iso;
    }
  }

  relativeTime(iso: string): string {
    try {
      const diff = Date.now() - new Date(iso).getTime();
      const mins = Math.floor(diff / 60000);
      if (mins < 1) {
        return 'Just now';
      }
      if (mins < 60) {
        return `${mins}m ago`;
      }
      const hrs = Math.floor(mins / 60);
      if (hrs < 24) {
        return `${hrs}h ago`;
      }
      return `${Math.floor(hrs / 24)}d ago`;
    } catch {
      return '';
    }
  }

  private dateKey(iso: string): string {
    try {
      return new Date(iso).toISOString().slice(0, 10);
    } catch {
      return iso.slice(0, 10);
    }
  }

  private dateLabel(dateKey: string): string {
    const today = new Date().toISOString().slice(0, 10);
    const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10);
    if (dateKey === today) {
      return 'Today';
    }
    if (dateKey === yesterday) {
      return 'Yesterday';
    }
    try {
      return new Date(`${dateKey}T12:00:00`).toLocaleDateString([], {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return dateKey;
    }
  }

  private scrollChatToBottom(): void {
    setTimeout(() => {
      const el = this.chatThreadRef?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    }, 0);
  }
}
