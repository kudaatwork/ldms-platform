/** Remove deprecated dev-only footer from older bot replies. */
export function stripLegacyBotNag(text: string | null | undefined): string {
  if (!text) {
    return '';
  }
  return text
    .replace(/\s*\(Configure GEMINI_API_KEY for full AI answers\.\)\s*/gi, ' ')
    .replace(/\s*\(Configure [A-Z_]+_API_KEY[^)]*\)\s*/gi, ' ')
    .replace(/\s{2,}/g, ' ')
    .trim();
}
