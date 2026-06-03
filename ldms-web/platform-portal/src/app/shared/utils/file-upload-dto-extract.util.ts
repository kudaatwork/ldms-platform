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
  return asRecord(parseJson(value));
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
  return extractFileUploadDtoList(response)[0] ?? null;
}

export function extractFileUploadDtoList(response: unknown): Record<string, unknown>[] {
  const obj = asRecord(parseJson(response));
  if (!obj) {
    return [];
  }
  const candidates = [obj, asRecord(obj['data']), asRecord(obj['body']), asRecord(obj['payload'])].filter(
    Boolean,
  ) as Record<string, unknown>[];
  for (const wrapped of candidates) {
    for (const key of ['fileUploadDtoList', 'fileUploadDtos', 'fileUploads', 'documents'] as const) {
      const list = wrapped[key];
      if (Array.isArray(list)) {
        return list
          .map((item) => asRecord(item))
          .filter((item): item is Record<string, unknown> => item != null && item['id'] != null);
      }
    }
  }
  const single = extractFileUploadDtoFromResponse(response);
  return single ? [single] : [];
}

export function extractUserDtoFromResponse(response: unknown): Record<string, unknown> | null {
  for (const key of ['userDto', 'user', 'UserDto'] as const) {
    const hit = extractSingleDto(response, key);
    if (hit) {
      return hit;
    }
  }
  return null;
}

export function extractOrganizationDtoFromResponse(response: unknown): Record<string, unknown> | null {
  for (const key of ['organizationDto', 'organization', 'OrganizationDto'] as const) {
    const hit = extractSingleDto(response, key);
    if (hit) {
      return hit;
    }
  }
  return null;
}
