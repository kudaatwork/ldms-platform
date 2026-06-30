import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, interval, startWith, switchMap, takeUntil } from 'rxjs';
import { DriverPortalService } from '../../services/driver-portal.service';
import { ReceiverContactDto, TripMessageDto } from '../../models/driver-portal.model';

const POLL_MS = 5000;

@Component({
  selector: 'app-driver-chat',
  templateUrl: './driver-chat.component.html',
  styleUrls: ['./driver-chat.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DriverChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('scrollBody') scrollBody?: ElementRef<HTMLDivElement>;

  tripId = 0;
  loading = true;
  sending = false;
  error = '';
  draft = '';

  messages: TripMessageDto[] = [];
  receiver: ReceiverContactDto | null = null;
  myRole = '';

  private lastCount = 0;
  private shouldScroll = false;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly driverService: DriverPortalService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.tripId = Number(this.route.snapshot.paramMap.get('tripId') ?? 0);
    if (!this.tripId) {
      this.error = 'Invalid trip.';
      this.loading = false;
      return;
    }
    // Poll the thread so new inbound messages appear without a manual refresh.
    interval(POLL_MS)
      .pipe(
        startWith(0),
        switchMap(() => this.driverService.getTripChat(this.tripId)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (state) => {
          this.applyState(state.messages, state.receiverContact, state.myRole);
          this.loading = false;
          this.error = '';
          this.cdr.markForCheck();
        },
        error: (e: Error) => {
          this.error = e.message;
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll && this.scrollBody) {
      this.scrollBody.nativeElement.scrollTop = this.scrollBody.nativeElement.scrollHeight;
      this.shouldScroll = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get receiverInitials(): string {
    const name = (this.receiver?.name ?? '').trim();
    if (!name) return 'R';
    const parts = name.split(/\s+/);
    return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || 'R';
  }

  get callHref(): string | null {
    return this.receiver?.phoneNumber ? `tel:${this.receiver.phoneNumber}` : null;
  }

  goBack(): void {
    void this.router.navigate(['/driver/trip', this.tripId]);
  }

  send(): void {
    const body = this.draft.trim();
    if (!body || this.sending) {
      return;
    }
    this.sending = true;
    this.error = '';
    this.cdr.markForCheck();
    this.driverService.sendTripMessage(this.tripId, body).subscribe({
      next: (state) => {
        this.draft = '';
        this.sending = false;
        this.applyState(state.messages, state.receiverContact, state.myRole);
        this.cdr.markForCheck();
      },
      error: (e: Error) => {
        this.error = e.message;
        this.sending = false;
        this.cdr.markForCheck();
      },
    });
  }

  trackById(_: number, m: TripMessageDto): number {
    return m.id;
  }

  private applyState(
    messages: TripMessageDto[],
    receiver: ReceiverContactDto | undefined,
    myRole: string,
  ): void {
    this.messages = messages;
    if (receiver) {
      this.receiver = receiver;
    }
    this.myRole = myRole;
    if (messages.length !== this.lastCount) {
      this.shouldScroll = true;
      this.lastCount = messages.length;
    }
  }
}
