/** Normalize pasted or stored email HTML before iframe preview. */
export function normalizeNotificationEmailMarkup(input: string): string {
  const trimmed = input.trim();
  if (!trimmed) {
    return trimmed;
  }

  const unquoted =
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
      ? trimmed.slice(1, -1)
      : trimmed;

  return unquoted
    .replace(/\\"/g, '"')
    .replace(/\\n/g, '\n')
    .replace(/\\t/g, '\t')
    .replace(/\\r/g, '\r');
}

/** Build a full HTML document for iframe srcdoc preview (mirrors editor live preview). */
export function toNotificationEmailPreviewHtml(input: string): string {
  const normalized = normalizeNotificationEmailMarkup(input);
  if (!normalized) {
    return '';
  }

  if (/<html[\s>]/i.test(normalized)) {
    return normalized;
  }

  return `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${normalized}</body></html>`;
}
