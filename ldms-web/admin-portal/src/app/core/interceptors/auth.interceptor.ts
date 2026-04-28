import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StorageService } from '../services/storage.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private readonly storage: StorageService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const base = environment.apiUrl;
    if (!req.url.startsWith(base) && !req.url.startsWith('/api')) {
      return next.handle(req);
    }
    // System endpoints are intentionally unauthenticated.
    if (req.url.includes('/v1/system/')) {
      return next.handle(req);
    }
    const token = this.storage.getToken();
    if (!token) {
      return next.handle(req);
    }
    const authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
    return next.handle(authReq);
  }
}
