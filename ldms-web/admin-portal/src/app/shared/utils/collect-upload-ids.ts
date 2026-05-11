/**
 * Collects numeric file-upload ids from any JSON-like tree where property names follow the
 * LX/Java convention of ending with `UploadId` (e.g. nationalIdUploadId, passportUploadId,
 * logoUploadId). Use for admin UIs that must show every linked document regardless of owner linkage.
 */
const UPLOAD_ID_KEY = /uploadid$/i;

export function collectUploadIdsFromJsonTree(root: unknown, maxDepth = 16): number[] {
  const ids = new Set<number>();
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
          ids.add(Math.trunc(n));
        }
      }
      if (value !== null && typeof value === 'object') {
        stack.push({ node: value, depth: depth + 1 });
      }
    }
  }
  return [...ids];
}
