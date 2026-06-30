import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { Router } from '@angular/router';
import { Subject, interval, takeUntil, switchMap, startWith } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { ClerkPortalService } from '../../services/clerk-portal.service';
import {
  ClerkProfileDto,
  ClerkWorkspaceMetrics,
  IncomingDeliveryRow,
} from '../../models/clerk-portal.model';

@Component({
  selector: 'app-clerk-workspace',
  templateUrl: './clerk-workspace.component.html',
  styleUrls: ['./clerk-workspace.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ClerkWorkspaceComponent implements OnInit, OnDestroy {
  clerkProfile: ClerkProfileDto | null = null;
  deliveries: IncomingDeliveryRow[] = [];
  metrics: ClerkWorkspaceMetrics = { incomingToday: 0, receivedToday: 0, pendingDeliveries: 0 };

  loadingProfile = true;
  loadingDeliveries = true;
  profileError = '';
  deliveriesError = '';
  refreshing = false;
  lastRefreshed: Date | null = null;

  private readonly POLL_INTERVAL_MS = 30_000;
  private readonly destroy$ = new Subject<void>();
  private readonly manualRefresh$ = new Subject<void>();

  constructor(
    private readonly clerkService: ClerkPortalService,
    private readonly authState: AuthStateService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadProfile();
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get currentUserName(): string {
    const u = this.authState.currentUser;
    if (!u) return 'Clerk';
    return [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email || 'Clerk';
  }

  get branchName(): string {
    return this.clerkProfile?.branchName ?? 'Branch';
  }

  get organizationName(): string {
    return this.clerkProfile?.organizationName ?? 'Organization';
  }

  // ── Profile ───────────────────────────────────────────────────────────────

  private loadProfile(): void {
    this.loadingProfile = true;
    this.clerkService.getMyClerkProfile().pipe(takeUntil(this.destroy$)).subscribe({
      next: (p) => {
        this.clerkProfile = p;
        this.loadingProfile = false;
        this.cdr.markForCheck();
      },
      error: (e: Error) => {
        this.profileError = e.message;
        this.loadingProfile = false;
        this.cdr.markForCheck();
      },
    });
  }

  // ── Deliveries polling ────────────────────────────────────────────────────

  private startPolling(): void {
    this.manualRefresh$
      .pipe(
        startWith(null),
        switchMap(() => interval(this.POLL_INTERVAL_MS).pipe(startWith(0))),
        takeUntil(this.destroy$),
      )
      .subscribe(() => this.loadDeliveries());
  }

  private loadDeliveries(): void {
    this.clerkService.getIncomingDeliveries().pipe(takeUntil(this.destroy$)).subscribe({
      next: (rows) => {
        this.deliveries = rows;
        this.metrics = this.computeMetrics(rows);
        this.loadingDeliveries = false;
        this.deliveriesError = '';
        this.lastRefreshed = new Date();
        this.cdr.markForCheck();
      },
      error: (e: Error) => {
        this.deliveriesError = e.message;
        this.loadingDeliveries = false;
        this.cdr.markForCheck();
      },
    });
  }

  refresh(): void {
    this.refreshing = true;
    this.manualRefresh$.next();
    setTimeout(() => {
      this.refreshing = false;
      this.cdr.markForCheck();
    }, 600);
  }

  openDelivery(tripId: number): void {
    void this.router.navigate(['/clerk/stock-receive', tripId]);
  }

  statusClass(status: string): string {
    const s = status.toUpperCase();
    if (s === 'ARRIVED' || s === 'COUNTING') return 'status--warning';
    if (s === 'OTP_PENDING' || s === 'DELIVERED') return 'status--success';
    if (s === 'IN_TRANSIT') return 'status--info';
    return 'status--muted';
  }

  statusLabel(status: string): string {
    const s = status.toUpperCase();
    if (s === 'ARRIVED') return 'Arrived';
    if (s === 'COUNTING') return 'Counting';
    if (s === 'OTP_PENDING') return 'Awaiting OTP';
    if (s === 'DELIVERED') return 'Delivered';
    if (s === 'IN_TRANSIT') return 'In transit';
    return status;
  }

  private computeMetrics(rows: IncomingDeliveryRow[]): ClerkWorkspaceMetrics {
    const today = new Date().toISOString().slice(0, 10);
    return {
      incomingToday: rows.filter((r) => r.eta?.startsWith(today) || r.arrivedAt?.startsWith(today)).length,
      receivedToday: rows.filter((r) => r.status.toUpperCase() === 'DELIVERED').length,
      pendingDeliveries: rows.filter((r) => !['DELIVERED', 'RETURNED'].includes(r.status.toUpperCase())).length,
    };
  }
}
