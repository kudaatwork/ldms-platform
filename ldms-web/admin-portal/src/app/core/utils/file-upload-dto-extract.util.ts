/** Shared `FileUploadDto` extraction for file-upload service HTTP envelopes. */

function parseJson(value: unknown): unknown {
  if (typeof value === 'string') {
    try {
      return JSON.parse(value);
    } catch {
      return value;
    }
  }
  return value;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function unwrapDtoValue(value: unknown): Record<string, unknown> | null {
  const direct = asRecord(value);
  if (direct) {
    return direct;
  }
  const parsed = parseJson(value);
  return asRecord(parsed);
}

function extractSingleDto(response: unknown, dtoKey: string): Record<string, unknown> | null {
  const parsed = parseJson(response);
  const obj = asRecord(parsed);
  if (!obj) {
    return null;
  }
  const candidates = [obj, asRecord(obj['data']), asRecord(obj['body']), asRecord(obj['payload'])].filter(
    Boolean,
  ) as Record<string, unknown>[];
  for (const wrapped of candidates) {
    const hit = unwrapDtoValue(wrapped[dtoKey]);
    if (hit?.['id'] != null) {
      return hit;
    }
  }
  return null;
}

export function extractFileUploadDtoFromResponse(response: unknown): Record<string, unknown> | null {
  for (const key of ['fileUploadDto', 'FileUploadDto'] as const) {
    const hit = extractSingleDto(response, key);
    if (hit) {
      return hit;
    }
  }
  const obj = asRecord(parseJson(response));
  if (!obj) {
    return null;
  }
  const list = obj['fileUploadDtoList'];
  if (Array.isArray(list) && list.length) {
    const first = asRecord(list[0]);
    if (first?.['id'] != null) {
      return first;
    }
  }
  const candidates = [obj, asRecord(obj['data']), asRecord(obj['body']), asRecord(obj['payload'])].filter(
    Boolean,
  ) as Record<string, unknown>[];
  for (const c of candidates) {
    if (c['id'] == null) {
      continue;
    }
    if (
      c['originalFileName'] != null ||
      c['storedFileName'] != null ||
      c['contentType'] != null ||
      c['fileType'] != null ||
      c['fileContent'] != null
    ) {
      return c;
    }
  }
  return null;
}
