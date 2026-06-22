/** True when the API envelope reports failure (`success` / `isSuccess` / in-body `statusCode` ≥ 400). */
export function isApiFailureEnvelope(response: unknown): boolean {
  const root = toObj(parsePossiblyStringifiedJson(response));
  if (!root) {
    return false;
  }
  const candidates = [root, toObj(root['data'])].filter(Boolean) as Record<string, unknown>[];
  for (const body of candidates) {
    if (body['success'] === false || body['isSuccess'] === false) {
      return true;
    }
    const statusCode = Number(body['statusCode']);
    if (Number.isFinite(statusCode) && statusCode >= 400) {
      return true;
    }
  }
  return false;
}

/** In-body status when HTTP status is 200 (LDMS convention). */
export function readInBodyStatusCode(response: unknown): number | null {
  const root = toObj(parsePossiblyStringifiedJson(response));
  if (!root) {
    return null;
  }
  const candidates = [root, toObj(root['data'])].filter(Boolean) as Record<string, unknown>[];
  for (const body of candidates) {
    const statusCode = Number(body['statusCode']);
    if (Number.isFinite(statusCode)) {
      return statusCode;
    }
  }
  return null;
}

/** Extract a flat DTO list from LDMS envelopes (`walletDepositDtoList`, nested `data`, etc.). */
export function extractDtoList<T>(response: unknown, listKeyHint = 'dtoList'): T[] {
  const { rows } = extractPagedResult(response, listKeyHint);
  return rows as T[];
}

export function readApiFailureMessage(response: unknown, fallback: string): string {
  const root = toObj(parsePossiblyStringifiedJson(response));
  if (!root) {
    return fallback;
  }
  const candidates = [root, toObj(root['data'])].filter(Boolean) as Record<string, unknown>[];
  for (const body of candidates) {
    const messages = body['errorMessages'];
    if (Array.isArray(messages) && messages.length > 0) {
      return messages.map((m) => String(m)).join(' ');
    }
    if (typeof body['message'] === 'string' && body['message'].trim()) {
      return body['message'].trim();
    }
  }
  return fallback;
}

/**
 * Parses Spring Data `Page` JSON from LDMS API envelopes (aligned with organizations table).
 * Handles optional `data` / `body` / `payload` wrappers and stringified JSON bodies.
 */
export function extractPagedResult(
  response: unknown,
  dtoPageKey: string,
): { rows: unknown[]; totalElements: number } {
  const empty = { rows: [] as unknown[], totalElements: 0 };
  const parsed = parsePossiblyStringifiedJson(response);
  const obj = toObj(parsed);
  if (!obj) {
    return empty;
  }
  const envelopeSources = [
    obj,
    toObj(obj['data']),
    toObj(obj['body']),
    toObj(obj['payload']),
  ].filter(Boolean) as Record<string, unknown>[];

  const snakeKey = snakeDtoPageKey(dtoPageKey);
  const explicitPageKeys = [dtoPageKey, dtoPageKey.replace(/DtoPage$/, 'Page'), 'page', ...(snakeKey ? [snakeKey] : [])];
  const dedupedKeys = [...new Set(explicitPageKeys.filter((k) => !!k))];

  for (const src of envelopeSources) {
    for (const key of dedupedKeys) {
      const direct = readSpringPage(src[key]);
      if (direct) {
        return direct;
      }
    }
    const discovered = findPageObject(src);
    if (discovered) {
      const fromNested = readSpringPage(discovered);
      if (fromNested) {
        return fromNested;
      }
    }
    const listKey = dtoPageKey.replace(/Page$/, 'List');
    const list = src[listKey];
    if (Array.isArray(list)) {
      return { rows: list, totalElements: list.length };
    }
    for (const [key, val] of Object.entries(src)) {
      if (key.toLowerCase().endsWith('dtopage')) {
        const p = readSpringPage(val);
        if (p) {
          return p;
        }
      }
      if (key.toLowerCase().endsWith('dtolist') && Array.isArray(val)) {
        return { rows: val, totalElements: val.length };
      }
    }
  }
  return empty;
}

function parsePossiblyStringifiedJson(value: unknown): unknown {
  if (typeof value !== 'string') {
    return value;
  }
  const trimmed = value.trim();
  if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
    return value;
  }
  try {
    return JSON.parse(trimmed) as unknown;
  } catch {
    return value;
  }
}

function toObj(value: unknown): Record<string, unknown> | null {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function findPageObject(src: Record<string, unknown>): Record<string, unknown> | null {
  if (Array.isArray(src['content'])) {
    return src;
  }
  for (const value of Object.values(src)) {
    const nested = toObj(value);
    if (!nested) {
      continue;
    }
    const found = findPageObject(nested);
    if (found) {
      return found;
    }
  }
  return null;
}

function snakeDtoPageKey(dtoPageKey: string): string | null {
  const m = /^(.+?)DtoPage$/.exec(dtoPageKey);
  if (!m) {
    return null;
  }
  const base = m[1].replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase();
  return `${base}_dto_page`;
}

function readSpringPage(val: unknown): { rows: unknown[]; totalElements: number } | null {
  const page = toObj(val);
  if (!page) {
    return null;
  }
  const content = page['content'];
  const rows = Array.isArray(content) ? content : [];
  let totalElements = coerceNonNegativeInt(page['totalElements']);
  if (totalElements == null) {
    const totalPages = coerceNonNegativeInt(page['totalPages']);
    const size = coerceNonNegativeInt(page['size']);
    if (totalPages != null && size != null && totalPages > 0) {
      totalElements = (totalPages - 1) * size + rows.length;
    } else {
      totalElements = rows.length;
    }
  }
  return { rows, totalElements };
}

function coerceNonNegativeInt(raw: unknown): number | null {
  if (raw == null || raw === '') {
    return null;
  }
  const n = typeof raw === 'number' ? raw : Number(raw);
  return Number.isFinite(n) && n >= 0 ? Math.trunc(n) : null;
}
