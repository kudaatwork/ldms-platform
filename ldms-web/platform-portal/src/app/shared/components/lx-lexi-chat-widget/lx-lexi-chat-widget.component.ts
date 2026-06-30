import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { Subject, finalize, takeUntil } from 'rxjs';
import {
  BotChatMessage,
  BotChatService,
  BotChatSession,
} from '../../../features/help-support/services/bot-chat.service';
import { isGuestSessionValidForBrand, LexiGuestChatService } from '../../services/lexi-guest-chat.service';
import { LEXXI_BOT_NAME } from '../../constants/lexxi-bot.constants';
import { LexxiChatLauncherService, LexxiChatBrand } from '../../services/lexxi-chat-launcher.service';

type ChatBubble = {
  id: string;
  role: BotChatMessage['role'];
  body: string;
  sentAt: string;
  isMine: boolean;
  author: string;
};

@Component({
  selector: 'app-lx-lexi-chat-widget',
  templateUrl: './lx-lexi-chat-widget.component.html',
  styleUrl: './lx-lexi-chat-widget.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LxLexiChatWidgetComponent implements OnInit, OnDestroy {
  /** Authenticated workspace chat vs public landing live chat. */
  @Input() mode: 'authenticated' | 'guest' = 'authenticated';

  @ViewChild('threadEl') threadEl?: ElementRef<HTMLElement>;

  open = false;
  session: BotChatSession | null = null;
  draft = '';
  loading = false;
  starting = false;
  sending = false;
  awaitingReply = false;
  error = '';
  chatBrand: LexxiChatBrand = 'live-chat';

  private sessionBrand: LexxiChatBrand | null = null;
  private readonly destroy$ = new Subject<void>();
  private readonly guestBootstrap$ = new Subject<void>();

  constructor(
    private readonly botChat: BotChatService,
    private readonly guestChat: LexiGuestChatService,
    private readonly chatLauncher: LexxiChatLauncherService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.chatLauncher.brand$
      .pipe(takeUntil(this.destroy$))
      .subscribe((brand) => {
        const previous = this.chatBrand;
        this.chatBrand = brand;
        if (previous !== brand && this.isGuest) {
          this.guestBootstrap$.next();
          this.session = null;
          this.sessionBrand = null;
          this.draft = '';
          this.error = '';
          if (this.open) {
            this.bootstrapGuestSession();
          }
        }
        this.cdr.markForCheck();
      });

    this.chatLauncher.launches.pipe(takeUntil(this.destroy$)).subscribe((intent) => {
      if (intent === 'open') {
        this.openPanel();
        return;
      }
      this.toggleOpen();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get isGuest(): boolean {
    return this.mode === 'guest';
  }

  readonly lexxiName = LEXXI_BOT_NAME;

  get isLiveChatBrand(): boolean {
    return this.chatBrand === 'live-chat';
  }

  get panelTitle(): string {
    if (this.isGuest) {
      return this.isLiveChatBrand ? 'Live chat' : `Chat with ${this.lexxiName}`;
    }
    return this.isLiveChatBrand ? 'Live chat' : `Chat with ${this.lexxiName}`;
  }

  get panelSubtitle(): string {
    if (this.isLiveChatBrand) {
      return this.isGuest
        ? 'Talk to a human agent about LDMS, pricing, and onboarding — free for visitors'
        : 'Message our team — a human agent will respond when available';
    }
    return this.isGuest
      ? `${this.lexxiName} is your upbeat LDMS guide — happy to help!`
      : 'Your upbeat LDMS guide — workflows, screens, and how-tos';
  }

  get statusText(): string {
    if (this.awaitingReply) {
      return this.isLiveChatBrand ? 'Sending your message…' : `${this.lexxiName} is typing…`;
    }
    return this.isLiveChatBrand ? 'Agents available — we typically reply soon' : 'Online';
  }

  get inputPlaceholder(): string {
    if (this.isLiveChatBrand) {
      return this.isGuest
        ? 'Type your question — a human agent will reply when available…'
        : 'Message our team — a human agent will respond when available…';
    }
    return this.isGuest
      ? `Ask ${this.lexxiName} about LDMS, pricing, or getting started…`
      : `Message ${this.lexxiName} — e.g. How do I create a PO?`;
  }

  get inputAriaLabel(): string {
    return this.isLiveChatBrand ? 'Message to live chat agent' : `Message to ${this.lexxiName}`;
  }

  get fabLabel(): string {
    return this.isLiveChatBrand ? 'Live chat' : this.lexxiName;
  }

  get headerIcon(): string {
    return this.isLiveChatBrand ? 'forum' : 'smart_toy';
  }

  setChatBrand(brand: LexxiChatBrand): void {
    this.chatLauncher.setBrand(brand);
    this.cdr.markForCheck();
  }

  bubbles(): ChatBubble[] {
    const agentLabel = this.isLiveChatBrand ? 'Support' : this.lexxiName;
    return (this.session?.messages ?? []).map((m) => ({
      id: m.id,
      role: m.role,
      body: m.body,
      sentAt: m.sentAt,
      isMine: m.role === 'user',
      author:
        m.role === 'user'
          ? 'You'
          : m.role === 'system'
            ? 'Support'
            : agentLabel,
    }));
  }

  toggleOpen(): void {
    if (this.open) {
      this.closePanel();
      return;
    }
    this.openPanel();
  }

  openPanel(): void {
    this.open = true;
    this.chatLauncher.setOpen(true);
    const needsGuestBootstrap =
      this.isGuest && (this.sessionBrand !== this.chatBrand || !this.session);
    if ((needsGuestBootstrap || (!this.isGuest && !this.session)) && !this.loading && !this.starting) {
      this.bootstrapSession();
    }
    this.cdr.markForCheck();
  }

  closePanel(): void {
    this.open = false;
    this.chatLauncher.setOpen(false);
    this.cdr.markForCheck();
  }

  onDraftInput(value: string): void {
    this.draft = value;
    this.cdr.markForCheck();
  }

  onDraftKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  sendMessage(): void {
    const body = this.draft.trim();
    const session = this.session;
    if (!session || !body || this.sending || this.loading) {
      return;
    }

    const optimistic: BotChatMessage = {
      id: `pending-${Date.now()}`,
      role: 'user',
      body,
      sentAt: new Date().toISOString(),
    };
    this.session = {
      ...session,
      messages: [...(session.messages ?? []), optimistic],
    };
    this.draft = '';
    this.sending = true;
    this.awaitingReply = true;
    this.error = '';
    this.cdr.markForCheck();
    this.scheduleScroll();

    const request$ = this.isGuest
      ? this.guestChat.sendMessage(this.chatBrand, session.sessionId, body)
      : this.botChat.sendMessage(session.sessionId, body, 'ASSISTANT');

    request$
      .pipe(
        finalize(() => {
          this.sending = false;
          this.awaitingReply = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (updated) => {
          this.session = updated;
          this.scheduleScroll();
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.error = err.message ?? 'Could not send message.';
          this.cdr.markForCheck();
        },
      });
  }

  startNewChat(): void {
    if (this.isGuest) {
      this.guestChat.clearStoredSessionId(this.chatBrand);
    }
    this.session = null;
    this.sessionBrand = null;
    this.error = '';
    this.bootstrapSession();
  }

  goSignIn(): void {
    void this.router.navigate(['/auth/login']);
  }

  private bootstrapSession(): void {
    if (this.isGuest) {
      this.bootstrapGuestSession();
      return;
    }
    this.bootstrapAuthenticatedSession();
  }

  private bootstrapGuestSession(): void {
    this.guestBootstrap$.next();
    const brand = this.chatBrand;
    const storedId = this.guestChat.readStoredSessionId(brand);
    if (storedId) {
      this.loading = true;
      this.guestChat
        .fetchSession(brand, storedId)
        .pipe(
          finalize(() => {
            this.loading = false;
            this.cdr.markForCheck();
          }),
          takeUntil(this.destroy$),
          takeUntil(this.guestBootstrap$),
        )
        .subscribe({
          next: (session) => {
            if (this.chatBrand !== brand) {
              return;
            }
            if (!isGuestSessionValidForBrand(session, brand)) {
              this.guestChat.clearStoredSessionId(brand);
              this.startGuestSession(brand);
              return;
            }
            this.session = session;
            this.sessionBrand = brand;
            this.scheduleScroll();
            this.cdr.markForCheck();
          },
          error: () => {
            if (this.chatBrand !== brand) {
              return;
            }
            this.guestChat.clearStoredSessionId(brand);
            this.startGuestSession(brand);
          },
        });
      return;
    }
    this.startGuestSession(brand);
  }

  private startGuestSession(brand: LexxiChatBrand = this.chatBrand): void {
    this.guestBootstrap$.next();
    this.starting = true;
    this.guestChat
      .startSession(brand)
      .pipe(
        finalize(() => {
          this.starting = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
        takeUntil(this.guestBootstrap$),
      )
      .subscribe({
        next: (session) => {
          if (this.chatBrand !== brand) {
            return;
          }
          this.session = session;
          this.sessionBrand = brand;
          this.scheduleScroll();
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          if (this.chatBrand !== brand) {
            return;
          }
          this.error =
            err.message ??
            (brand === 'live-chat' ? 'Live chat is unavailable right now.' : `${this.lexxiName} is unavailable right now.`);
          this.cdr.markForCheck();
        },
      });
  }

  private bootstrapAuthenticatedSession(): void {
    this.starting = true;
    this.botChat
      .startSession(undefined, 'ASSISTANT')
      .pipe(
        finalize(() => {
          this.starting = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: ({ session }) => {
          this.session = session;
          this.scheduleScroll();
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.error = err.message ?? `${this.lexxiName} is unavailable right now.`;
          this.cdr.markForCheck();
        },
      });
  }

  private scheduleScroll(): void {
    requestAnimationFrame(() => {
      const el = this.threadEl?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    });
  }
}
