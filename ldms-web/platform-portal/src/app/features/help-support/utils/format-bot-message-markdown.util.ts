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

function parseTableCells(line: string): string[] {
  let trimmed = line.trim();
  if (trimmed.startsWith('|')) {
    trimmed = trimmed.slice(1);
  }
  if (trimmed.endsWith('|')) {
    trimmed = trimmed.slice(0, -1);
  }
  return trimmed.split('|').map((cell) => cell.trim());
}

function isTableSeparator(line: string): boolean {
  if (!line.includes('|') && !/^:?-{3,}:?(\s+\|:?-{3,}:?)+$/.test(line.trim())) {
    return false;
  }
  const cells = parseTableCells(line);
  if (cells.length < 2) {
    return false;
  }
  return cells.every((cell) => /^:?-{2,}:?$/.test(cell));
}

function isTableRow(line: string): boolean {
  const trimmed = line.trim();
  if (!trimmed.includes('|')) {
    return false;
  }
  if (isTableSeparator(trimmed)) {
    return false;
  }
  return parseTableCells(trimmed).length >= 2;
}

function renderTable(headerLine: string, bodyLines: string[]): string {
  const headers = parseTableCells(headerLine);
  const rows = bodyLines.map(parseTableCells);
  const headHtml = headers.map((cell) => `<th>${inlineFormat(cell)}</th>`).join('');
  const bodyHtml = rows
    .map((cells) => `<tr>${cells.map((cell) => `<td>${inlineFormat(cell)}</td>`).join('')}</tr>`)
    .join('');
  return `<div class="bot-md-table-wrap"><table class="bot-md-table"><thead><tr>${headHtml}</tr></thead><tbody>${bodyHtml}</tbody></table></div>`;
}

function tryRenderTableBlock(lines: string[], startIndex: number): { html: string; nextIndex: number } | null {
  const first = lines[startIndex]?.trim() ?? '';
  if (!isTableRow(first)) {
    return null;
  }

  const block: string[] = [first];
  let index = startIndex + 1;

  while (index < lines.length) {
    const trimmed = lines[index].trim();
    if (!trimmed) {
      break;
    }
    if (isTableSeparator(trimmed)) {
      index += 1;
      continue;
    }
    if (!isTableRow(trimmed)) {
      break;
    }
    block.push(trimmed);
    index += 1;
  }

  if (block.length < 2) {
    return null;
  }

  return {
    html: renderTable(block[0], block.slice(1)),
    nextIndex: index,
  };
}

function isHorizontalRule(line: string): boolean {
  return /^[-*_]{3,}$/.test(line.trim());
}

/** Renders lightweight markdown from LDMS bot replies (bold, lists, headings, tables). */
export function formatBotMessageMarkdown(text: string): string {
  const cleaned = stripBotToolMarkup(stripLegacyBotNag(text)).replace(/\bLexi\b/g, 'Lexxi');
  if (!cleaned) {
    return '';
  }

  const lines = cleaned.replace(/\r\n/g, '\n').split('\n');
  const out: string[] = [];
  let inUl = false;
  let inOl = false;
  let index = 0;

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

  while (index < lines.length) {
    const raw = lines[index];
    const trimmed = raw.trim();

    if (!trimmed) {
      closeLists();
      index += 1;
      continue;
    }

    const tableBlock = tryRenderTableBlock(lines, index);
    if (tableBlock) {
      closeLists();
      out.push(tableBlock.html);
      index = tableBlock.nextIndex;
      continue;
    }

    const ulMatch = trimmed.match(/^[-*•]\s+(.+)$/);
    const olMatch = trimmed.match(/^\d+[.)]\s+(.+)$/);
    const headingMatch = trimmed.match(/^#{1,6}\s+(.+)$/);

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
      index += 1;
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
      index += 1;
      continue;
    }

    closeLists();

    if (headingMatch) {
      out.push(`<p class="bot-md-heading"><strong>${inlineFormat(headingMatch[1])}</strong></p>`);
    } else if (isHorizontalRule(trimmed)) {
      out.push('<hr class="bot-md-hr" />');
    } else {
      out.push(`<p>${inlineFormat(trimmed)}</p>`);
    }
    index += 1;
  }

  closeLists();
  return out.join('');
}
