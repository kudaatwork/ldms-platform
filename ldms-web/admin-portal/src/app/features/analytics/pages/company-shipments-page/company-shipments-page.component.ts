import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, finalize, switchMap, takeUntil } from 'rxjs';
import { PlatformOpsAdminService } from '../../../../core/services/platform-ops-admin.service';
import type { PlatformCompanyOps, PlatformShipmentOps } from '../../../../core/services/platform-ops-mock.data';

@Component({
  selector: 'app-company-shipments-page',
  templateUrl: './company-shipments-page.component.html',
  styleUrl: './company-shipments-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class CompanyShipmentsPageComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  loading = true;
  organizationId = 0;
  company: PlatformCompanyOps | null = null;
  shipments: PlatformShipmentOps[] = [];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
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
          this.loading = true;
          return this.platformOps.refresh();
        }),
      )
      .subscribe({
        next: (summary) => {
          this.company = summary.companies.find((c) => c.organizationId === this.organizationId) ?? null;
          this.title.setTitle(
            this.company ? `${this.company.organizationName} shipments | LX Admin` : 'Company shipments | LX Admin',
          );
          this.platformOps
            .fetchCompanyShipments(this.organizationId)
            .pipe(
              finalize(() => {
                this.loading = false;
                this.cdr.markForCheck();
              }),
            )
            .subscribe((rows) => {
              this.shipments = rows;
              this.cdr.markForCheck();
            });
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  openShipment(row: PlatformShipmentOps): void {
    void this.router.navigate(['/analytics/companies', this.organizationId, 'shipments', row.shipmentId]);
  }

  statusTone(status: string): string {
    return status.toLowerCase().replace('_', '-');
  }

  trackByShipmentId(_i: number, row: PlatformShipmentOps): number {
    return row.shipmentId;
  }
}
