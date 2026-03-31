import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { StorageService } from '../services/storage.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private readonly storage: StorageService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.storage.getToken();
    if (!token) {
      return next.handle(req);
    }
    return next.handle(
      req.clone({
        setHeaders: { Authorization: `Bearer ${token}` },
      }),
    );
  }
}
