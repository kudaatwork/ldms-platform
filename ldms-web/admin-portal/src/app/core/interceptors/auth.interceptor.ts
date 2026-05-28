import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { isLdmsApiRequest } from '../utils/api-url.util';
import { StorageService } from '../services/storage.service';

function normalizeAccessToken(raw: string | null | undefined): string | null {
  if (!raw) {
    return null;
  }
  const trimmed = raw.trim();
  if (!trimmed || trimmed.startsWith('mock-token-')) {
    return null;
  }
  if (trimmed.toLowerCase().startsWith('bearer ')) {
    return trimmed.slice(7).trim() || null;
  }
  return trimmed;
}

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private readonly storage: StorageService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!isLdmsApiRequest(req.url)) {
      return next.handle(req);
    }
    if (req.url.includes('/v1/system/') || req.url.includes('/v1/auth/')) {
      return next.handle(req);
    }

    const token = normalizeAccessToken(this.storage.getToken());
    if (!token) {
      return throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            statusText: 'Unauthorized',
            url: req.url,
            error: { message: 'Not signed in. Log in again to continue.' },
          }),
      );
    }

    const authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
    return next.handle(authReq);
  }
}
