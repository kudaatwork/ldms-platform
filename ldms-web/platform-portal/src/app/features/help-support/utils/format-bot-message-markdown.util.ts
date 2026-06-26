import { stripLegacyBotNag } from './strip-legacy-bot-nag.util';
import { stripBotToolMarkup } from './strip-bot-tool-markup.util';

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function inlineFormat(line: string): string {
  return escapeHtml(line)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)/g, '<em>$1</em>');
}

/** Renders lightweight markdown from LDMS bot replies (bold, lists, headings). */
export function formatBotMessageMarkdown(text: string): string {
  const cleaned = stripBotToolMarkup(stripLegacyBotNag(text));
  if (!cleaned) {
    return '';
  }

  const lines = cleaned.replace(/\r\n/g, '\n').split('\n');
  const out: string[] = [];
  let inUl = false;
  let inOl = false;

  const closeLists = (): void => {
    if (inUl) {
      out.push('</ul>');
      inUl = false;
    }
    if (inOl) {
      out.push('</ol>');
      inOl = false;
    }
  };

  for (const raw of lines) {
    const trimmed = raw.trim();

    if (!trimmed) {
      closeLists();
      continue;
    }

    const ulMatch = trimmed.match(/^[-*•]\s+(.+)$/);
    const olMatch = trimmed.match(/^\d+[.)]\s+(.+)$/);
    const headingMatch = trimmed.match(/^#{1,3}\s+(.+)$/);

    if (ulMatch) {
      if (inOl) {
        out.push('</ol>');
        inOl = false;
      }
      if (!inUl) {
        out.push('<ul>');
        inUl = true;
      }
      out.push(`<li>${inlineFormat(ulMatch[1])}</li>`);
      continue;
    }

    if (olMatch) {
      if (inUl) {
        out.push('</ul>');
        inUl = false;
      }
      if (!inOl) {
        out.push('<ol>');
        inOl = true;
      }
      out.push(`<li>${inlineFormat(olMatch[1])}</li>`);
      continue;
    }

    closeLists();

    if (headingMatch) {
      out.push(`<p class="bot-md-heading"><strong>${inlineFormat(headingMatch[1])}</strong></p>`);
    } else {
      out.push(`<p>${inlineFormat(trimmed)}</p>`);
    }
  }

  closeLists();
  return out.join('');
}
