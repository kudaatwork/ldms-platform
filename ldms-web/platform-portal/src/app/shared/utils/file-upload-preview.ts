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

/** Detect image MIME from leading bytes when `contentType` is missing or wrong. */
export function sniffImageMimeFromBase64(b64: string): string | null {
  try {
    const clean = normalizeBase64(b64).slice(0, 64);
    if (!clean) {
      return null;
    }
    const bin = atob(padBase64(clean));
    const u8 = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) {
      u8[i] = bin.charCodeAt(i);
    }
    if (u8.length >= 3 && u8[0] === 0xff && u8[1] === 0xd8 && u8[2] === 0xff) {
      return 'image/jpeg';
    }
    if (u8.length >= 4 && u8[0] === 0x89 && u8[1] === 0x50 && u8[2] === 0x4e && u8[3] === 0x47) {
      return 'image/png';
    }
    if (u8.length >= 3 && u8[0] === 0x47 && u8[1] === 0x49 && u8[2] === 0x46) {
      return 'image/gif';
    }
    if (u8.length >= 12 && u8[0] === 0x52 && u8[1] === 0x49 && u8[2] === 0x46 && u8[3] === 0x46) {
      const sig = String.fromCharCode(u8[8], u8[9], u8[10], u8[11]);
      if (sig === 'WEBP') {
        return 'image/webp';
      }
    }
    return null;
  } catch {
    return null;
  }
}

export function inferMimeFromFileName(name: string): string | null {
  const n = name.trim().toLowerCase();
  if (n.endsWith('.png')) {
    return 'image/png';
  }
  if (n.endsWith('.jpg') || n.endsWith('.jpeg')) {
    return 'image/jpeg';
  }
  if (n.endsWith('.gif')) {
    return 'image/gif';
  }
  if (n.endsWith('.webp')) {
    return 'image/webp';
  }
  if (n.endsWith('.pdf')) {
    return 'application/pdf';
  }
  return null;
}

export type FilePreviewKind = 'image' | 'pdf';

export interface FilePreviewResult {
  kind: FilePreviewKind;
  dataUrl: string;
}

/**
 * Builds a `data:` URL for in-browser preview when `fileContent` is raw base64 from the file-upload API.
 */
export function resolveFilePreview(
  r: Record<string, unknown>,
  options?: { maxBase64Chars?: number },
): FilePreviewResult | null {
  const max = options?.maxBase64Chars ?? 2_500_000;
  const b64 = normalizeBase64(String(r['fileContent'] ?? ''));
  if (!b64 || b64.length > max) {
    return null;
  }

  let ct = String(r['contentType'] ?? '').trim().toLowerCase();
  const name = String(r['originalFileName'] ?? '').toLowerCase();
  const fileTypeEnum = String(r['fileType'] ?? '').toUpperCase();

  if (!ct || ct === 'application/octet-stream') {
    ct = inferMimeFromFileName(name) ?? ct;
  }
  if (
    (!ct || ct === 'application/octet-stream') &&
    (fileTypeEnum.includes('CERTIFICATE') ||
      fileTypeEnum.includes('CLEARANCE') ||
      fileTypeEnum.includes('LICENSE') ||
      fileTypeEnum === 'PASSPORT' ||
      fileTypeEnum === 'NATIONAL_ID')
  ) {
    ct = name.endsWith('.pdf') || !/\.(png|jpe?g|gif|webp)$/i.test(name) ? 'application/pdf' : ct;
  }
  if (ct === 'image/jpg') {
    ct = 'image/jpeg';
  }

  const nameSuggestsPdf = name.endsWith('.pdf');
  const nameSuggestsImage = /\.(png|jpe?g|gif|webp)$/i.test(name);

  if (!ct.startsWith('image/') && ct !== 'application/pdf' && !nameSuggestsPdf && !nameSuggestsImage) {
    const sniffed = sniffImageMimeFromBase64(b64);
    if (sniffed) {
      ct = sniffed;
    } else if (nameSuggestsPdf) {
      ct = 'application/pdf';
    }
  }

  if (ct.startsWith('image/')) {
    return { kind: 'image', dataUrl: `data:${ct};base64,${b64}` };
  }
  if (ct === 'application/pdf' || nameSuggestsPdf) {
    return { kind: 'pdf', dataUrl: `data:application/pdf;base64,${b64}` };
  }

  const sniffed = sniffImageMimeFromBase64(b64);
  if (sniffed) {
    return { kind: 'image', dataUrl: `data:${sniffed};base64,${b64}` };
  }
  return null;
}
