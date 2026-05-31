/** Normalizes base64 payloads (strip whitespace) for data URLs. */
export function normalizeBase64(raw: string): string {
  return raw.replace(/\s+/g, '');
}

function padBase64(s: string): string {
  const pad = s.length % 4;
  if (pad === 0) {
    return s;
  }
  return s + '='.repeat(4 - pad);
}

export function inferMimeFromFileName(name: string): string | null {
  const n = name.trim().toLowerCase();
  if (n.endsWith('.png')) return 'image/png';
  if (n.endsWith('.jpg') || n.endsWith('.jpeg')) return 'image/jpeg';
  if (n.endsWith('.gif')) return 'image/gif';
  if (n.endsWith('.webp')) return 'image/webp';
  if (n.endsWith('.pdf')) return 'application/pdf';
  return null;
}

export type FilePreviewKind = 'image' | 'pdf';

export interface FilePreviewResult {
  kind: FilePreviewKind;
  dataUrl: string;
}

export function resolveFilePreview(
  r: Record<string, unknown>,
  options?: { maxBase64Chars?: number },
): FilePreviewResult | null {
  const max = options?.maxBase64Chars ?? 900_000;
  const raw = String(r['fileContent'] ?? '').trim();
  if (!raw || raw.length > max) {
    return null;
  }
  const b64 = normalizeBase64(raw);
  const contentType = String(r['contentType'] ?? '').trim().toLowerCase();
  const name = String(r['originalFileName'] ?? r['storedFileName'] ?? '').trim();
  const mime = contentType || inferMimeFromFileName(name) || 'application/octet-stream';
  if (mime.includes('pdf') || name.toLowerCase().endsWith('.pdf')) {
    return { kind: 'pdf', dataUrl: `data:application/pdf;base64,${padBase64(b64)}` };
  }
  if (mime.startsWith('image/')) {
    return { kind: 'image', dataUrl: `data:${mime};base64,${padBase64(b64)}` };
  }
  return null;
}
