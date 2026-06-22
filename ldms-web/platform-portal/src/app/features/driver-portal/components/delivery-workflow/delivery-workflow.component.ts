import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { DeliveryWorkflowService } from '../../services/delivery-workflow.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import {
  DeliveryReturnLine,
  DeliveryWorkflowPhase,
  OtpChannel,
  resolveWorkflowPhase,
  TripDeliveryWorkflowResponse,
} from '../../models/driver-portal.model';

function tripStatusOtpPending(status?: string): boolean {
  return (status ?? '').toUpperCase() === 'OTP_PENDING';
}

/** Ordered workflow phases — delivery notes are collected during OTP verify, not as a separate step. */
const PHASES: DeliveryWorkflowPhase[] = [
  'ARRIVAL',
  'STOCK_COUNTING',
  'FINISHED_COUNTING',
  'SEND_OTP',
  'OTP_VERIFICATION',
  'START_RETURN',
  'RETURNS',
  'CONFIRM_RETURN',
];

@Component({
  selector: 'app-delivery-workflow',
  templateUrl: './delivery-workflow.component.html',
  styleUrls: ['./delivery-workflow.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DeliveryWorkflowComponent implements OnInit, OnChanges, OnDestroy {
  @Input() tripId!: number;
  @Input() initialPhase: DeliveryWorkflowPhase = 'ARRIVAL';
  /**
   * When true the component shows an arrival geofence prompt at the top of the ARRIVAL step.
   * Set by the parent page when the live-tracking snapshot indicates the truck is near the destination.
   */
  @Input() arrivalSuggested = false;

  /** Emitted when the entire workflow reaches COMPLETE. */
  @Output() workflowComplete = new EventEmitter<void>();
  /** Emitted on any phase transition. */
  @Output() phaseChange = new EventEmitter<DeliveryWorkflowPhase>();
  /** Emitted after workflow state is loaded from the API. */
  @Output() stateLoaded = new EventEmitter<TripDeliveryWorkflowResponse>();

  currentPhase: DeliveryWorkflowPhase = 'ARRIVAL';
  loading = false;
  error = '';

  // Counting step state
  driverConfirmedCounting = false;
  customerConfirmedCounting = false;
  driverConfirmedFinished = false;
  customerConfirmedFinished = false;

  // OTP step state — single channel selection
  selectedChannel: OtpChannel = 'SMS';
  otpRecipient = '';
  otpCode = '';
  otpSent = false;
  otpWaiting = false;

  // Delivery notes collected at OTP verify step
  deliveryNotes = '';

  // Returns step
  hasReturns = false;
  returnLines: DeliveryReturnLine[] = [];
  returnForm: FormGroup;

  readonly PHASES = PHASES;
  readonly ALL_CHANNELS: { value: OtpChannel; label: string; icon: string }[] = [
    { value: 'SMS', label: 'SMS', icon: 'sms' },
    { value: 'WHATSAPP', label: 'WhatsApp', icon: 'chat' },
    { value: 'EMAIL', label: 'Email', icon: 'email' },
  ];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly workflowService: DeliveryWorkflowService,
    private readonly authState: AuthStateService,
    private readonly fb: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.returnForm = this.fb.group({
      productName: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(0.01)]],
      reason: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.currentPhase = this.initialPhase ?? 'ARRIVAL';
    if (this.tripId) {
      this.loadWorkflowState();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['tripId'] && !changes['tripId'].firstChange && this.tripId) {
      this.loadWorkflowState();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Load state from server ────────────────────────────────────────────────

  private loadWorkflowState(): void {
    this.workflowService
      .getWorkflowState(this.tripId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          // Resolve phase from combined workflowDto + tripDto
          const resolvedPhase = resolveWorkflowPhase(res);
          this.currentPhase = resolvedPhase;

          // Hydrate confirmation flags from workflowDto
          const wf = res.workflowDto;
          if (wf) {
            this.driverConfirmedCounting = !!wf.driverCountingStartedAt;
            this.customerConfirmedCounting = !!wf.customerCountingStartedAt;
            this.driverConfirmedFinished = !!wf.driverCountingFinishedAt;
            this.customerConfirmedFinished = !!wf.customerCountingFinishedAt;
            this.otpSent = tripStatusOtpPending(res.tripDto?.status);
            this.deliveryNotes = wf.deliveryNotes ?? '';
            this.returnLines = wf.returnLines ?? [];
            if (wf.otpChannel) {
              this.selectedChannel = wf.otpChannel;
            }
            if (wf.otpRecipient) {
              this.otpRecipient = wf.otpRecipient;
            }
          } else {
            // Fallback: root-level fields (backward compat)
            this.driverConfirmedCounting = res.driverConfirmedCounting ?? false;
            this.customerConfirmedCounting = res.customerConfirmedCounting ?? false;
            this.driverConfirmedFinished = res.driverConfirmedFinished ?? false;
            this.customerConfirmedFinished = res.customerConfirmedFinished ?? false;
            this.otpSent = res.otpSent ?? false;
            this.deliveryNotes = res.deliveryNotes ?? '';
            this.returnLines = res.returnLines ?? [];
          }

          this.stateLoaded.emit(res);
          this.cdr.markForCheck();
        },
        error: (e: Error) => {
          this.error = e.message;
          this.cdr.markForCheck();
        },
      });
  }

  // ── Step helpers ──────────────────────────────────────────────────────────

  get currentStepIndex(): number {
    return PHASES.indexOf(this.currentPhase);
  }

  get isComplete(): boolean {
    return this.currentPhase === 'COMPLETE';
  }

  private advanceTo(phase: DeliveryWorkflowPhase): void {
    this.currentPhase = phase;
    this.error = '';
    this.phaseChange.emit(phase);
    if (phase === 'COMPLETE') {
      this.workflowComplete.emit();
    }
    this.cdr.markForCheck();
    setTimeout(() => {
      document.querySelector('.dwf-body')?.scrollTo({ top: 0, behavior: 'smooth' });
    }, 80);
  }

  private run(
    action$: ReturnType<typeof this.workflowService.triggerArrival>,
    nextPhase: DeliveryWorkflowPhase,
  ): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    action$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.loading = false;
        this.advanceTo(nextPhase);
      },
      error: (e: Error) => {
        this.loading = false;
        this.error = e.message;
        this.cdr.markForCheck();
      },
    });
  }

  // ── Step 1: Arrival ───────────────────────────────────────────────────────

  confirmArrival(): void {
    const driverUserId = Number(this.authState.currentUser?.userId ?? 0) || undefined;
    this.run(
      this.workflowService.triggerArrival({ tripId: this.tripId, driverUserId }),
      'STOCK_COUNTING',
    );
  }

  // ── Step 2: Stock counting toggles ────────────────────────────────────────

  toggleDriverCounting(): void {
    this.driverConfirmedCounting = !this.driverConfirmedCounting;
    if (this.driverConfirmedCounting) {
      this.error = '';
      this.workflowService
        .startCounting(this.tripId, { actorRole: 'DRIVER' })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          error: (e: Error) => {
            this.driverConfirmedCounting = false;
            this.error = e.message;
            this.cdr.markForCheck();
          },
        });
    }
    this.cdr.markForCheck();
  }

  toggleCustomerCounting(): void {
    this.customerConfirmedCounting = !this.customerConfirmedCounting;
    if (this.customerConfirmedCounting) {
      this.error = '';
      this.workflowService
        .startCounting(this.tripId, { actorRole: 'CUSTOMER' })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          error: (e: Error) => {
            this.customerConfirmedCounting = false;
            this.error = e.message;
            this.cdr.markForCheck();
          },
        });
    }
    this.cdr.markForCheck();
  }

  get countingProgress(): number {
    return (
      (this.driverConfirmedCounting ? 50 : 0) +
      (this.customerConfirmedCounting ? 50 : 0)
    );
  }

  get canProceedFromCounting(): boolean {
    return this.driverConfirmedCounting && this.customerConfirmedCounting;
  }

  proceedFromCounting(): void {
    this.advanceTo('FINISHED_COUNTING');
  }

  // ── Step 3: Finished counting ─────────────────────────────────────────────

  toggleDriverFinished(): void {
    this.driverConfirmedFinished = !this.driverConfirmedFinished;
    if (this.driverConfirmedFinished) {
      this.error = '';
      this.workflowService
        .finishCounting(this.tripId, { actorRole: 'DRIVER' })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          error: (e: Error) => {
            this.driverConfirmedFinished = false;
            this.error = e.message;
            this.cdr.markForCheck();
          },
        });
    }
    this.cdr.markForCheck();
  }

  toggleCustomerFinished(): void {
    this.customerConfirmedFinished = !this.customerConfirmedFinished;
    if (this.customerConfirmedFinished) {
      this.error = '';
      this.workflowService
        .finishCounting(this.tripId, { actorRole: 'CUSTOMER' })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          error: (e: Error) => {
            this.customerConfirmedFinished = false;
            this.error = e.message;
            this.cdr.markForCheck();
          },
        });
    }
    this.cdr.markForCheck();
  }

  get canProceedFromFinished(): boolean {
    return this.driverConfirmedFinished && this.customerConfirmedFinished;
  }

  confirmFinishedCounting(): void {
    this.advanceTo('SEND_OTP');
  }

  // ── Step 4: Send OTP ──────────────────────────────────────────────────────

  selectChannel(channel: OtpChannel): void {
    this.selectedChannel = channel;
    this.cdr.markForCheck();
  }

  sendOtp(): void {
    if (!this.otpRecipient.trim()) {
      this.error = 'Please enter a recipient contact.';
      this.cdr.markForCheck();
      return;
    }
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    this.workflowService
      .sendOtp({
        tripId: this.tripId,
        channel: this.selectedChannel,
        recipientContact: this.otpRecipient.trim(),
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading = false;
          this.otpSent = true;
          this.otpWaiting = true;
          this.advanceTo('OTP_VERIFICATION');
          setTimeout(() => {
            this.otpWaiting = false;
            this.cdr.markForCheck();
          }, 2200);
        },
        error: (e: Error) => {
          this.loading = false;
          this.error = e.message;
          this.cdr.markForCheck();
        },
      });
  }

  // ── Step 5: Verify OTP (includes delivery notes) ──────────────────────────

  verifyOtp(): void {
    if (!/^\d{6}$/.test(this.otpCode.trim())) {
      this.error = 'Enter the 6-digit OTP code.';
      this.cdr.markForCheck();
      return;
    }
    this.run(
      this.workflowService.verifyOtp({
        tripId: this.tripId,
        otp: this.otpCode.trim(),
        deliveryNotes: this.deliveryNotes.trim() || undefined,
      }),
      'START_RETURN',
    );
  }

  resendOtp(): void {
    this.advanceTo('SEND_OTP');
    this.otpCode = '';
    this.otpSent = false;
    this.otpWaiting = false;
    this.cdr.markForCheck();
  }

  // ── Step 6: Start return ──────────────────────────────────────────────────

  startReturn(): void {
    this.hasReturns = true;
    this.run(
      this.workflowService.startReturn(this.tripId),
      'RETURNS',
    );
  }

  skipReturn(): void {
    this.hasReturns = false;
    this.run(
      this.workflowService.confirmReturn(this.tripId),
      'COMPLETE',
    );
  }

  // ── Step 7: Returns ───────────────────────────────────────────────────────

  addReturnLine(): void {
    if (this.returnForm.invalid) {
      this.returnForm.markAllAsTouched();
      return;
    }
    const val = this.returnForm.value as DeliveryReturnLine;
    this.returnLines = [...this.returnLines, { ...val }];
    this.returnForm.reset({ quantity: 1 });
    this.cdr.markForCheck();
  }

  removeReturnLine(index: number): void {
    this.returnLines = this.returnLines.filter((_, i) => i !== index);
    this.cdr.markForCheck();
  }

  submitReturns(): void {
    if (this.returnLines.length === 0) {
      this.error = 'Add at least one return item or skip returns.';
      this.cdr.markForCheck();
      return;
    }
    this.run(
      this.workflowService.recordReturns(this.tripId, {
        actorRole: 'DRIVER',
        returnLines: this.returnLines,
      }),
      'CONFIRM_RETURN',
    );
  }

  // ── Step 8: Confirm return complete ──────────────────────────────────────

  confirmReturnComplete(): void {
    this.run(
      this.workflowService.confirmReturn(this.tripId),
      'COMPLETE',
    );
  }
}
