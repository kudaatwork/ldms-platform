import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ldmsServiceUrl } from '../../../core/utils/api-url.util';
import {
  isApiFailureEnvelope,
  readApiFailureMessage,
} from '../../../core/utils/api-paged-response.util';
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

  getWorkflowState(tripId: number): Observable<TripDeliveryWorkflowResponse> {
    return this.http.get<unknown>(`${this.deliveryBase}/${tripId}`).pipe(
      map((raw) => {
        if (isApiFailureEnvelope(raw)) {
          throw new Error(readApiFailureMessage(raw, 'Failed to load delivery workflow'));
        }
        return raw as TripDeliveryWorkflowResponse;
      }),
      catchError((err) =>
        throwError(() => this.toApiError(err, 'Failed to load delivery workflow')),
      ),
    );
  }

  triggerArrival(payload: TriggerArrivalRequest): Observable<void> {
    return this.http.post<unknown>(`${this.tripBase}/trigger-arrival`, payload).pipe(
      this.unwrapAction('Could not confirm arrival'),
    );
  }

  startCounting(tripId: number, payload: StartCountingRequest): Observable<void> {
    return this.http
      .post<unknown>(`${this.deliveryBase}/${tripId}/start-counting`, payload)
      .pipe(this.unwrapAction('Could not start stock counting'));
  }

  finishCounting(tripId: number, payload: FinishCountingRequest): Observable<void> {
    return this.http
      .post<unknown>(`${this.deliveryBase}/${tripId}/finish-counting`, payload)
      .pipe(this.unwrapAction('Could not finish stock counting'));
  }

  sendOtp(payload: SendOtpRequest): Observable<void> {
    return this.http
      .post<unknown>(`${this.deliveryBase}/send-otp`, payload)
      .pipe(this.unwrapAction('Could not send delivery OTP'));
  }

  verifyOtp(payload: VerifyOtpRequest): Observable<void> {
    return this.http
      .post<unknown>(`${this.deliveryBase}/verify-otp`, payload)
      .pipe(this.unwrapAction('Could not verify delivery OTP'));
  }

  startReturn(tripId: number): Observable<void> {
    return this.http
      .post<unknown>(`${this.deliveryBase}/${tripId}/start-return`, {})
      .pipe(this.unwrapAction('Could not start return journey'));
  }

  recordReturns(tripId: number, payload: RecordReturnsRequest): Observable<void> {
    return this.http
      .post<unknown>(`${this.deliveryBase}/${tripId}/record-returns`, payload)
      .pipe(this.unwrapAction('Could not record return items'));
  }

  confirmReturn(tripId: number): Observable<void> {
    return this.http
      .post<unknown>(`${this.deliveryBase}/${tripId}/confirm-return`, {})
      .pipe(this.unwrapAction('Could not confirm return'));
  }

  private unwrapAction(fallback: string) {
    return (source: Observable<unknown>) =>
      source.pipe(
        map((raw) => {
          if (isApiFailureEnvelope(raw)) {
            throw new Error(readApiFailureMessage(raw, fallback));
          }
          return void 0;
        }),
        catchError((err) => throwError(() => this.toApiError(err, fallback))),
      );
  }

  private toApiError(err: unknown, fallback: string): Error {
    if (err instanceof Error && !(err instanceof HttpErrorResponse)) {
      return err;
    }
    if (err instanceof HttpErrorResponse) {
      const body = err.error;
      if (typeof body === 'string' && body.trim()) {
        return new Error(body.trim());
      }
      const fromEnvelope = readApiFailureMessage(body, '');
      if (fromEnvelope) {
        return new Error(fromEnvelope);
      }
      const gatewayError =
        typeof body === 'object' &&
        body &&
        typeof (body as Record<string, unknown>)['error'] === 'string'
          ? String((body as Record<string, unknown>)['error'])
          : '';
      return new Error(gatewayError || err.message || fallback);
    }
    return new Error(fallback);
  }
}
