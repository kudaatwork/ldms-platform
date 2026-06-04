import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Subject, catchError, forkJoin, of, takeUntil } from 'rxjs';
import {
  HelpArticle,
  HelpArticleCategory,
  HelpSupportService,
  PlatformStatusSummary,
  SupportTicket,
  SupportTicketCategory,
} from '../../services/help-support.service';
import { UserProfileService } from '../../../../core/services/user-profile.service';

type HelpTab = 'overview' | 'faq' | 'ticket' | 'tickets' | 'status';

@Component({
  selector: 'app-help-support-page',
  templateUrl: './help-support-page.component.html',
  styleUrls: ['./help-support-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class HelpSupportPageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly helpApi = inject(HelpSupportService);
  private readonly profileApi = inject(UserProfileService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly activeTab = signal<HelpTab>('overview');
  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly submitting = signal(false);
  readonly submitOk = signal(false);
  readonly createdTicket = signal<SupportTicket | null>(null);
  readonly faqSearch = signal('');
  readonly selectedArticle = signal<HelpArticle | null>(null);
  readonly selectedCategory = signal<HelpArticleCategory | 'ALL'>('ALL');

  articles: HelpArticle[] = [];
  tickets: SupportTicket[] = [];
  platformStatus: PlatformStatusSummary | null = null;
  userDisplayName = '';

  readonly categoryFilters: { id: HelpArticleCategory | 'ALL'; label: string; icon: string }[] = [
    { id: 'ALL', label: 'All topics', icon: 'auto_awesome' },
    { id: 'GETTING_STARTED', label: 'Getting started', icon: 'rocket_launch' },
    { id: 'OPERATIONS', label: 'Operations', icon: 'local_shipping' },
    { id: 'ACCOUNT', label: 'Account', icon: 'shield' },
    { id: 'BILLING', label: 'Billing', icon: 'receipt_long' },
    { id: 'PLATFORM', label: 'Platform', icon: 'cloud' },
  ];

  readonly ticketCategories: { id: SupportTicketCategory; label: string }[] = [
    { id: 'GENERAL', label: 'General question' },
    { id: 'TECHNICAL', label: 'Technical issue' },
    { id: 'OPERATIONS', label: 'Orders / shipments' },
    { id: 'BILLING', label: 'Billing / invoices' },
    { id: 'ACCESS', label: 'Access & permissions' },
    { id: 'SECURITY', label: 'Security concern' },
  ];

  readonly ticketForm = inject(FormBuilder).nonNullable.group({
    subject: ['', [Validators.required, Validators.maxLength(200)]],
    category: ['GENERAL' as SupportTicketCategory, Validators.required],
    description: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(8000)]],
  });

  ngOnInit(): void {
    this.profileApi.fetchCurrentUser().pipe(takeUntil(this.destroy$)).subscribe((profile) => {
      this.userDisplayName = profile?.displayName ?? profile?.email ?? '';
      this.cdr.markForCheck();
    });
    this.reload();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setTab(tab: HelpTab): void {
    this.activeTab.set(tab);
    if (tab === 'faq' && !this.selectedArticle() && this.filteredArticles.length) {
      this.openArticle(this.filteredArticles[0]);
    }
  }

  setCategory(category: HelpArticleCategory | 'ALL'): void {
    this.selectedCategory.set(category);
    this.selectedArticle.set(null);
    this.reloadArticles(category);
  }

  get filteredArticles(): HelpArticle[] {
    const q = this.faqSearch().trim().toLowerCase();
    if (!q) {
      return this.articles;
    }
    return this.articles.filter(
      (a) =>
        a.title.toLowerCase().includes(q) ||
        a.summary.toLowerCase().includes(q) ||
        a.bodyMarkdown.toLowerCase().includes(q),
    );
  }

  openArticle(article: HelpArticle): void {
    this.selectedArticle.set(article);
  }

  articleBodyLines(article: HelpArticle | null): { kind: 'h2' | 'text'; text: string }[] {
    if (!article) {
      return [];
    }
    return article.bodyMarkdown.split('\n').map((line) => {
      const trimmed = line.trim();
      if (trimmed.startsWith('## ')) {
        return { kind: 'h2' as const, text: trimmed.slice(3) };
      }
      return { kind: 'text' as const, text: line };
    });
  }

  statusTone(status: PlatformStatusSummary['overallStatus'] | undefined): string {
    switch (status) {
      case 'OPERATIONAL':
        return 'operational';
      case 'OUTAGE':
        return 'outage';
      default:
        return 'degraded';
    }
  }

  ticketStatusLabel(status: SupportTicket['status']): string {
    return status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  }

  reload(): void {
    this.loading.set(true);
    this.loadError.set('');
    let firstError = '';
    const captureError = (err: Error) => {
      if (!firstError) {
        firstError = err.message;
      }
      return err;
    };

    forkJoin({
      articles: this.helpApi.fetchArticles().pipe(
        catchError((err: Error) => {
          captureError(err);
          return of([] as HelpArticle[]);
        }),
      ),
      tickets: this.helpApi.fetchMyTickets().pipe(
        catchError((err: Error) => {
          captureError(err);
          return of([] as SupportTicket[]);
        }),
      ),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ articles, tickets }) => {
          this.articles = articles;
          this.tickets = tickets;
          this.loadError.set(firstError);
          this.loading.set(false);
          this.cdr.markForCheck();
        },
      });

    this.loadPlatformStatusInBackground(() => firstError, (msg) => {
      firstError = msg;
    });
  }

  /** Platform health probes every LDMS service and can take several seconds — load after the page shell. */
  private loadPlatformStatusInBackground(
    readError: () => string,
    writeError: (msg: string) => void,
  ): void {
    this.helpApi
      .fetchPlatformStatus()
      .pipe(
        catchError((err: Error) => {
          if (!readError()) {
            writeError(err.message);
          }
          return of(null);
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((status) => {
        this.platformStatus = status;
        const errMsg = readError();
        if (errMsg && !this.loadError()) {
          this.loadError.set(errMsg);
        }
        this.cdr.markForCheck();
      });
  }

  reloadArticles(category: HelpArticleCategory | 'ALL'): void {
    const req =
      category === 'ALL' ? this.helpApi.fetchArticles() : this.helpApi.fetchArticles(category);
    req.pipe(takeUntil(this.destroy$)).subscribe({
      next: (articles) => {
        this.articles = articles;
        this.cdr.markForCheck();
      },
    });
  }

  submitTicket(): void {
    this.ticketForm.markAllAsTouched();
    if (this.ticketForm.invalid || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.submitOk.set(false);
    const value = this.ticketForm.getRawValue();
    this.helpApi
      .createTicket({
        subject: value.subject,
        description: value.description,
        category: value.category,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (ticket) => {
          this.tickets = [ticket, ...this.tickets];
          this.createdTicket.set(ticket);
          this.submitOk.set(true);
          this.submitting.set(false);
          this.ticketForm.reset({ category: 'GENERAL', subject: '', description: '' });
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.loadError.set(err.message);
          this.submitting.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  fieldError(field: 'subject' | 'description'): string | null {
    const c = this.ticketForm.get(field);
    if (!c || !c.errors || !c.touched) {
      return null;
    }
    if (c.errors['required']) {
      return 'This field is required.';
    }
    if (c.errors['minlength']) {
      return `Enter at least ${c.errors['minlength'].requiredLength} characters.`;
    }
    if (c.errors['maxlength']) {
      return 'This value is too long.';
    }
    return null;
  }

  trackArticle(_: number, item: HelpArticle): string {
    return item.slug;
  }

  trackTicket(_: number, item: SupportTicket): string {
    return item.ticketNumber;
  }
}
