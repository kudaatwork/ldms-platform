import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { formatBotMessageMarkdown } from '../utils/format-bot-message-markdown.util';

@Pipe({
  name: 'botMessageMarkdown',
  standalone: false,
})
export class BotMessageMarkdownPipe implements PipeTransform {
  constructor(private readonly sanitizer: DomSanitizer) {}

  transform(value: string | null | undefined): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(formatBotMessageMarkdown(value ?? ''));
  }
}
