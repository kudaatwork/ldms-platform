import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { MOCK_USERS, PLATFORM_KPI_CONFIG } from '../../features/dashboard/data/platform-mock-data';

@Injectable()
export class MockInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!environment.useMocks || !req.url.includes('/api/v1/frontend/')) {
      return next.handle(req);
    }

    if (req.url.endsWith('/api/v1/frontend/auth/login') && req.method === 'POST') {
      const body = req.body as { email: string; password: string };
      const row = MOCK_USERS.find((u) => u.email === body.email && u.password === body.password);
      if (!row) {
        return of(new HttpResponse({ status: 401, body: { message: 'Invalid credentials' } })).pipe(delay(250));
      }
      const payload = btoa(JSON.stringify(row.user));
      const token = `mock.${payload}.signature`;
      return of(new HttpResponse({ status: 200, body: { token } })).pipe(delay(250));
    }

    if (req.url.includes('/api/v1/frontend/dashboard/kpis') && req.method === 'GET') {
      const classification = req.params.get('classification');
      const cards = classification ? PLATFORM_KPI_CONFIG[classification as keyof typeof PLATFORM_KPI_CONFIG] : [];
      return of(new HttpResponse({ status: 200, body: { cards } })).pipe(delay(200));
    }

    return next.handle(req);
  }
}
