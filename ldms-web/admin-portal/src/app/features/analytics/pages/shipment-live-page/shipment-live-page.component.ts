import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { Subject, finalize, switchMap, takeUntil, timer } from 'rxjs';
import { PlatformOpsAdminService } from '../../../../core/services/platform-ops-admin.service';
import type { PlatformShipmentOps } from '../../../../core/services/platform-ops-mock.data';

@Component({
  selector: 'app-shipment-live-page',
  templateUrl: './shipment-live-page.component.html',
  styleUrl: './shipment-live-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ShipmentLivePageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  loading = true;
  organizationId = 0;
  shipmentId = 0;
  shipment: PlatformShipmentOps | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly platformOps: PlatformOpsAdminService,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
  ) {}

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        takeUntil(this.destroy$),
        switchMap((params) => {
          this.organizationId = Number(params.get('orgId'));
          this.shipmentId = Number(params.get('shipmentId'));
          this.loading = true;
          return this.platformOps.fetchShipmentLive(this.shipmentId);
        }),
      )
      .subscribe({
        next: (row) => {
          this.shipment = row;
          this.title.setTitle(row ? `${row.shipmentRef} live | LX Admin` : 'Shipment live | LX Admin');
          this.loading = false;
          this.cdr.markForCheck();
        },
      });

    timer(2500, 2500)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.platformOps.fetchShipmentLive(this.shipmentId)),
      )
      .subscribe((row) => {
        if (row) {
          this.shipment = row;
          this.cdr.markForCheck();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get mapPinX(): number {
    if (!this.shipment) {
      return 50;
    }
    return 15 + ((this.shipment.lng + 20) % 30) * 2.2;
  }

  get mapPinY(): number {
    if (!this.shipment) {
      return 50;
    }
    return 20 + ((Math.abs(this.shipment.lat) % 8) * 8);
  }
}
