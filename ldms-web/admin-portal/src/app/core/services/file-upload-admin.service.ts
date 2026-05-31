import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { extractPagedResult } from '../utils/api-paged-response.util';
import {
  extractFileUploadDtoFromResponse,
  extractFileUploadDtoList,
} from '../utils/file-upload-dto-extract.util';
import { ldmsApiUrl } from '../utils/api-url.util';

export type FileUploadOwnerType = 'USER' | 'ORGANIZATION';

/** Summary row for admin tables and dialogs (no inline base64). */
export interface FileUploadSummary {
  id: number;
  originalFileName: string;
  fileType: string;
  contentType: string;
  fileSizeInBytes?: number;
  createdAt: string;
  entityStatus: string;
  ownerType?: string;
  ownerId?: number;
}

/**
 * Read/write access to ldms-file-upload-service for admin (previews, catalogs, soft delete).
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

  /** Paginated metadata catalogue (no inline file bytes). */
  findAllMetadataPage(page: number, size: number): Observable<{ rows: FileUploadSummary[]; totalElements: number }> {
    const safePage = Math.max(0, page);
    const safeSize = Math.max(1, Math.min(size, 100));
    return this.http
      .get<unknown>(`${this.base}/find-all`, {
        params: { page: String(safePage), size: String(safeSize) },
      })
      .pipe(
        map((resp) => {
          const { rows, totalElements } = extractPagedResult(resp, 'fileUploadDtoPage');
          return {
            rows: rows
              .map((row) => this.dtoToSummary(row as Record<string, unknown>))
              .filter((r): r is FileUploadSummary => r != null),
            totalElements,
          };
        }),
        catchError(() => of({ rows: [], totalElements: 0 })),
      );
  }

  /** Loads every active upload up to {@code maxRecords} for admin tables. */
  loadAllMetadata(maxRecords: number, pageSize = 100): Observable<FileUploadSummary[]> {
    const size = Math.max(1, Math.min(pageSize, 100));
    return this.findAllMetadataPage(0, size).pipe(
      switchMap(({ rows, totalElements }) => {
        const cap = Math.min(totalElements, maxRecords);
        if (rows.length >= cap) {
          return of(rows.slice(0, maxRecords));
        }
        const pagesNeeded = Math.ceil(cap / size);
        if (pagesNeeded <= 1) {
          return of(rows.slice(0, maxRecords));
        }
        const more = Array.from({ length: pagesNeeded - 1 }, (_, i) => i + 1);
        return forkJoin(more.map((page) => this.findAllMetadataPage(page, size))).pipe(
          map((rest) => [...rows, ...rest.flatMap((p) => p.rows)].slice(0, maxRecords)),
        );
      }),
    );
  }

  findByOwner(ownerType: FileUploadOwnerType, ownerId: number): Observable<FileUploadSummary[]> {
    if (!Number.isFinite(ownerId) || ownerId < 1) {
      return of([]);
    }
    return this.http
      .get<unknown>(`${this.base}/find-by-owner`, {
        params: { ownerType, ownerId: String(ownerId) },
      })
      .pipe(
        map((resp) => this.mapDtoList(resp)),
        catchError(() => of([])),
      );
  }

  deleteById(id: number): Observable<boolean> {
    if (!Number.isFinite(id) || id < 1) {
      return of(false);
    }
    return this.http.delete<unknown>(`${this.base}/delete-by-id/${id}`).pipe(
      map((resp) => {
        const obj = resp && typeof resp === 'object' ? (resp as Record<string, unknown>) : {};
        const data = obj['data'] && typeof obj['data'] === 'object' ? (obj['data'] as Record<string, unknown>) : obj;
        const code = Number(data['statusCode'] ?? obj['statusCode'] ?? 200);
        const success = data['success'] ?? obj['success'];
        return code >= 200 && code < 300 && success !== false;
      }),
      catchError(() => of(false)),
    );
  }

  mapDtoList(response: unknown): FileUploadSummary[] {
    return extractFileUploadDtoList(response)
      .map((dto) => this.dtoToSummary(dto))
      .filter((r): r is FileUploadSummary => r != null);
  }

  dtoToSummary(dto: Record<string, unknown>): FileUploadSummary | null {
    const id = Number(dto['id'] ?? 0);
    if (!Number.isFinite(id) || id < 1) {
      return null;
    }
    const original = String(dto['originalFileName'] ?? '').trim();
    const stored = String(dto['storedFileName'] ?? '').trim();
    return {
      id,
      originalFileName: original || stored || `Document #${id}`,
      fileType: this.fileTypeLabel(dto['fileType']),
      contentType: String(dto['contentType'] ?? '').trim(),
      fileSizeInBytes: dto['fileSizeInBytes'] != null ? Number(dto['fileSizeInBytes']) : undefined,
      createdAt: dto['createdAt'] != null ? String(dto['createdAt']) : '',
      entityStatus: dto['entityStatus'] != null ? String(dto['entityStatus']) : 'ACTIVE',
      ownerType: dto['ownerType'] != null ? String(dto['ownerType']) : undefined,
      ownerId: dto['ownerId'] != null ? Number(dto['ownerId']) : undefined,
    };
  }

  private fileTypeLabel(value: unknown): string {
    if (value == null) {
      return '';
    }
    if (typeof value === 'object' && value !== null && 'name' in (value as object)) {
      return String((value as { name?: unknown }).name ?? '').trim();
    }
    return String(value).trim();
  }
}
