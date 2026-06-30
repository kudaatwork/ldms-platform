import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { LandingMotionService } from '../../services/landing-motion.service';
import { LexxiChatLauncherService } from '../../../../shared/services/lexxi-chat-launcher.service';
import { LEXXI_BOT_NAME } from '../../../../shared/constants/lexxi-bot.constants';

@Component({
  selector: 'app-support-page',
  templateUrl: './support-page.component.html',
  styleUrls: ['./support-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SupportPageComponent implements AfterViewInit {
  readonly lexxiName = LEXXI_BOT_NAME;

  readonly channels = [
    {
      icon: 'forum',
      title: 'Live chat',
      badge: 'Free for visitors',
      summary: 'Talk to a human Project LX support agent.',
      details: [
        'Ask about LDMS capabilities, pricing, onboarding, and corridor programmes.',
        'Available on the landing site before you create an account.',
        'Sign in for account-specific questions — agents see your organisation context.',
        'Typically replies within one business day; urgent issues can escalate to tickets after sign-in.',
      ],
      cta: 'Start live chat',
      action: 'live-chat' as const,
    },
    {
      icon: 'smart_toy',
      title: `Chat with ${LEXXI_BOT_NAME}`,
      badge: 'Instant AI guide',
      summary: 'Your upbeat LDMS assistant for quick how-to answers.',
      details: [
        'Explains orders, shipments, trips, fleet, billing, and onboarding in plain language.',
        'Free on the landing page and inside Help & Support for signed-in users.',
        'Great for workflow questions, pricing overviews, and “where do I click?” guidance.',
        'Switch to Live chat or open a ticket when you need a person on the case.',
      ],
      cta: `Ask ${LEXXI_BOT_NAME}`,
      action: 'lexxi' as const,
    },
    {
      icon: 'support_agent',
      title: 'Help & Support workspace',
      badge: 'Signed-in organisations',
      summary: 'Tickets, guides, and agent mode inside your portal.',
      details: [
        'Open and track support tickets with your programme context attached.',
        'Browse documentation and FAQs tailored to organisation users.',
        'Use Lexxi in agent mode for deeper platform tasks (wallet lookups, portal paths, ticket creation).',
        'Available from the top bar after you sign in to your organisation workspace.',
      ],
      cta: 'Sign in to workspace',
      action: 'sign-in' as const,
    },
  ] as const;

  readonly compareRows = [
    {
      feature: 'Who answers',
      liveChat: 'Human support agent',
      lexxi: `${LEXXI_BOT_NAME} AI guide`,
      workspace: 'Tickets + agents + Lexxi',
    },
    {
      feature: 'Best for',
      liveChat: 'Pricing, onboarding, programme questions',
      lexxi: 'Quick LDMS how-to and workflow help',
      workspace: 'Account issues, escalations, audit trail',
    },
    {
      feature: 'Availability',
      liveChat: 'Landing site + signed-in top bar',
      lexxi: 'Landing site + Help & Support',
      workspace: 'Organisation portal (after sign-in)',
    },
    {
      feature: 'Cost for visitors',
      liveChat: 'Free',
      lexxi: 'Free',
      workspace: 'Requires account',
    },
  ] as const;

  readonly faqs = [
    {
      q: 'Which option should I use first?',
      a: 'Try Lexxi for instant answers about how LDMS works. Choose Live chat when you want a human to discuss pricing, onboarding, or a tailored corridor programme. After sign-in, use Help & Support for tickets and account-specific issues.',
    },
    {
      q: 'Is Live chat the same as Lexxi?',
      a: 'No. Live chat routes your message to a human Project LX support agent. Lexxi is an AI assistant that answers from LDMS guides and documentation — not a person.',
    },
    {
      q: 'Can I use support before creating an account?',
      a: 'Yes. Live chat and Lexxi are available on this marketing site without signing in. Create an account when you are ready to onboard an organisation or explore the full portal.',
    },
    {
      q: 'Where do signed-in users get help?',
      a: 'Open Help & Support from your organisation workspace, or use Live chat and Lexxi from the top bar. Live chat and Lexxi remain available alongside tickets and documentation.',
    },
  ] as const;

  constructor(
    private readonly router: Router,
    private readonly el: ElementRef<HTMLElement>,
    private readonly motion: LandingMotionService,
    private readonly chatLauncher: LexxiChatLauncherService,
  ) {}

  ngAfterViewInit(): void {
    this.motion.initHeroReveal(this.el.nativeElement);
  }

  onChannelAction(action: 'live-chat' | 'lexxi' | 'sign-in'): void {
    if (action === 'sign-in') {
      void this.router.navigate(['/auth/login']);
      return;
    }
    this.chatLauncher.openChat(action);
  }

  goContact(): void {
    void this.router.navigate(['/contact']);
  }
}
