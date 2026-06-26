import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, catchError, finalize, of, startWith, switchMap } from 'rxjs';
import {
  AdminSupportTicket,
  HelpSupportAdminService,
  SupportTicketStatus,
} from '../../services/help-support-admin.service';
import {
  LxExportFormat,
  downloadBlob,
  exportClientTableAsCsv,
  exportFilename,
} from '../../../../shared/utils/lx-export.util';

type StatusFilter = 'ALL' | SupportTicketStatus;

interface AdminChatThreadItem {
  id: string;
  role: string;
  author: string;
  at: string;
  body: string;
  isMine: boolean;
  internal: boolean;
  dateKey: string;
}

@Component({
  selector: 'app-help-support-admin-page',
  templateUrl: './help-support-admin-page.component.html',
  styleUrl: './help-support-admin-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class HelpSupportAdminPageComponent implements OnInit, OnDestroy, AfterViewChecked {
  loading = true;
  detailLoading = false;
  actionBusy = false;
  sendingMessage = false;
  error = '';
  detailError = '';
  tickets: AdminSupportTicket[] = [];
  selected: AdminSupportTicket | null = null;
  messageDraft = '';
  internalNote = false;

  readonly search = signal('');
  readonly statusFilter = signal<StatusFilter>('ALL');
  readonly isTyping = signal(false);
  readonly mobileShowChat = signal(false);

  @ViewChild('chatThread') chatThreadRef?: ElementRef<HTMLElement>;

  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private typingTimer: ReturnType<typeof setTimeout> | null = null;
  private scrollPending = false;

  readonly statusFilters: { id: StatusFilter; label: string }[] = [
    { id: 'ALL', label: 'All' },
    { id: 'OPEN', label: 'Open' },
    { id: 'IN_PROGRESS', label: 'In progress' },
    { id: 'WAITING_ON_CUSTOMER', label: 'Waiting' },
    { id: 'RESOLVED', label: 'Resolved' },
    { id: 'CLOSED', label: 'Closed' },
  ];

  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new Subject<void>();

  /** When the page is opened from a bell notification, auto-select this ticket once loaded. */
  private pendingTicketId: number | null = null;

  constructor(
    private readonly title: Title,
    private readonly helpApi: HelpSupportAdminService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Help & Support | LX Admin');
    const ticketIdParam = Number(this.route.snapshot.queryParamMap.get('ticketId'));
    this.pendingTicketId = Number.isFinite(ticketIdParam) && ticketIdParam > 0 ? ticketIdParam : null;
    this.reload$
      .pipe(
        startWith(undefined),
        switchMap(() => {
          this.loading = true;
          this.error = '';
          this.cdr.markForCheck();
          return this.helpApi.fetchAllTickets().pipe(
            catchError((err: Error) => {
              this.error = err.message;
              return of([] as AdminSupportTicket[]);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((tickets) => this.applyTickets(tickets));
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
  }

  get openTicketCount(): number {
    return this.tickets.filter((t) => t.status === 'OPEN').length;
  }

  get inProgressCount(): number {
    return this.tickets.filter((t) => t.status === 'IN_PROGRESS').length;
  }

  get waitingCount(): number {
    return this.tickets.filter((t) => t.status === 'WAITING_ON_CUSTOMER').length;
  }

  get isClosed(): boolean {
    return this.selected?.status === 'CLOSED';
  }

  statDisplay(value: number): string | number {
    return this.loading ? '—' : value;
  }

  get filteredTickets(): AdminSupportTicket[] {
    const q = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    return this.tickets.filter((t) => {
      if (status !== 'ALL' && t.status !== status) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        t.ticketNumber.toLowerCase().includes(q) ||
        t.subject.toLowerCase().includes(q) ||
        t.requesterUsername.toLowerCase().includes(q) ||
        t.requesterEmail.toLowerCase().includes(q) ||
        (t.assignedHandlerUsername ?? '').toLowerCase().includes(q) ||
        t.category.toLowerCase().includes(q)
      );
    });
  }


  conversationItems(): AdminChatThreadItem[] {
    if (!this.selected) {
      return [];
    }
    const items: AdminChatThreadItem[] = [
      {
        id: `desc-${this.selected.id}`,
        role: 'REQUESTER',
        author: this.selected.requesterUsername,
        at: this.selected.createdAt,
        body: this.selected.description,
        isMine: false,
        internal: false,
        dateKey: this.dateKey(this.selected.createdAt),
      },
    ];
    for (const message of this.selected.messages ?? []) {
      const internal = message.visibility === 'INTERNAL' || message.authorRole === 'SYSTEM';
      items.push({
        id: `msg-${message.id}`,
        role: message.authorRole,
        author: this.messageAuthorLabel({
          author: message.authorUsername,
          role: message.authorRole,
          internal,
        }),
        at: message.createdAt,
        body: message.body,
        isMine: message.authorRole === 'HANDLER' && !internal,
        internal,
        dateKey: this.dateKey(message.createdAt),
      });
    }
    return items;
  }

  dateGroups(): { dateKey: string; label: string; items: AdminChatThreadItem[] }[] {
    const map = new Map<string, AdminChatThreadItem[]>();
    for (const item of this.conversationItems()) {
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

  reload(): void {
    this.reload$.next();
  }

  selectTicket(ticket: AdminSupportTicket): void {
    this.selected = ticket;
    this.detailError = '';
    this.messageDraft = '';
    this.internalNote = false;
    this.isTyping.set(false);
    this.mobileShowChat.set(true);
    this.startPolling(ticket.id);
    this.loadTicketDetail(ticket.id);
  }

  backToInbox(): void {
    this.mobileShowChat.set(false);
  }

  onReplyInput(value: string): void {
    this.messageDraft = value;
    this.isTyping.set(value.trim().length > 0);
    this.clearTypingTimer();
    this.typingTimer = setTimeout(() => {
      if (!this.messageDraft.trim()) {
        this.isTyping.set(false);
        this.cdr.markForCheck();
      }
    }, 1800);
    this.cdr.markForCheck();
  }

  onReplyKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  relativeTime(iso: string | undefined): string {
    if (!iso) {
      return '';
    }
    const diffMs = Date.now() - new Date(iso).getTime();
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

  lastMessagePreview(ticket: AdminSupportTicket): string {
    const messages = ticket.messages ?? [];
    if (messages.length) {
      const last = messages[messages.length - 1];
      const prefix =
        last.visibility === 'INTERNAL'
          ? 'Note: '
          : last.authorRole === 'HANDLER'
            ? 'You: '
            : `${ticket.requesterUsername}: `;
      return prefix + this.truncate(last.body, 72);
    }
    return this.truncate(ticket.description, 72);
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

  setStatusFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    const rows = this.filteredTickets;
    if (rows.length && !rows.some((t) => t.ticketNumber === this.selected?.ticketNumber)) {
      this.selectTicket(rows[0]);
    } else {
      this.cdr.markForCheck();
    }
  }

  startWork(): void {
    if (!this.selected) return;
    this.runAction(() =>
      this.selected!.status === 'OPEN' && !this.selected!.assignedHandlerUsername
        ? this.helpApi.assignToMe(this.selected!.id)
        : this.helpApi.updateStatus(this.selected!.id, 'IN_PROGRESS'),
    );
  }

  markWaitingOnCustomer(): void {
    this.transitionStatus('WAITING_ON_CUSTOMER');
  }

  resolveTicket(): void {
    this.transitionStatus('RESOLVED');
  }

  closeTicket(): void {
    this.transitionStatus('CLOSED');
  }

  assignToMe(): void {
    if (!this.selected) return;
    this.runAction(() => this.helpApi.assignToMe(this.selected!.id));
  }

  sendMessage(): void {
    const ticket = this.selected;
    const body = this.messageDraft.trim();
    if (!ticket || !body || this.sendingMessage || this.isClosed) {
      return;
    }
    this.sendingMessage = true;
    this.detailError = '';
    this.cdr.markForCheck();
    this.helpApi
      .addMessage(ticket.id, body, this.internalNote ? 'INTERNAL' : 'PUBLIC')
      .pipe(
        finalize(() => {
          this.sendingMessage = false;
          this.cdr.markForCheck();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updated) => {
          this.messageDraft = '';
          this.internalNote = false;
          this.applyUpdatedTicket(updated);
          this.snackBar.open('Message sent.', 'Close', { duration: 3000, panelClass: ['app-snackbar-success'] });
        },
        error: (err: Error) => {
          this.detailError = err.message;
        },
      });
  }

  exportAs(format: LxExportFormat): void {
    if (format === 'csv') {
      const ok = exportClientTableAsCsv(
        format,
        this.filteredTickets,
        [
          { header: 'ticketNumber', value: (t) => t.ticketNumber },
          { header: 'subject', value: (t) => t.subject },
          { header: 'status', value: (t) => t.status },
          { header: 'priority', value: (t) => t.priority },
          { header: 'category', value: (t) => t.category },
          { header: 'requester', value: (t) => t.requesterUsername },
          { header: 'handler', value: (t) => t.assignedHandlerUsername ?? '' },
          { header: 'createdAt', value: (t) => t.createdAt },
        ],
        'support-tickets',
        (message) => this.snackBar.open(message, 'Close', { duration: 4500 }),
      );
      if (ok) {
        this.snackBar.open('Exported queue snapshot (CSV).', 'Close', { duration: 3500, panelClass: ['app-snackbar-success'] });
      }
      return;
    }

    const status = this.statusFilter();
    this.helpApi
      .exportTickets(format, {
        search: this.search().trim() || undefined,
        status: status === 'ALL' ? undefined : status,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (resp) => {
          const blob = resp.body;
          if (!blob) {
            this.snackBar.open('Export returned no data.', 'Close', { duration: 4500 });
            return;
          }
          downloadBlob(blob, exportFilename('support-tickets', format));
          this.snackBar.open(`Exported support tickets (${format.toUpperCase()}).`, 'Close', {
            duration: 3500,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: (err: Error) => {
          this.snackBar.open(err.message || 'Export failed.', 'Close', { duration: 5000 });
        },
      });
  }

  goSystemHealth(): void {
    void this.router.navigate(['/system/health']);
  }

  statusLabel(status: string): string {
    return status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  }

  categoryIcon(category: string): string {
    switch (category) {
      case 'TECHNICAL':
        return 'build';
      case 'BILLING':
        return 'receipt_long';
      case 'ACCESS':
        return 'vpn_key';
      case 'SECURITY':
        return 'shield';
      case 'OPERATIONS':
        return 'local_shipping';
      default:
        return 'support_agent';
    }
  }

  priorityTone(priority: string): string {
    switch (priority) {
      case 'URGENT':
        return 'urgent';
      case 'HIGH':
        return 'high';
      case 'LOW':
        return 'low';
      default:
        return 'normal';
    }
  }

  messageAuthorLabel(item: { author?: string; role?: string; internal?: boolean }): string {
    if (item.role === 'SYSTEM') {
      return 'System';
    }
    if (item.internal) {
      return `${item.author ?? 'Handler'} · internal note`;
    }
    if (item.role === 'HANDLER') {
      return `${item.author ?? 'Handler'} · support`;
    }
    return item.author ?? 'Requester';
  }

  trackTicket(_: number, t: AdminSupportTicket): string {
    return t.ticketNumber;
  }

  trackMessage(_: number, item: AdminChatThreadItem): string {
    return item.id;
  }

  trackDate(_: number, group: { dateKey: string }): string {
    return group.dateKey;
  }

  private transitionStatus(status: SupportTicketStatus): void {
    if (!this.selected) return;
    this.runAction(() => this.helpApi.updateStatus(this.selected!.id, status));
  }

  private runAction(request: () => ReturnType<HelpSupportAdminService['updateStatus']>): void {
    if (!this.selected || this.actionBusy) {
      return;
    }
    this.actionBusy = true;
    this.detailError = '';
    this.cdr.markForCheck();
    request()
      .pipe(
        finalize(() => {
          this.actionBusy = false;
          this.cdr.markForCheck();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updated) => {
          this.applyUpdatedTicket(updated);
          this.snackBar.open('Ticket updated.', 'Close', { duration: 3000, panelClass: ['app-snackbar-success'] });
        },
        error: (err: Error) => {
          this.detailError = err.message;
        },
      });
  }

  private loadTicketDetail(id: number): void {
    this.detailLoading = true;
    this.cdr.markForCheck();
    this.helpApi
      .fetchTicketById(id)
      .pipe(
        finalize(() => {
          this.detailLoading = false;
          this.cdr.markForCheck();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (ticket) => {
          this.applyUpdatedTicket(ticket);
          this.scrollPending = true;
        },
        error: (err: Error) => {
          this.detailError = err.message;
        },
      });
  }

  private applyTickets(tickets: AdminSupportTicket[]): void {
    this.tickets = tickets;
    if (this.pendingTicketId) {
      const deepLinked = tickets.find((t) => t.id === this.pendingTicketId);
      this.pendingTicketId = null;
      if (deepLinked) {
        this.selectTicket(deepLinked);
        this.loading = false;
        this.cdr.markForCheck();
        return;
      }
    }
    if (tickets.length && !this.selected) {
      this.selectTicket(tickets[0]);
    } else if (this.selected) {
      const match = tickets.find((t) => t.id === this.selected!.id);
      if (match) {
        this.selected = { ...this.selected, ...match, messages: this.selected.messages };
      } else {
        this.selected = tickets[0] ?? null;
        if (this.selected) {
          this.loadTicketDetail(this.selected.id);
        }
      }
    }
    this.loading = false;
    this.cdr.markForCheck();
  }

  private applyUpdatedTicket(ticket: AdminSupportTicket): void {
    const prevLen = this.selected?.messages?.length ?? 0;
    this.selected = ticket;
    this.tickets = this.tickets.map((t) => (t.id === ticket.id ? { ...t, ...ticket, messages: ticket.messages } : t));
    if ((ticket.messages?.length ?? 0) > prevLen) {
      this.scrollPending = true;
    }
    this.cdr.markForCheck();
  }

  private startPolling(ticketId: number): void {
    this.stopPolling();
    this.pollTimer = setInterval(() => {
      if (this.selected?.id !== ticketId || this.sendingMessage || this.detailLoading) {
        return;
      }
      this.helpApi
        .fetchTicketById(ticketId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (detail) => {
            const prevLen = this.selected?.messages?.length ?? 0;
            const nextLen = detail.messages?.length ?? 0;
            if (
              prevLen !== nextLen ||
              this.selected?.status !== detail.status ||
              this.selected?.modifiedAt !== detail.modifiedAt
            ) {
              this.applyUpdatedTicket(detail);
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
    const today = new Date();
    const yesterday = new Date();
    yesterday.setDate(today.getDate() - 1);
    if (this.dateKey(iso) === this.dateKey(today.toISOString())) {
      return 'Today';
    }
    if (this.dateKey(iso) === this.dateKey(yesterday.toISOString())) {
      return 'Yesterday';
    }
    return new Date(iso).toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });
  }

  private truncate(text: string, max: number): string {
    const trimmed = text.replace(/\s+/g, ' ').trim();
    if (trimmed.length <= max) {
      return trimmed;
    }
    return trimmed.slice(0, max - 1) + '…';
  }
}
