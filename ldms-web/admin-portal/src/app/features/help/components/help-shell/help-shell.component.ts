import { Component } from '@angular/core';

interface HelpNavTab {
  label: string;
  route: string;
  icon: string;
  description: string;
}

@Component({
  selector: 'app-help-shell',
  templateUrl: './help-shell.component.html',
  styleUrl: './help-shell.component.scss',
  standalone: false,
})
export class HelpShellComponent {
  readonly tabs: HelpNavTab[] = [
    {
      label: 'Live chat',
      route: '/help/live-chat',
      icon: 'forum',
      description: 'Real-time support conversations with portal users',
    },
    {
      label: 'Demo requisitions',
      route: '/help/requisitions',
      icon: 'event_available',
      description: 'Book-a-demo requests from the platform portal',
    },
    {
      label: 'Bot service',
      route: '/help/bot-service',
      icon: 'smart_toy',
      description: 'Monitor WhatsApp, web, and SMS bot conversations',
    },
  ];
}
