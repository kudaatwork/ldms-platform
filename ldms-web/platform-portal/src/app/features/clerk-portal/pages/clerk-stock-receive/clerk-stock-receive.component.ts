import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { ClerkPortalService } from '../../services/clerk-portal.service';
import { IncomingDeliveryRow, StockReceiveRequest } from '../../models/clerk-portal.model';

@Component({
  selector: 'app-clerk-stock-receive',
  templateUrl: './clerk-stock-receive.component.html',
  styleUrls: ['./clerk-stock-receive.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ClerkStockReceiveComponent implements OnInit, OnDestroy {
  tripId = 0;
  delivery: IncomingDeliveryRow | null = null;

  loading = true;
  error = '';
  submitting = false;
  submitError = '';
  submitSuccess = false;

  quantityReceived: number | null = null;
  notes = '';
  condition: 'GOOD' | 'DAMAGED' | 'PARTIAL' = 'GOOD';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly clerkService: ClerkPortalService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('tripId');
    this.tripId = idParam ? Number(idParam) : 0;
    if (!this.tripId) {
      this.error = 'Invalid delivery reference';
      this.loading = false;
      this.cdr.markForCheck();
      return;
    }
    this.loadDelivery();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadDelivery(): void {
    this.loading = true;
    this.clerkService.getDeliveryDetail(this.tripId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (d) => {
          this.delivery = d;
          this.quantityReceived = d.quantity;
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (e: Error) => {
          this.error = e.message;
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
  }

  submitReceive(): void {
    if (!this.delivery || this.quantityReceived === null || this.quantityReceived < 0) {
      this.submitError = 'Please enter a valid quantity received';
      this.cdr.markForCheck();
      return;
    }

    this.submitting = true;
    this.submitError = '';
    this.cdr.markForCheck();

    const request: StockReceiveRequest = {
      tripId: this.tripId,
      quantityReceived: this.quantityReceived,
      notes: this.notes.trim() || undefined,
      condition: this.condition,
    };

    this.clerkService.receiveStock(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.submitting = false;
          this.submitSuccess = true;
          this.cdr.markForCheck();
          setTimeout(() => {
            void this.router.navigate(['/clerk/workspace']);
          }, 1500);
        },
        error: (e: Error) => {
          this.submitting = false;
          this.submitError = e.message;
          this.cdr.markForCheck();
        },
      });
  }

  goBack(): void {
    void this.router.navigate(['/clerk/workspace']);
  }

  openChat(): void {
    void this.router.navigate(['/clerk/chat', this.tripId]);
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
}
