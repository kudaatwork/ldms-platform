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
import { ActivatedRoute } from '@angular/router';
import { Subject, catchError, finalize, forkJoin, of, takeUntil, timer } from 'rxjs';
import {
  BotAnalyticsAdminService,
  BotAnalyticsSummary,
} from '../../services/bot-analytics-admin.service';
import { buildMessageChartRows, buildSessionVolumeYAxisTicks, botMixRingDash, botMixRingOffset } from '../../utils/bot-analytics-chart.util';
import {
  BotConversationSession,
  BotMessage,
  BotSessionStatus,
} from '../../services/bot-service-mock.data';
import { BotServiceAdminService } from '../../services/bot-service-admin.service';
import {
  BotLlmSettings,
  BotLlmSettingsAdminService,
} from '../../services/bot-llm-settings-admin.service';

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
  analyticsError = '';
  sessions: BotConversationSession[] = [];
  selected: BotConversationSession | null = null;
  analytics: BotAnalyticsSummary | null = null;
  analyticsDays = 30;

  llmSettings: BotLlmSettings | null = null;
  llmLoading = false;
  llmSaving = false;
  llmError = '';
  llmProvider = 'auto';
  llmModel = '';

  readonly llmProviderOptions: Array<{ id: string; label: string }> = [
    { id: 'auto', label: 'Auto' },
    { id: 'anthropic', label: 'Claude' },
    { id: 'gemini', label: 'Gemini' },
  ];

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
    private readonly llmSettingsService: BotLlmSettingsAdminService,
    private readonly analyticsService: BotAnalyticsAdminService,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    private readonly route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Bot service | LX Admin');
    const topicParam = this.route.snapshot.queryParamMap.get('topic');
    if (topicParam) {
      this.topicFilter.set(topicParam);
      this.pageView.set('live');
    }
    this.reload();
    this.loadLlmSettings();
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
    this.analyticsError = '';
    forkJoin({
      sessions: this.botService.listSessions(),
      analytics: this.analyticsService.getSummary(this.analyticsDays).pipe(
        catchError((err: Error) => {
          this.analyticsError = err.message;
          return of(null);
        }),
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

  loadLlmSettings(): void {
    this.llmLoading = true;
    this.llmError = '';
    this.llmSettingsService
      .currentSettings()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.llmLoading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (settings) => {
          this.llmSettings = settings;
          this.llmProvider = settings.runtimeProvider ?? settings.configuredProvider ?? 'auto';
          this.llmModel = this.resolveLlmModelSelection(settings);
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.llmError = err.message;
          this.cdr.markForCheck();
        },
      });
  }

  applyLlmSettings(): void {
    if (this.llmSaving) {
      return;
    }
    if (this.llmProvider === 'gemini' && !this.llmSettings?.geminiConfigured) {
      this.llmError = 'Gemini is not configured on this server (missing API key).';
      return;
    }
    if (this.llmProvider === 'anthropic' && !this.llmSettings?.anthropicConfigured) {
      this.llmError = 'Claude is not configured on this server (missing API key).';
      return;
    }
    if (this.llmProvider !== 'auto' && !this.llmModel?.trim()) {
      this.llmError = 'Select a model before applying.';
      return;
    }
    this.llmSaving = true;
    this.llmError = '';
    const provider = this.llmProvider === 'auto' ? '' : this.llmProvider;
    const payload =
      this.llmProvider === 'gemini'
        ? { provider, geminiModel: this.llmModel || null, anthropicModel: null }
        : this.llmProvider === 'anthropic'
          ? { provider, anthropicModel: this.llmModel || null, geminiModel: null }
          : { provider: null, geminiModel: null, anthropicModel: null };

    this.llmSettingsService
      .updateRuntime(payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.llmSaving = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (settings) => {
          this.llmSettings = settings;
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.llmError = err.message;
          this.cdr.markForCheck();
        },
      });
  }

  onLlmProviderChange(): void {
    const options = this.llmModelOptions;
    this.llmModel = options.length ? options[0].modelId : '';
    this.cdr.markForCheck();
  }

  get llmModelOptions(): Array<{ modelId: string; label: string }> {
    const catalog = this.llmSettings?.modelCatalog ?? [];
    const provider = this.llmProvider === 'auto' ? this.llmSettings?.activeProvider ?? 'anthropic' : this.llmProvider;
    return catalog
      .filter((row) => row.providerId === provider)
      .map((row) => ({ modelId: row.modelId, label: row.label }));
  }

  llmActiveLabel(): string {
    if (!this.llmSettings) {
      return 'Loading…';
    }
    const provider = this.llmSettings.activeProvider ?? '—';
    const model = this.llmSettings.activeModel ?? '—';
    return `${provider} · ${model}`;
  }

  private resolveLlmModelSelection(settings: BotLlmSettings): string {
    const provider = settings.runtimeProvider ?? settings.activeProvider ?? 'auto';
    if (provider === 'gemini') {
      return settings.runtimeGeminiModel ?? settings.activeModel ?? '';
    }
    if (provider === 'anthropic') {
      return settings.runtimeAnthropicModel ?? settings.activeModel ?? '';
    }
    return settings.activeModel ?? '';
  }

  setPageView(view: PageView): void {
    this.pageView.set(view);
    if (view === 'topics' && !this.analytics && !this.analyticsLoading) {
      this.refreshAnalytics();
    }
    this.cdr.markForCheck();
  }

  setAnalyticsDays(days: number): void {
    if (days === this.analyticsDays && this.analytics) {
      return;
    }
    this.analyticsDays = days;
    this.refreshAnalytics();
  }

  refreshAnalytics(): void {
    this.analyticsLoading = true;
    this.analyticsError = '';
    this.analyticsService
      .getSummary(this.analyticsDays, true)
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
        error: (err: Error) => {
          this.analytics = null;
          this.analyticsError = err.message;
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

  get displayAnalytics(): BotAnalyticsSummary {
    return this.analytics ?? this.buildFallbackAnalytics();
  }

  get usingAnalyticsFallback(): boolean {
    return !this.analytics && this.sessions.length > 0;
  }

  get topicInsights(): TopicInsightRow[] {
    const api = this.displayAnalytics.topTopics ?? [];
    if (api.length) {
      const max = Math.max(...api.map((t) => t.count), 1);
      return api.slice(0, 10).map((t) => ({
        topic: t.topic,
        count: t.count,
        pct: Math.round((t.count / max) * 100),
      }));
    }
    return [];
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
    return (this.displayAnalytics.channelBreakdown ?? []).reduce((max, r) => Math.max(max, r.count), 1);
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
    return (this.displayAnalytics.sessionsByDay ?? []).reduce((max, r) => Math.max(max, r.count), 1);
  }

  dailyBarWidth(count: number): number {
    const max = this.maxDailySessions();
    return max > 0 ? Math.round((count / max) * 100) : 0;
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

  formatScore(score?: number | null): string {
    if (score == null) {
      return '—';
    }
    return score.toFixed(1);
  }

  get chartDays(): ReturnType<typeof buildMessageChartRows> {
    return buildMessageChartRows(this.displayAnalytics.messagesByDay ?? [], (iso) =>
      this.formatChartDate(iso),
    );
  }

  get chartYAxisTicks(): number[] {
    const max = this.chartDays.reduce((m, row) => Math.max(m, row.totalMessages), 0);
    return buildSessionVolumeYAxisTicks(max);
  }

  get botMessageShare(): number {
    return 100 - this.userMessageShare;
  }

  get mixUserDash(): string {
    return botMixRingDash(this.userMessageShare);
  }

  get mixBotDash(): string {
    return botMixRingDash(this.botMessageShare);
  }

  get mixBotOffset(): number {
    return botMixRingOffset(this.userMessageShare);
  }

  get userMessageShare(): number {
    const total = (this.displayAnalytics.userMessages ?? 0) + (this.displayAnalytics.botMessages ?? 0);
    if (total <= 0) {
      return 50;
    }
    return Math.round(((this.displayAnalytics.userMessages ?? 0) / total) * 100);
  }

  get satisfactionPct(): number {
    const score = this.displayAnalytics.averageSatisfactionScore;
    if (score == null) {
      return 0;
    }
    return Math.round((score / 5) * 100);
  }

  get satisfactionRingOffset(): number {
    return 264 - (this.satisfactionPct / 100) * 264;
  }

  rankMedal(index: number): 'gold' | 'silver' | 'bronze' | null {
    if (index === 0) {
      return 'gold';
    }
    if (index === 1) {
      return 'silver';
    }
    if (index === 2) {
      return 'bronze';
    }
    return null;
  }

  channelLabel(channel: string): string {
    switch ((channel ?? '').toUpperCase()) {
      case 'WHATSAPP':
        return 'WhatsApp';
      case 'SMS':
        return 'SMS';
      case 'WEB':
        return 'Web portal';
      default:
        return channel;
    }
  }

  private formatChartDate(iso: string): string {
    try {
      const d = new Date(iso.includes('T') ? iso : `${iso}T12:00:00`);
      return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
    } catch {
      return iso;
    }
  }

  private buildFallbackAnalytics(): BotAnalyticsSummary {
    const windowStart = Date.now() - (this.analyticsDays - 1) * 86_400_000;
    const inWindow = this.sessions.filter((s) => {
      const stamp = s.lastMessageAt || s.messages?.[s.messages.length - 1]?.sentAt;
      if (!stamp) {
        return true;
      }
      return new Date(stamp).getTime() >= windowStart;
    });

    const dayMap = new Map<string, number>();
    const messageDayMap = new Map<string, { user: number; bot: number }>();
    const topicMap = new Map<string, number>();
    const channelMap = new Map<string, number>();
    let userMessages = 0;
    let botMessages = 0;
    let ratedTotal = 0;
    let ratedCount = 0;
    const todayKey = new Date().toISOString().slice(0, 10);
    const weekStart = Date.now() - 6 * 86_400_000;
    let sessionsToday = 0;
    let sessionsLast7Days = 0;

    for (const session of inWindow) {
      const dayKey = this.dateKey(session.lastMessageAt);
      dayMap.set(dayKey, (dayMap.get(dayKey) ?? 0) + 1);

      const topic = (session.topic || 'General inquiry').trim();
      topicMap.set(topic, (topicMap.get(topic) ?? 0) + 1);

      const channel = session.channel ?? 'WEB';
      channelMap.set(channel, (channelMap.get(channel) ?? 0) + 1);

      if (dayKey === todayKey) {
        sessionsToday++;
      }
      if (new Date(session.lastMessageAt).getTime() >= weekStart) {
        sessionsLast7Days++;
      }

      for (const message of session.messages ?? []) {
        const messageDayKey = this.dateKey(message.sentAt || session.lastMessageAt);
        const bucket = messageDayMap.get(messageDayKey) ?? { user: 0, bot: 0 };
        const role = (message.role ?? '').toLowerCase();
        if (role === 'user') {
          userMessages++;
          bucket.user++;
        } else if (role === 'bot') {
          botMessages++;
          bucket.bot++;
        }
        messageDayMap.set(messageDayKey, bucket);
      }

      if (session.satisfactionScore != null) {
        ratedTotal += session.satisfactionScore;
        ratedCount++;
      }
    }

    const sessionsByDay = Array.from(dayMap.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, count]) => ({ date, count }));

    const messagesByDay = Array.from(messageDayMap.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, counts]) => ({
        date,
        userMessages: counts.user,
        botMessages: counts.bot,
      }));

    const topTopics = Array.from(topicMap.entries())
      .map(([topic, count]) => ({ topic, count }))
      .sort((a, b) => b.count - a.count);

    const channelBreakdown = Array.from(channelMap.entries()).map(([channel, count]) => ({
      channel,
      count,
    }));

    const totalMessages = userMessages + botMessages;

    return {
      totalSessions: inWindow.length,
      sessionsToday,
      sessionsLast7Days,
      totalMessages,
      userMessages,
      botMessages,
      averageMessagesPerSession:
        inWindow.length > 0 ? Math.round((totalMessages / inWindow.length) * 100) / 100 : 0,
      averageSatisfactionScore: ratedCount > 0 ? ratedTotal / ratedCount : null,
      ratedSessionCount: ratedCount,
      publishedFaqCount: 0,
      sessionsByDay,
      messagesByDay,
      topTopics,
      channelBreakdown,
    };
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
