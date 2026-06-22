import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { LandingMotionService } from '../../services/landing-motion.service';

@Component({
  selector: 'app-about-page',
  templateUrl: './about-page.component.html',
  styleUrls: ['./about-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class AboutPageComponent implements AfterViewInit {
  readonly pillars = [
    {
      icon: 'public',
      title: 'Corridor-first',
      copy: 'LDMS is built for road programmes that cross borders, warehouses, and regulatory boundaries — not generic last-mile only.',
    },
    {
      icon: 'hub',
      title: 'Event-driven',
      copy: 'Trips, orders, documents, and billing publish domain events so your ERP, BI, and partner systems stay in sync.',
    },
    {
      icon: 'verified_user',
      title: 'Compliance-ready',
      copy: 'Organisation KYC, driver onboarding, and document trails give programme sponsors audit confidence from day one.',
    },
  ] as const;

  readonly timeline = [
    { phase: '01', title: 'Onboard organisations', detail: 'Suppliers register customers and transporters; LX ops verify corridor participants.' },
    { phase: '02', title: 'Master data & stock', detail: 'Warehouses, products, reservations, and purchase orders align inventory with corridor demand.' },
    { phase: '03', title: 'Dispatch & trips', detail: 'Shipments become live trips with GPS, stops, and driver apps — on the road or via WhatsApp.' },
    { phase: '04', title: 'Delivery & billing', detail: 'GRVs close the loop; invoices, prepaid wallet usage, and subscription packages reconcile spend.' },
  ] as const;

  constructor(
    private readonly router: Router,
    private readonly el: ElementRef<HTMLElement>,
    private readonly motion: LandingMotionService,
  ) {}

  ngAfterViewInit(): void {
    this.motion.initHeroReveal(this.el.nativeElement);
  }

  goPricing(): void {
    void this.router.navigate(['/pricing']);
  }

  goDemo(): void {
    void this.router.navigate(['/demo']);
  }
}
