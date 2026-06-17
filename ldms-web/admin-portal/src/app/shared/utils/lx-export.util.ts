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

export function exportFormatLabel(format: LxExportFormat): string {
  if (format === 'xlsx') {
    return 'Excel';
  }
  return format.toUpperCase();
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
  return new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' });
}

function cellValue<T>(row: T, column: LxExportColumn<T>): string | number {
  const cell = column.value(row);
  if (cell == null) {
    return '';
  }
  if (typeof cell === 'boolean') {
    return cell ? 'Yes' : 'No';
  }
  return cell;
}

export async function exportRowsAsPdf<T>(
  rows: readonly T[],
  columns: readonly LxExportColumn<T>[],
  filenameBase: string,
  title?: string,
): Promise<void> {
  const [{ jsPDF }, autoTableModule] = await Promise.all([import('jspdf'), import('jspdf-autotable')]);
  const autoTable = autoTableModule.default;
  const landscape = columns.length > 5;
  const doc = new jsPDF({
    orientation: landscape ? 'landscape' : 'portrait',
    unit: 'pt',
    format: 'a4',
  });
  const reportTitle = title ?? filenameBase.replace(/-/g, ' ');
  doc.setFontSize(14);
  doc.text(reportTitle, 40, 40);
  doc.setFontSize(9);
  doc.setTextColor(100);
  doc.text(`Generated ${new Date().toLocaleString()}`, 40, 58);
  doc.setTextColor(0);

  const head = [columns.map((column) => column.header)];
  const body = rows.map((row) => columns.map((column) => String(cellValue(row, column))));

  autoTable(doc, {
    startY: 70,
    head,
    body,
    styles: { fontSize: 8, cellPadding: 4, overflow: 'linebreak' },
    headStyles: { fillColor: [30, 58, 138], textColor: 255 },
    alternateRowStyles: { fillColor: [248, 250, 252] },
    margin: { left: 40, right: 40 },
  });

  doc.save(exportFilename(filenameBase, 'pdf'));
}

export async function exportRowsAsXlsx<T>(rows: readonly T[], columns: readonly LxExportColumn<T>[]): Promise<Blob> {
  const XLSX = await import('xlsx');
  const header = columns.map((column) => column.header);
  const data = rows.map((row) => columns.map((column) => cellValue(row, column)));
  const worksheet = XLSX.utils.aoa_to_sheet([header, ...data]);
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Export');
  const buffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' });
  return new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
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

export interface LxClientTableExportOptions {
  title?: string;
}

/** Client-side CSV/PDF/Excel for tables without a backend export endpoint. Returns true when export started. */
export function exportClientTableAsCsv<T>(
  format: LxExportFormat,
  rows: readonly T[],
  columns: readonly LxExportColumn<T>[],
  filenameBase: string,
  onUnsupportedMessage: (message: string) => void,
  options?: LxClientTableExportOptions,
): boolean {
  if (format === 'csv') {
    downloadBlob(exportRowsAsCsv(rows, columns), exportFilename(filenameBase, 'csv'));
    return true;
  }
  if (format === 'pdf') {
    void exportRowsAsPdf(rows, columns, filenameBase, options?.title).catch(() => {
      onUnsupportedMessage('PDF export failed. Try CSV or Excel instead.');
    });
    return true;
  }
  if (format === 'xlsx') {
    void exportRowsAsXlsx(rows, columns)
      .then((blob) => downloadBlob(blob, exportFilename(filenameBase, 'xlsx')))
      .catch(() => {
        onUnsupportedMessage('Excel export failed. Try CSV or PDF instead.');
      });
    return true;
  }
  onUnsupportedMessage(`Export format "${format}" is not supported.`);
  return false;
}
