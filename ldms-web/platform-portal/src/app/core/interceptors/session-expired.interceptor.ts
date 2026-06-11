import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { AuthStateService } from '../services/auth-state.service';
import { SessionExpiryService } from '../services/session-expiry.service';
import { StorageService } from '../services/storage.service';
import { isLdmsApiRequest } from '../utils/api-url.util';
import { isPublicLdmsApiRequest } from '../utils/public-api.util';

@Injectable()
export class SessionExpiredInterceptor implements HttpInterceptor {
  constructor(
    private readonly storage: StorageService,
    private readonly authState: AuthStateService,
    private readonly sessionExpiry: SessionExpiryService,
  ) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse && err.status === 401) {
          this.maybeForceLogout(req.url);
        }
        return throwError(() => err);
      }),
    );
  }

  private maybeForceLogout(url: string): void {
    if (!isLdmsApiRequest(url) || isPublicLdmsApiRequest(url)) {
      return;
    }
    const token = this.storage.getToken();
    const signedIn = !!(token && !token.startsWith('mock.')) || !!this.authState.currentUser;
    if (!signedIn) {
      return;
    }
    this.sessionExpiry.scheduleHandleSessionExpired('unauthorized');
  }
}
