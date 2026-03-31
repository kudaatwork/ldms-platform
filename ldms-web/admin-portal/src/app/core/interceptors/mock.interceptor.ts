import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * When `useMocks` is true, extend this interceptor to short-circuit API calls with fixtures.
 * Pass-through by default so the app runs against a real backend when mocks are not implemented.
 */
@Injectable()
export class MockInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!environment.useMocks) {
      return next.handle(req);
    }
    return next.handle(req);
  }
}
