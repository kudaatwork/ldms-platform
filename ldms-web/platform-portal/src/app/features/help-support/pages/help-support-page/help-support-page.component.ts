import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
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
import {
  BotChatMessage,
  BotChatService,
  BotChatSession,
  BotAssistantMode,
  BotPricing,
} from '../../services/bot-chat.service';
import {
  BotLlmSettings,
  BotLlmSettingsService,
} from '../../services/bot-llm-settings.service';
import { UserProfileService } from '../../../../core/services/user-profile.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { OrgContextService } from '../../../../core/services/org-context.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { PlatformWalletService, type PlatformActionChargeRow } from '../../../../core/services/platform-wallet.service';
import {
  HELP_SUPPORT_ACTION_CODES,
  actionChargeMap,
  chargeCentsForAction,
  formatActionPrice,
} from '../../../../shared/utils/platform-action-pricing.util';

type HelpTab = 'overview' | 'faq' | 'ticket' | 'tickets' | 'status' | 'assistant';

export interface ChatThreadItem {
  id: string;
  role: 'REQUESTER' | 'HANDLER' | 'SYSTEM';
  author: string;
  at: string;
  body: string;
  isMine: boolean;
  dateKey: string;
}

@Component({
  selector: 'app-help-support-page',
  templateUrl: './help-support-page.component.html',
  styleUrls: ['./help-support-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class HelpSupportPageComponent implements OnInit, OnDestroy, AfterViewChecked {
  private readonly destroy$ = new Subject<void>();
  private readonly helpApi = inject(HelpSupportService);
  private readonly botApi = inject(BotChatService);
  private readonly botLlmApi = inject(BotLlmSettingsService);
  private readonly profileApi = inject(UserProfileService);
  private readonly notifications = inject(NotificationService);
  private readonly orgContext = inject(OrgContextService);
  private readonly authState = inject(AuthStateService);
  private readonly platformWallet = inject(PlatformWalletService);
  private readonly cdr = inject(ChangeDetectorRef);

  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private typingTimer: ReturnType<typeof setTimeout> | null = null;
  private scrollPending = false;
  private lastReadByTicket = new Map<number, number>();

  @ViewChild('chatThread') chatThreadRef?: ElementRef<HTMLElement>;

  readonly activeTab = signal<HelpTab>('overview');
  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly actionError = signal('');
  readonly submitting = signal(false);
  readonly submitOk = signal(false);
  readonly createdTicket = signal<SupportTicket | null>(null);
  readonly faqSearch = signal('');
  readonly selectedArticle = signal<HelpArticle | null>(null);
  readonly selectedCategory = signal<HelpArticleCategory | 'ALL'>('ALL');
  readonly selectedTicket = signal<SupportTicket | null>(null);
  readonly sendingReply = signal(false);
  readonly chatLoading = signal(false);
  readonly ticketSearch = signal('');
  readonly isTyping = signal(false);
  readonly mobileShowChat = signal(false);
  readonly supportPresence = signal<'online' | 'away' | 'offline'>('offline');
  readonly botSessions = signal<BotChatSession[]>([]);
  readonly selectedBotSession = signal<BotChatSession | null>(null);
  readonly botChatLoading = signal(false);
  readonly botSending = signal(false);
  readonly botRating = signal(false);
  readonly botStarting = signal(false);
  readonly botAwaitingReply = signal(false);
  readonly assistantOnline = signal(true);
  readonly lexiName = 'Lexi';
  readonly botAssistantMode = signal<BotAssistantMode>('ASSISTANT');

  lexiDisplayName(): string {
    return this.isAgentMode() ? `${this.lexiName} (Agent)` : this.lexiName;
  }
  readonly botModeSwitching = signal(false);
  readonly botPricing = signal<BotPricing | null>(null);
  readonly platformActionCharges = signal<PlatformActionChargeRow[]>([]);

  llmSettings: BotLlmSettings | null = null;
  llmProvider: 'auto' | 'gemini' | 'anthropic' = 'auto';
  llmModel = '';
  llmLoading = false;
  llmSaving = false;
  llmError = '';
  private appliedLlmProvider: 'auto' | 'gemini' | 'anthropic' = 'auto';
  private appliedLlmModel = '';
  readonly llmProviderOptions = [
    { id: 'auto' as const, label: 'Auto' },
    { id: 'gemini' as const, label: 'Gemini' },
    { id: 'anthropic' as const, label: 'Claude' },
  ];

  articles: HelpArticle[] = [];
  tickets: SupportTicket[] = [];
  platformStatus: PlatformStatusSummary | null = null;
  userDisplayName = '';
  organizationName = '';
  ticketReplyDraft = '';
  botReplyDraft = '';

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
    this.authState.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.organizationName = this.orgContext.organizationName;
      this.cdr.markForCheck();
    });
    this.organizationName = this.orgContext.organizationName;
    this.reload();
  }

  ngAfterViewChecked(): void {
    if (this.scrollPending && this.chatThreadRef) {
      this.scrollPending = false;
      const el = this.chatThreadRef.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.clearTypingTimer();
    this.destroy$.next();
    this.destroy$.complete();
  }

  setTab(tab: HelpTab): void {
    this.actionError.set('');
    this.activeTab.set(tab);
    if (tab !== 'tickets') {
      this.stopPolling();
      this.mobileShowChat.set(false);
    }
    if (tab === 'assistant') {
      this.loadHelpSupportPricing();
      this.loadBotSessions();
      this.loadLlmSettings();
    }
    if (tab === 'ticket' || tab === 'tickets') {
      this.loadHelpSupportPricing();
    }
    if (tab === 'faq' && !this.selectedArticle() && this.filteredArticles.length) {
      this.openArticle(this.filteredArticles[0]);
    }
    if (tab === 'tickets' && this.tickets.length && !this.selectedTicket()) {
      this.openTicket(this.tickets[0]);
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

  get filteredTickets(): SupportTicket[] {
    const q = this.ticketSearch().trim().toLowerCase();
    const sorted = [...this.tickets].sort(
      (a, b) => new Date(b.modifiedAt ?? b.createdAt).getTime() - new Date(a.modifiedAt ?? a.createdAt).getTime(),
    );
    if (!q) {
      return sorted;
    }
    return sorted.filter(
      (t) =>
        t.ticketNumber.toLowerCase().includes(q) ||
        t.subject.toLowerCase().includes(q) ||
        t.category.toLowerCase().includes(q) ||
        this.lastMessagePreview(t).toLowerCase().includes(q),
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

  categoryIcon(category: SupportTicketCategory | string): string {
    switch (category) {
      case 'TECHNICAL':
        return 'build';
      case 'OPERATIONS':
        return 'local_shipping';
      case 'BILLING':
        return 'receipt_long';
      case 'ACCESS':
        return 'vpn_key';
      case 'SECURITY':
        return 'security';
      default:
        return 'help_outline';
    }
  }

  reload(): void {
    this.loading.set(true);
    this.loadError.set('');
    this.actionError.set('');
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
          if (this.activeTab() === 'tickets' && tickets.length && !this.selectedTicket()) {
            this.openTicket(tickets[0]);
          }
          this.cdr.markForCheck();
        },
      });

    this.loadPlatformStatusInBackground(() => firstError, (msg) => {
      firstError = msg;
    });
  }

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
          this.setActionError(err.message);
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

  trackMessage(_: number, item: ChatThreadItem): string {
    return item.id;
  }

  trackDate(_: number, group: { dateKey: string }): string {
    return group.dateKey;
  }

  openTicket(ticket: SupportTicket): void {
    this.selectedTicket.set(ticket);
    this.ticketReplyDraft = '';
    this.isTyping.set(false);
    this.mobileShowChat.set(true);
    this.chatLoading.set(true);
    this.updateSupportPresence(ticket);
    this.startPolling(ticket.id);

    this.helpApi.fetchMyTicketById(ticket.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (detail) => {
        this.applyTicketDetail(detail, true);
        this.chatLoading.set(false);
        this.cdr.markForCheck();
      },
      error: (err: Error) => {
        this.setActionError(err.message);
        this.chatLoading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  backToInbox(): void {
    this.mobileShowChat.set(false);
  }

  onReplyInput(value: string): void {
    this.ticketReplyDraft = value;
    this.isTyping.set(value.trim().length > 0);
    this.clearTypingTimer();
    this.typingTimer = setTimeout(() => {
      if (!this.ticketReplyDraft.trim()) {
        this.isTyping.set(false);
        this.cdr.markForCheck();
      }
    }, 1800);
    this.cdr.markForCheck();
  }

  onReplyKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendTicketReply();
    }
  }

  sendTicketReply(): void {
    const ticket = this.selectedTicket();
    const body = this.ticketReplyDraft.trim();
    if (!ticket || !body || ticket.status === 'CLOSED' || this.sendingReply()) {
      return;
    }
    this.sendingReply.set(true);
    this.isTyping.set(false);
    this.helpApi
      .addTicketMessage(ticket.id, body)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.applyTicketDetail(updated, true);
          this.ticketReplyDraft = '';
          this.sendingReply.set(false);
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.setActionError(err.message);
          this.sendingReply.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  conversation(ticket: SupportTicket | null): ChatThreadItem[] {
    if (!ticket) {
      return [];
    }
    const items: ChatThreadItem[] = [
      {
        id: `desc-${ticket.id}`,
        role: 'REQUESTER',
        author: 'You',
        at: ticket.createdAt,
        body: ticket.description,
        isMine: true,
        dateKey: this.dateKey(ticket.createdAt),
      },
    ];
    for (const message of ticket.messages ?? []) {
      if (message.visibility === 'INTERNAL' || message.authorRole === 'SYSTEM') {
        continue;
      }
      const isMine = message.authorRole === 'REQUESTER';
      items.push({
        id: `msg-${message.id}`,
        role: message.authorRole,
        author: isMine ? 'You' : this.handlerLabel(ticket),
        at: message.createdAt,
        body: message.body,
        isMine,
        dateKey: this.dateKey(message.createdAt),
      });
    }
    return items;
  }

  dateGroups(ticket: SupportTicket | null): { dateKey: string; label: string; items: ChatThreadItem[] }[] {
    const items = this.conversation(ticket);
    const map = new Map<string, ChatThreadItem[]>();
    for (const item of items) {
      const bucket = map.get(item.dateKey) ?? [];
      bucket.push(item);
      map.set(item.dateKey, bucket);
    }
    return [...map.entries()].map(([dateKey, groupItems]) => ({
      dateKey,
      label: this.dateLabel(groupItems[0]?.at ?? dateKey),
      items: groupItems,
    }));
  }

  lastMessagePreview(ticket: SupportTicket): string {
    const publicMessages = (ticket.messages ?? []).filter(
      (m) => m.visibility !== 'INTERNAL' && m.authorRole !== 'SYSTEM',
    );
    if (publicMessages.length) {
      const last = publicMessages[publicMessages.length - 1];
      const prefix = last.authorRole === 'HANDLER' ? 'Support: ' : 'You: ';
      return prefix + this.truncate(last.body, 72);
    }
    return this.truncate(ticket.description, 72);
  }

  unreadCount(ticket: SupportTicket): number {
    const lastReadId = this.lastReadByTicket.get(ticket.id) ?? 0;
    const publicMessages = (ticket.messages ?? []).filter(
      (m) => m.visibility !== 'INTERNAL' && m.authorRole !== 'SYSTEM',
    );
    return publicMessages.filter((m) => m.id > lastReadId && m.authorRole === 'HANDLER').length;
  }

  relativeTime(iso: string | undefined): string {
    if (!iso) {
      return '';
    }
    const then = new Date(iso).getTime();
    const diffMs = Date.now() - then;
    const mins = Math.floor(diffMs / 60_000);
    if (mins < 1) {
      return 'Just now';
    }
    if (mins < 60) {
      return `${mins}m ago`;
    }
    const hours = Math.floor(mins / 60);
    if (hours < 24) {
      return `${hours}h ago`;
    }
    const days = Math.floor(hours / 24);
    if (days < 7) {
      return `${days}d ago`;
    }
    return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  }

  presenceLabel(): string {
    switch (this.supportPresence()) {
      case 'online':
        return 'Support team online';
      case 'away':
        return 'Support reviewing your case';
      default:
        return 'We respond within one business day';
    }
  }

  avatarInitials(name: string): string {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (!parts.length) {
      return '?';
    }
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  private handlerLabel(ticket: SupportTicket): string {
    return ticket.assignedHandlerUsername ? `Support · ${ticket.assignedHandlerUsername}` : 'Support team';
  }

  private applyTicketDetail(detail: SupportTicket, scroll: boolean): void {
    const prevCount = this.selectedTicket()?.messages?.length ?? 0;
    this.selectedTicket.set(detail);
    this.tickets = this.tickets.map((t) => (t.id === detail.id ? { ...t, ...detail } : t));
    this.markTicketRead(detail);
    this.updateSupportPresence(detail);
    if (scroll || (detail.messages?.length ?? 0) > prevCount) {
      this.scrollPending = true;
    }
  }

  private markTicketRead(ticket: SupportTicket): void {
    const publicMessages = (ticket.messages ?? []).filter(
      (m) => m.visibility !== 'INTERNAL' && m.authorRole !== 'SYSTEM',
    );
    const lastId = publicMessages.length ? publicMessages[publicMessages.length - 1].id : 0;
    this.lastReadByTicket.set(ticket.id, lastId);
  }

  private updateSupportPresence(ticket: SupportTicket): void {
    if (ticket.status === 'CLOSED' || ticket.status === 'RESOLVED') {
      this.supportPresence.set('offline');
      return;
    }
    if (ticket.status === 'IN_PROGRESS' || ticket.assignedHandlerUsername) {
      this.supportPresence.set('online');
      return;
    }
    if (ticket.status === 'WAITING_ON_CUSTOMER') {
      this.supportPresence.set('away');
      return;
    }
    this.supportPresence.set('offline');
  }

  private startPolling(ticketId: number): void {
    this.stopPolling();
    this.pollTimer = setInterval(() => {
      if (this.selectedTicket()?.id !== ticketId || this.sendingReply()) {
        return;
      }
      this.helpApi
        .fetchMyTicketById(ticketId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (detail) => {
            const prev = this.selectedTicket();
            const prevLen = prev?.messages?.length ?? 0;
            const nextLen = detail.messages?.length ?? 0;
            if (
              prevLen !== nextLen ||
              prev?.status !== detail.status ||
              prev?.modifiedAt !== detail.modifiedAt
            ) {
              this.applyTicketDetail(detail, nextLen > prevLen);
              this.cdr.markForCheck();
            }
          },
        });
    }, 4000);
  }

  private stopPolling(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  private clearTypingTimer(): void {
    if (this.typingTimer) {
      clearTimeout(this.typingTimer);
      this.typingTimer = null;
    }
  }

  private dateKey(iso: string): string {
    return iso.slice(0, 10);
  }

  private dateLabel(iso: string): string {
    const date = new Date(iso);
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);
    if (this.dateKey(iso) === this.dateKey(today.toISOString())) {
      return 'Today';
    }
    if (this.dateKey(iso) === this.dateKey(yesterday.toISOString())) {
      return 'Yesterday';
    }
    return date.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });
  }

  private truncate(text: string, max: number): string {
    const trimmed = text.replace(/\s+/g, ' ').trim();
    if (trimmed.length <= max) {
      return trimmed;
    }
    return trimmed.slice(0, max - 1) + '…';
  }

  loadHelpSupportPricing(): void {
    this.platformWallet
      .listActiveActionCharges()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (charges) => {
          this.platformActionCharges.set(charges);
          this.cdr.markForCheck();
        },
        error: () => {
          this.cdr.markForCheck();
        },
      });

    this.botApi
      .fetchPricing()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (pricing) => {
          this.botPricing.set(pricing);
          this.cdr.markForCheck();
        },
        error: () => {
          this.cdr.markForCheck();
        },
      });
  }

  private actionChargeLookup(): Map<string, PlatformActionChargeRow> {
    return actionChargeMap(this.platformActionCharges());
  }

  botAssistantPriceLabel(): string {
    const charges = this.actionChargeLookup();
    if (charges.size) {
      const cents = chargeCentsForAction(charges, HELP_SUPPORT_ACTION_CODES.ASSISTANT_MESSAGE);
      if (cents <= 0) {
        return 'Included';
      }
      return formatActionPrice(charges, HELP_SUPPORT_ACTION_CODES.ASSISTANT_MESSAGE, (c, cur) =>
        this.platformWallet.formatCents(c, cur),
      );
    }
    const pricing = this.botPricing();
    if (pricing) {
      return this.botMessagePriceLabel(pricing.assistantMessageCents, pricing.currencyCode);
    }
    return 'Included';
  }

  botAgentPriceLabel(): string {
    const charges = this.actionChargeLookup();
    if (charges.size) {
      return formatActionPrice(
        charges,
        HELP_SUPPORT_ACTION_CODES.AGENT_MESSAGE,
        (c, cur) => this.platformWallet.formatCents(c, cur),
        { perMessage: true },
      );
    }
    const pricing = this.botPricing();
    if (!pricing) {
      return '… / msg';
    }
    return this.botMessagePriceLabel(pricing.agentMessageCents, pricing.currencyCode, true);
  }

  botAgentPriceHint(): string {
    const cents = this.chargeCents(HELP_SUPPORT_ACTION_CODES.AGENT_MESSAGE);
    if (cents <= 0) {
      return 'Agent mode can create user groups, add users to groups, check your wallet, find portal screens, search LDMS architecture, and open support tickets.';
    }
    const agentLabel = this.formatChargeCents(cents);
    return `Agent mode can create user groups, add users to groups, check your wallet, find portal screens, search LDMS architecture, and open support tickets. Each message is billed at ${agentLabel} / msg from your wallet.`;
  }

  supportTicketOpenHint(): string {
    const base = 'Include PO numbers, shipment IDs, or screenshots when relevant.';
    const cents = this.chargeCents(HELP_SUPPORT_ACTION_CODES.TICKET_OPEN);
    if (cents <= 0) {
      return base;
    }
    return `${base} Opening a ticket is billed at ${this.formatChargeCents(cents)} from your prepaid wallet.`;
  }

  liveChatMessageHint(): string {
    const cents = this.chargeCents(HELP_SUPPORT_ACTION_CODES.LIVE_CHAT_MESSAGE);
    if (cents <= 0) {
      return '';
    }
    return `Each message in this conversation is billed at ${this.formatChargeCents(cents)} / msg from your prepaid wallet when applicable.`;
  }

  private chargeCents(actionCode: string): number {
    const charges = this.platformActionCharges();
    if (charges.length) {
      return chargeCentsForAction(charges, actionCode);
    }
    const pricing = this.botPricing();
    if (!pricing) {
      return 0;
    }
    switch (actionCode) {
      case HELP_SUPPORT_ACTION_CODES.AGENT_MESSAGE:
        return pricing.agentMessageCents;
      case HELP_SUPPORT_ACTION_CODES.ASSISTANT_MESSAGE:
        return pricing.assistantMessageCents;
      case HELP_SUPPORT_ACTION_CODES.TICKET_OPEN:
        return pricing.supportTicketOpenCents;
      case HELP_SUPPORT_ACTION_CODES.LIVE_CHAT_MESSAGE:
        return pricing.liveChatMessageCents;
      case HELP_SUPPORT_ACTION_CODES.SESSION_START:
        return pricing.sessionStartCents;
      default:
        return 0;
    }
  }

  private formatChargeCents(cents: number): string {
    const currency = this.botPricing()?.currencyCode ?? 'USD';
    return this.platformWallet.formatCents(cents, currency);
  }

  private botMessagePriceLabel(cents: number, currencyCode: string, perMessageSuffix = false): string {
    if (cents <= 0) {
      return 'Included';
    }
    const formatted = this.platformWallet.formatCents(cents, currencyCode);
    return perMessageSuffix ? `${formatted} / msg` : formatted;
  }

  loadBotSessions(): void {
    this.botChatLoading.set(true);
    this.botApi
      .fetchMySessions()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (sessions) => {
          this.botSessions.set(sessions);
          if (!this.selectedBotSession() && sessions.length) {
            this.openBotSession(sessions[0].sessionId);
          } else {
            this.botChatLoading.set(false);
          }
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.setActionError(err.message);
          this.botChatLoading.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  startBotChat(): void {
    if (this.botStarting()) {
      return;
    }
    this.botStarting.set(true);
    this.botApi
      .startSession(undefined, this.botAssistantMode())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ session, pricing }) => {
          if (pricing) {
            this.botPricing.set(pricing);
          }
          this.botSessions.set([session, ...this.botSessions()]);
          this.selectedBotSession.set(session);
          this.botAssistantMode.set(session.assistantMode ?? 'ASSISTANT');
          this.botStarting.set(false);
          this.botChatLoading.set(false);
          this.scrollPending = true;
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.setActionError(err.message);
          this.botStarting.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  openBotSession(sessionId: string): void {
    this.botChatLoading.set(true);
    this.botApi
      .fetchSession(sessionId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (session) => {
          this.selectedBotSession.set(session);
          this.botAssistantMode.set(session.assistantMode ?? 'ASSISTANT');
          this.botChatLoading.set(false);
          this.botAwaitingReply.set(false);
          this.assistantOnline.set(true);
          this.scrollPending = true;
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.setActionError(err.message);
          this.botChatLoading.set(false);
          this.assistantOnline.set(!this.isUnreachableError(err.message));
          this.cdr.markForCheck();
        },
      });
  }

  onBotReplyInput(value: string): void {
    this.botReplyDraft = value;
    this.cdr.markForCheck();
  }

  onBotReplyKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendBotReply();
    }
  }

  sendBotReply(): void {
    const session = this.selectedBotSession();
    const body = this.botReplyDraft.trim();
    if (!session || !body || this.botSending() || this.botModeSwitching()) {
      return;
    }

    const assistantMode = this.botAssistantMode();

    const optimistic: BotChatMessage = {
      id: `pending-${Date.now()}`,
      role: 'user',
      body,
      sentAt: new Date().toISOString(),
    };
    this.selectedBotSession.set({
      ...session,
      messages: [...(session.messages ?? []), optimistic],
      messageCount: (session.messageCount ?? 0) + 1,
      lastMessageAt: optimistic.sentAt,
    });
    this.botReplyDraft = '';
    this.scrollPending = true;
    this.cdr.markForCheck();

    // Paint the user's bubble first, then show Lexi typing and call the API.
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        this.botSending.set(true);
        this.botAwaitingReply.set(true);
        this.actionError.set('');
        this.cdr.markForCheck();

        this.botApi
          .sendMessage(session.sessionId, body, assistantMode)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (updated) => {
              this.selectedBotSession.set(updated);
              this.botAssistantMode.set(updated.assistantMode ?? assistantMode);
              this.botSending.set(false);
              this.botAwaitingReply.set(false);
              this.assistantOnline.set(true);
              this.actionError.set('');
              this.scrollPending = true;
              this.cdr.markForCheck();
            },
            error: (err: Error) => {
              this.selectedBotSession.set(session);
              this.botReplyDraft = body;
              this.botSending.set(false);
              this.botAwaitingReply.set(false);
              this.assistantOnline.set(!this.isUnreachableError(err.message));
              this.setActionError(err.message);
              this.cdr.markForCheck();
            },
          });
      });
    });
  }

  private isUnreachableError(message: string): boolean {
    const normalized = message.toLowerCase();
    return (
      normalized.includes('reach') ||
      normalized.includes('gateway') ||
      normalized.includes('not found') ||
      normalized.includes('running')
    );
  }

  isAgentMode(): boolean {
    return this.botAssistantMode() === 'AGENT';
  }

  loadLlmSettings(): void {
    this.llmLoading = true;
    this.llmError = '';
    this.botLlmApi
      .currentSettings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (settings) => {
          this.llmSettings = settings;
          this.applyLlmSettingsToForm(settings);
          this.captureAppliedLlmState();
          this.llmLoading = false;
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.llmError = err.message;
          this.llmLoading = false;
          this.cdr.markForCheck();
        },
      });
  }

  onLlmProviderChange(): void {
    const options = this.llmModelOptions;
    this.llmModel = options.length ? options[0].modelId : '';
    this.cdr.markForCheck();
  }

  onLlmModelChange(): void {
    this.cdr.markForCheck();
  }

  llmHasPendingChanges(): boolean {
    if (this.llmLoading) {
      return false;
    }
    return this.llmProvider !== this.appliedLlmProvider || this.llmModel !== this.appliedLlmModel;
  }

  applyLlmSettings(): void {
    if (this.llmSaving || !this.llmHasPendingChanges()) {
      return;
    }
    if (this.llmProvider === 'gemini' && !this.llmSettings?.geminiConfigured) {
      this.notifications.error('Gemini is not configured on this server (missing API key).');
      return;
    }
    if (this.llmProvider === 'anthropic' && !this.llmSettings?.anthropicConfigured) {
      this.notifications.error('Claude is not configured on this server (missing API key).');
      return;
    }
    if (this.llmProvider !== 'auto' && !this.llmModel?.trim()) {
      this.notifications.error('Select a model before applying.');
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

    this.botLlmApi
      .updateRuntime(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (settings) => {
          this.llmSettings = settings;
          this.applyLlmSettingsToForm(settings);
          this.captureAppliedLlmState();
          this.llmSaving = false;
          this.notifications.success(`AI model updated to ${this.llmActiveLabel()}`);
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.llmError = err.message;
          this.llmSaving = false;
          this.notifications.error(err.message);
          this.cdr.markForCheck();
        },
      });
  }

  get llmModelOptions(): Array<{ modelId: string; label: string }> {
    const catalog = this.llmSettings?.modelCatalog ?? [];
    const provider =
      this.llmProvider === 'auto' ? (this.llmSettings?.activeProvider ?? 'anthropic') : this.llmProvider;
    return catalog
      .filter((row) => row.providerId === provider)
      .map((row) => ({ modelId: row.modelId, label: row.label }));
  }

  llmActiveLabel(): string {
    if (!this.llmSettings) {
      return '—';
    }
    const provider = this.formatLlmProviderLabel(this.llmSettings.activeProvider);
    const model = this.llmSettings.activeModel ?? '—';
    return `${provider} · ${model}`;
  }

  private formatLlmProviderLabel(provider: string | null | undefined): string {
    if (!provider) {
      return '—';
    }
    const match = this.llmProviderOptions.find((row) => row.id === provider);
    return match?.label ?? provider;
  }

  llmProvidersAvailable(): boolean {
    return !!(this.llmSettings?.geminiConfigured || this.llmSettings?.anthropicConfigured);
  }

  private applyLlmSettingsToForm(settings: BotLlmSettings): void {
    const provider = settings.runtimeProvider ?? settings.activeProvider ?? 'auto';
    if (provider === 'gemini') {
      this.llmProvider = 'gemini';
      this.llmModel = settings.runtimeGeminiModel ?? settings.activeModel ?? '';
    } else if (provider === 'anthropic') {
      this.llmProvider = 'anthropic';
      this.llmModel = settings.runtimeAnthropicModel ?? settings.activeModel ?? '';
    } else {
      this.llmProvider = 'auto';
      this.llmModel = '';
    }
  }

  private captureAppliedLlmState(): void {
    this.appliedLlmProvider = this.llmProvider;
    this.appliedLlmModel = this.llmModel;
  }

  setBotAssistantMode(mode: BotAssistantMode): void {
    if (this.botModeSwitching() || this.botAssistantMode() === mode) {
      return;
    }
    const session = this.selectedBotSession();
    if (!session) {
      this.botAssistantMode.set(mode);
      this.notifications.success(
        mode === 'AGENT' ? 'Lexi Agent selected for new chats.' : 'Lexi Assistant selected for new chats.',
      );
      this.cdr.markForCheck();
      return;
    }
    const previousMode = this.botAssistantMode();
    this.botAssistantMode.set(mode);
    this.botModeSwitching.set(true);
    this.cdr.markForCheck();
    this.botApi
      .setAssistantMode(session.sessionId, mode)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.selectedBotSession.set(updated);
          this.botAssistantMode.set(updated.assistantMode ?? mode);
          this.botModeSwitching.set(false);
          this.notifications.success(
            mode === 'AGENT' ? 'Lexi Agent mode — tools are active.' : 'Lexi Assistant mode — workflow help.',
          );
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.botAssistantMode.set(previousMode);
          this.setActionError(err.message);
          this.botModeSwitching.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  rateBotChat(score: number): void {
    const session = this.selectedBotSession();
    if (!session || session.satisfactionScore || this.botRating()) {
      return;
    }
    this.botRating.set(true);
    this.botApi
      .rateSession(session.sessionId, score)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.selectedBotSession.set(updated);
          this.botRating.set(false);
          this.cdr.markForCheck();
        },
        error: () => {
          this.botRating.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  botConversation(session: BotChatSession | null): ChatThreadItem[] {
    if (!session?.messages?.length) {
      return [];
    }
    return session.messages.map((message) => ({
      id: message.id,
      role: message.role === 'user' ? 'REQUESTER' : message.role === 'bot' ? 'HANDLER' : 'SYSTEM',
      author: message.role === 'user' ? 'You' : message.role === 'bot' ? this.lexiDisplayName() : 'System',
      at: message.sentAt,
      body: message.body,
      isMine: message.role === 'user',
      dateKey: this.dateKey(message.sentAt),
    }));
  }

  botDateGroups(session: BotChatSession | null): { dateKey: string; label: string; items: ChatThreadItem[] }[] {
    const items = this.botConversation(session);
    const map = new Map<string, ChatThreadItem[]>();
    for (const item of items) {
      const bucket = map.get(item.dateKey) ?? [];
      bucket.push(item);
      map.set(item.dateKey, bucket);
    }
    return [...map.entries()].map(([dateKey, groupItems]) => ({
      dateKey,
      label: this.dateLabel(groupItems[0]?.at ?? dateKey),
      items: groupItems,
    }));
  }

  trackBotSession(_: number, item: BotChatSession): string {
    return item.sessionId;
  }

  isWalletActionError(message: string): boolean {
    const normalized = message.toLowerCase();
    return (
      normalized.includes('wallet') ||
      normalized.includes('prepaid') ||
      normalized.includes('billing') ||
      normalized.includes('top up')
    );
  }

  clearActionError(): void {
    this.actionError.set('');
    this.cdr.markForCheck();
  }

  private setActionError(message: string): void {
    this.actionError.set(message);
    this.cdr.markForCheck();
  }
}
