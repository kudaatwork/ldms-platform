import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { Subject, finalize, takeUntil } from 'rxjs';
import {
  BotAnalyticsAdminService,
  BotAnalyticsSummary,
} from '../../services/bot-analytics-admin.service';
import { buildMessageChartRows, buildSessionVolumeYAxisTicks, botMixRingDash, botMixRingOffset } from '../../utils/bot-analytics-chart.util';

export interface ChartDayRow {
  date: string;
  label: string;
  userMessages: number;
  botMessages: number;
  totalMessages: number;
}

export interface TopicRow {
  topic: string;
  count: number;
  pct: number;
}

@Component({
  selector: 'app-bot-analytics-admin-page',
  templateUrl: './bot-analytics-admin-page.component.html',
  styleUrl: './bot-analytics-admin-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class BotAnalyticsAdminPageComponent implements OnInit, OnDestroy {
  loading = true;
  error = '';
  summary: BotAnalyticsSummary | null = null;
  days = 30;

  /** True only on first load when no cached data is shown yet. */
  get initialLoading(): boolean {
    return this.loading && this.summary === null;
  }

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly analyticsService: BotAnalyticsAdminService,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Bot analytics | LX Admin');
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(forceRefresh = false): void {
    this.loading = true;
    this.error = '';
    this.analyticsService
      .getSummary(this.days, forceRefresh)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (s) => {
          this.summary = s;
          this.cdr.markForCheck();
        },
        error: (e: Error) => {
          this.error = e.message;
          this.cdr.markForCheck();
        },
      });
  }

  setDays(value: number): void {
    if (value === this.days) {
      return;
    }
    this.days = value;
    this.summary = null;
    this.load(true);
  }

  get chartDays(): ChartDayRow[] {
    return buildMessageChartRows(this.summary?.messagesByDay ?? [], (iso) => this.formatChartDate(iso));
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

  get topicRows(): TopicRow[] {
    const rows = this.summary?.topTopics ?? [];
    const max = rows[0]?.count ?? 1;
    return rows.slice(0, 8).map((t) => ({
      topic: t.topic,
      count: t.count,
      pct: max > 0 ? Math.round((t.count / max) * 100) : 0,
    }));
  }

  get userMessageShare(): number {
    const total = (this.summary?.userMessages ?? 0) + (this.summary?.botMessages ?? 0);
    if (total <= 0) {
      return 50;
    }
    return Math.round(((this.summary?.userMessages ?? 0) / total) * 100);
  }

  get satisfactionPct(): number {
    const score = this.summary?.averageSatisfactionScore;
    if (score == null) {
      return 0;
    }
    return Math.round((score / 5) * 100);
  }

  get satisfactionRingOffset(): number {
    const circumference = 2 * Math.PI * 42;
    return circumference - (this.satisfactionPct / 100) * circumference;
  }

  channelWidth(count: number): number {
    const max = this.maxChannelCount();
    return max > 0 ? Math.round((count / max) * 100) : 0;
  }

  maxChannelCount(): number {
    return (this.summary?.channelBreakdown ?? []).reduce((max, r) => Math.max(max, r.count), 1);
  }

  channelIcon(channel: string): string {
    switch ((channel ?? '').toUpperCase()) {
      case 'WHATSAPP':
        return 'chat';
      case 'SMS':
        return 'sms';
      case 'WEB':
        return 'language';
      default:
        return 'devices';
    }
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

  openLiveChatForTopic(topic: string): void {
    void this.router.navigate(['/help/bot-service'], {
      queryParams: { topic },
    });
  }

  formatScore(score?: number | null): string {
    if (score == null) {
      return '—';
    }
    return score.toFixed(1);
  }

  heroStat(value: string | number | null | undefined): string | number {
    if (this.initialLoading) {
      return '…';
    }
    if (value == null || value === '') {
      return '—';
    }
    return value;
  }

  private formatChartDate(iso: string): string {
    try {
      const d = new Date(iso.includes('T') ? iso : `${iso}T12:00:00`);
      return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
    } catch {
      return iso;
    }
  }
}
