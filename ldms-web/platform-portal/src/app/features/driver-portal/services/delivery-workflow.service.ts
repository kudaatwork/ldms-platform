import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import {
  FinishCountingRequest,
  RecordReturnsRequest,
  SendOtpRequest,
  StartCountingRequest,
  TriggerArrivalRequest,
  TripDeliveryWorkflowResponse,
  VerifyOtpRequest,
} from '../models/driver-portal.model';

@Injectable({ providedIn: 'root' })
export class DeliveryWorkflowService {
  private readonly tripBase = ldmsServiceUrl('trip-tracking', 'trip', undefined, 'frontend');
  private readonly deliveryBase = ldmsServiceUrl(
    'trip-tracking',
    'trip-delivery',
    undefined,
    'frontend',
  );

  constructor(private readonly http: HttpClient) {}

  // ── Workflow state ────────────────────────────────────────────────────────

  getWorkflowState(tripId: number): Observable<TripDeliveryWorkflowResponse> {
    return this.http
      .get<TripDeliveryWorkflowResponse>(`${this.deliveryBase}/${tripId}`)
      .pipe(catchError(this.handleError));
  }

  // ── Step 1: Arrival ───────────────────────────────────────────────────────

  triggerArrival(payload: TriggerArrivalRequest): Observable<void> {
    return this.http
      .post<void>(`${this.tripBase}/trigger-arrival`, payload)
      .pipe(catchError(this.handleError));
  }

  // ── Step 2: Stock counting ────────────────────────────────────────────────

  startCounting(tripId: number, payload: StartCountingRequest): Observable<void> {
    return this.http
      .post<void>(`${this.deliveryBase}/${tripId}/start-counting`, payload)
      .pipe(catchError(this.handleError));
  }

  // ── Step 3: Finished counting ─────────────────────────────────────────────

  finishCounting(tripId: number, payload: FinishCountingRequest): Observable<void> {
    return this.http
      .post<void>(`${this.deliveryBase}/${tripId}/finish-counting`, payload)
      .pipe(catchError(this.handleError));
  }

  // ── Step 4: Send OTP ──────────────────────────────────────────────────────

  sendOtp(payload: SendOtpRequest): Observable<void> {
    return this.http
      .post<void>(`${this.deliveryBase}/send-otp`, payload)
      .pipe(catchError(this.handleError));
  }

  // ── Step 5: OTP verification ──────────────────────────────────────────────

  verifyOtp(payload: VerifyOtpRequest): Observable<void> {
    return this.http
      .post<void>(`${this.deliveryBase}/verify-otp`, payload)
      .pipe(catchError(this.handleError));
  }

  // ── Step 6: Start return journey ──────────────────────────────────────────

  startReturn(tripId: number): Observable<void> {
    return this.http
      .post<void>(`${this.deliveryBase}/${tripId}/start-return`, {})
      .pipe(catchError(this.handleError));
  }

  // ── Step 7: Record returns ────────────────────────────────────────────────

  recordReturns(tripId: number, payload: RecordReturnsRequest): Observable<void> {
    return this.http
      .post<void>(`${this.deliveryBase}/${tripId}/record-returns`, payload)
      .pipe(catchError(this.handleError));
  }

  // ── Step 8: Confirm return complete ──────────────────────────────────────

  confirmReturn(tripId: number): Observable<void> {
    return this.http
      .post<void>(`${this.deliveryBase}/${tripId}/confirm-return`, {})
      .pipe(catchError(this.handleError));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private handleError = (err: any): Observable<never> =>
    throwError(
      () => new Error(err?.error?.message ?? err?.message ?? 'Delivery workflow error'),
    );
}
