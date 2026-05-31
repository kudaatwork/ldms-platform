/** Collects numeric ids from properties ending with `UploadId` anywhere in a JSON-like tree. */
const UPLOAD_ID_KEY = /uploadid$/i;

export interface UploadIdRef {
  uploadId: number;
  fieldKey: string;
}

export function collectUploadIdRefsFromJsonTree(root: unknown, maxDepth = 16): UploadIdRef[] {
  const byId = new Map<number, UploadIdRef>();
  const stack: { node: unknown; depth: number }[] = [{ node: root, depth: 0 }];
  const visited = new WeakSet<object>();

  while (stack.length > 0) {
    const { node, depth } = stack.pop()!;
    if (node === null || node === undefined || depth > maxDepth) {
      continue;
    }
    if (Array.isArray(node)) {
      for (const item of node) {
        stack.push({ node: item, depth: depth + 1 });
      }
      continue;
    }
    if (typeof node !== 'object') {
      continue;
    }
    if (visited.has(node)) {
      continue;
    }
    visited.add(node);
    const rec = node as Record<string, unknown>;
    for (const [key, value] of Object.entries(rec)) {
      if (UPLOAD_ID_KEY.test(key)) {
        const n = typeof value === 'string' ? Number(value.trim()) : Number(value);
        if (Number.isFinite(n) && n > 0 && n <= Number.MAX_SAFE_INTEGER) {
          const id = Math.trunc(n);
          if (!byId.has(id)) {
            byId.set(id, { uploadId: id, fieldKey: key });
          }
        }
      }
      if (value !== null && typeof value === 'object') {
        stack.push({ node: value, depth: depth + 1 });
      }
    }
  }

  return [...byId.values()];
}

export function humanizeUploadFieldKey(fieldKey: string): string {
  const base = fieldKey.replace(/UploadId$/i, '');
  if (!base) {
    return 'Linked document';
  }
  return base
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
    .trim();
}
