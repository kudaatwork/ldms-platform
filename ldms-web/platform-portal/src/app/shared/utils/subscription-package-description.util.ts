/** Strip leading list markers from an admin-entered feature line. */
function stripBulletPrefix(line: string): string {
  return line.replace(/^[\s•\-*–—]+/, '').trim();
}

/**
 * Turn a subscription package description into marketing bullet points.
 * Supports one feature per line (admin portal), semicolon lists, or multiple sentences.
 */
export function packageFeaturePoints(description?: string | null): string[] {
  const text = description?.trim();
  if (!text) {
    return [];
  }

  const lines = text
    .split(/\r?\n/)
    .map(stripBulletPrefix)
    .filter(Boolean);
  if (lines.length > 1) {
    return lines;
  }

  const single = lines[0] ?? text;
  if (single.includes(';')) {
    return single
      .split(';')
      .map((part) => part.trim())
      .filter(Boolean);
  }

  const sentences = single
    .split(/(?<=[.!?])\s+/)
    .map((part) => part.trim())
    .filter(Boolean);
  if (sentences.length > 1) {
    return sentences;
  }

  return [single];
}

export const DEFAULT_SUBSCRIPTION_PACKAGE_FEATURES = [
  'Monthly platform access',
  'Usage tracked for transparency',
  'Selectable in organisation billing settings',
] as const;
