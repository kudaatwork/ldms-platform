import { HttpErrorResponse } from '@angular/common/http';
import { Observable, from, mergeMap, throwError } from 'rxjs';

/** Server/API export formats used across LDMS admin tables. */
export type LxExportFormat = 'csv' | 'xlsx' | 'pdf';

export function exportFormatToApiParam(format: LxExportFormat): string {
  return format === 'xlsx' ? 'xlsx' : format;
}

export function exportFormatExtension(format: LxExportFormat): string {
  return format === 'xlsx' ? 'xlsx' : format;
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.rel = 'noopener';
  anchor.style.display = 'none';
  document.body.appendChild(anchor);
  anchor.click();
  requestAnimationFrame(() => {
    anchor.remove();
    URL.revokeObjectURL(url);
  });
}

/** Reject empty bodies and JSON/text error payloads masquerading as export files. */
export async function validateExportBlob(blob: Blob, format: LxExportFormat): Promise<Blob> {
  if (!blob || blob.size === 0) {
    throw new Error('Export returned an empty file. Try narrowing your date filters.');
  }
  const type = (blob.type || '').toLowerCase();
  if (type.includes('json') || type === 'text/plain') {
    const text = (await blob.text()).trim();
    throw new Error(text || 'Export failed.');
  }
  if (format === 'pdf') {
    const head = await blob.slice(0, 5).text();
    if (!head.startsWith('%PDF')) {
      const text = (await blob.text()).trim();
      throw new Error(text || 'Export did not return a valid PDF.');
    }
  }
  return blob;
}

export interface LxExportColumn<T> {
  header: string;
  value: (row: T) => string | number | boolean | null | undefined;
}

/** Client-side CSV for screens without a backend export endpoint (mock data, embedded lists). */
export function exportRowsAsCsv<T>(rows: readonly T[], columns: readonly LxExportColumn<T>[]): Blob {
  const escape = (raw: string): string => {
    const value = raw.replace(/"/g, '""');
    return /[",\n\r]/.test(value) ? `"${value}"` : value;
  };
  const headerLine = columns.map((c) => escape(c.header)).join(',');
  const lines = rows.map((row) =>
    columns
      .map((c) => {
        const cell = c.value(row);
        return escape(cell == null ? '' : String(cell));
      })
      .join(','),
  );
  const csv = [headerLine, ...lines].join('\n');
  return new Blob([csv], { type: 'text/csv;charset=utf-8' });
}

export function exportFilename(base: string, format: LxExportFormat): string {
  const stamp = new Date().toISOString().slice(0, 10);
  return `${base}-${stamp}.${exportFormatExtension(format)}`;
}

/** Maps blob error bodies from export POST failures into a thrown Error. */
export function mapExportHttpError(err: HttpErrorResponse): Observable<never> {
  const blob: Blob | null = err.error instanceof Blob ? err.error : null;
  if (blob) {
    return from(blob.text()).pipe(
      mergeMap((text) =>
        throwError(() => new Error(text || `Export failed with status ${err.status}.`)),
      ),
    );
  }
  const message =
    typeof err.error === 'object' && err.error && 'message' in err.error
      ? String((err.error as { message?: string }).message)
      : err.message;
  return throwError(() => new Error(message || `Export failed with status ${err.status}.`));
}

/** CSV download for mock/local tables; PDF/XLSX show a friendly message. Returns true when a file was saved. */
export function exportClientTableAsCsv<T>(
  format: LxExportFormat,
  rows: readonly T[],
  columns: readonly LxExportColumn<T>[],
  filenameBase: string,
  onCsvOnlyMessage: (message: string) => void,
): boolean {
  if (format !== 'csv') {
    onCsvOnlyMessage('This screen supports CSV export only until a server export API is available.');
    return false;
  }
  const blob = exportRowsAsCsv(rows, columns);
  downloadBlob(blob, exportFilename(filenameBase, 'csv'));
  return true;
}
