import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { extractFileUploadDtoFromResponse } from '../utils/file-upload-dto-extract.util';
import { ldmsApiUrl } from '../utils/api-url.util';

/**
 * Read-only access to ldms-file-upload-service for admin previews (certificates, IDs, etc.).
 * Uses {@code backoffice} like organizations / users admin services.
 */
@Injectable({ providedIn: 'root' })
export class FileUploadAdminService {
  private readonly base = ldmsApiUrl('/ldms-file-upload-service/v1/backoffice/file-upload');

  constructor(private readonly http: HttpClient) {}

  getById(id: number): Observable<Record<string, unknown> | null> {
    if (!Number.isFinite(id) || id < 1) {
      return of(null);
    }
    return this.http.get<unknown>(`${this.base}/find-by-id/${id}`).pipe(
      map((resp) => extractFileUploadDtoFromResponse(resp)),
      catchError(() => of(null)),
    );
  }
}
