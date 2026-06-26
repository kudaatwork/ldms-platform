/** Removes internal tool-call traces the LLM may echo in user-visible replies. */
export function stripBotToolMarkup(text: string): string {
  if (!text) {
    return '';
  }
  let cleaned = text.trim();
  cleaned = cleaned.replace(/<tool_call>\s*\{[\s\S]*?\}\s*<\/tool_call>/gi, '');
  cleaned = cleaned.replace(/<tool_response>\s*\{[\s\S]*?\}\s*<\/tool_response>/gi, '');
  cleaned = cleaned.replace(/\n*Tool calls made:[\s\S]*?(?=\n\n---|\n\nI ran into|\n\nDone!|\n\n\*|$)/gi, '');
  cleaned = cleaned.replace(/\n*---\s*\n*I ran into a limitation[\s\S]*?(?=\n\nOption \d|$)/gi, '');
  cleaned = cleaned.replace(/^\s*---\s*$/gm, '');
  return cleaned.replace(/\n{3,}/g, '\n\n').trim();
}
