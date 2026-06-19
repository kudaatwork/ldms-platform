import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Subject, finalize, takeUntil } from 'rxjs';
import {
  BotAnalyticsAdminService,
  BotAnalyticsSummary,
} from '../../services/bot-analytics-admin.service';

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

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly analyticsService: BotAnalyticsAdminService,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Bot analytics | LX Admin');
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.analyticsService
      .getSummary(this.days)
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
    this.days = value;
    this.load();
  }

  maxDailyCount(): number {
    const rows = this.summary?.sessionsByDay ?? [];
    return rows.reduce((max, r) => Math.max(max, r.count), 1);
  }

  barWidth(count: number): number {
    const max = this.maxDailyCount();
    return max > 0 ? Math.round((count / max) * 100) : 0;
  }

  maxChannelCount(): number {
    const rows = this.summary?.channelBreakdown ?? [];
    return rows.reduce((max, r) => Math.max(max, r.count), 1);
  }

  channelWidth(count: number): number {
    const max = this.maxChannelCount();
    return max > 0 ? Math.round((count / max) * 100) : 0;
  }

  formatScore(score?: number | null): string {
    if (score == null) {
      return '—';
    }
    return `${score.toFixed(1)} / 5`;
  }
}
