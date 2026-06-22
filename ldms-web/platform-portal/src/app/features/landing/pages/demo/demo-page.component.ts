import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, signal } from '@angular/core';
import { Router } from '@angular/router';
import { LandingMotionService } from '../../services/landing-motion.service';

@Component({
  selector: 'app-demo-page',
  templateUrl: './demo-page.component.html',
  styleUrls: ['./demo-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DemoPageComponent implements AfterViewInit {
  readonly activeScene = signal(0);

  readonly demoStages = [
    {
      icon: 'local_shipping',
      title: 'Dispatch & release',
      detail: 'Shipments move from docs to ready-for-pickup — events flow to fleet and billing without duplicate logic.',
      accent: '#3b82f6',
    },
    {
      icon: 'map',
      title: 'Live trip walkthrough',
      detail: 'See dispatch → trip tracking → stops and notifications as events flow through the platform in real time.',
      accent: '#2dd4bf',
    },
    {
      icon: 'account_balance_wallet',
      title: 'Billing & wallet demo',
      detail: 'Watch prepaid wallet top-up, per-action deductions, and subscription packages configured from the admin console.',
      accent: '#f97316',
    },
    {
      icon: 'task_alt',
      title: 'Delivery & settlement',
      detail: 'GRV sign-off closes the trip, triggers invoicing, and keeps wallet balances reconciled for your programme.',
      accent: '#8b5cf6',
    },
  ] as const;

  constructor(
    private readonly router: Router,
    private readonly el: ElementRef<HTMLElement>,
    private readonly motion: LandingMotionService,
  ) {}

  ngAfterViewInit(): void {
    this.motion.initHeroReveal(this.el.nativeElement);
  }

  goContact(): void {
    void this.router.navigate(['/contact']);
  }

  goPricing(): void {
    void this.router.navigate(['/pricing']);
  }
}
