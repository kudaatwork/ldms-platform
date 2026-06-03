import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { isLdmsApiRequest, LDMS_CLIENT_PLATFORM_HEADER } from '../utils/api-url.util';

/** Value stored in audit_log.client_platform for platform portal traffic. */
export const PLATFORM_PORTAL_CLIENT_PLATFORM = 'PLATFORM_PORTAL';

@Injectable()
export class ClientPlatformInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!isLdmsApiRequest(req.url) || req.headers.has(LDMS_CLIENT_PLATFORM_HEADER)) {
      return next.handle(req);
    }
    return next.handle(
      req.clone({
        setHeaders: { [LDMS_CLIENT_PLATFORM_HEADER]: PLATFORM_PORTAL_CLIENT_PLATFORM },
      }),
    );
  }
}
