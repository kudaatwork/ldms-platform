import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { isLdmsApiRequest } from '../utils/api-url.util';
import { normalizeAccessToken } from '../utils/jwt.util';
import { StorageService } from '../services/storage.service';

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
    if (token) {
      return next.handle(
        req.clone({
          setHeaders: { Authorization: `Bearer ${token}` },
        }),
      );
    }

    // Unsigned backoffice calls (services permitAll); frontend/surface calls need a session.
    if (req.url.includes('/v1/backoffice/')) {
      return next.handle(req);
    }

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
}
