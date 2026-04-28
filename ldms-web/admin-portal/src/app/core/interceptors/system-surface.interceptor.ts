import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable()
export class SystemSurfaceInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const base = environment.apiUrl;
    const isApiCall = req.url.startsWith(base) || req.url.startsWith('/api');
    const isSystemEndpoint = req.url.includes('/v1/system/');

    if (!isApiCall || !isSystemEndpoint) {
      return next.handle(req);
    }

    // Guarantee that system surface requests are unauthenticated and non-credentialed.
    let headers = req.headers;
    if (headers.has('Authorization')) {
      headers = headers.delete('Authorization');
    }
    const stripped = req.clone({
      headers,
      withCredentials: false,
    });
    return next.handle(stripped);
  }
}
