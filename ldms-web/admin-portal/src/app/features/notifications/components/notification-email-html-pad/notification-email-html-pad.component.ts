import {
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  normalizeNotificationEmailMarkup,
  toNotificationEmailPreviewHtml,
} from '../../utils/notification-email-preview.util';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { html } from '@codemirror/lang-html';
import { HighlightStyle, syntaxHighlighting } from '@codemirror/language';
import { Compartment, EditorState } from '@codemirror/state';
import { EditorView } from '@codemirror/view';
import { basicSetup } from 'codemirror';
import { tags } from '@lezer/highlight';
import * as prettier from 'prettier/standalone';
import prettierHtml from 'prettier/plugins/html';
import prettierPostcss from 'prettier/plugins/postcss';

@Component({
  selector: 'app-notification-email-html-pad',
  templateUrl: './notification-email-html-pad.component.html',
  styleUrl: './notification-email-html-pad.component.scss',
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => NotificationEmailHtmlPadComponent),
      multi: true,
    },
  ],
})
export class NotificationEmailHtmlPadComponent
  implements ControlValueAccessor, AfterViewInit, OnDestroy
{
  @ViewChild('host', { static: true }) host!: ElementRef<HTMLDivElement>;
  @ViewChild('previewFrame') previewFrame!: ElementRef<HTMLIFrameElement>;

  expanded = false;
  formatting = false;
  wrapEnabled = true;
  copySuccess = false;
  previewEnabled = true;
  previewHtml = '';
  previewSrcDoc: SafeHtml = '';
  previewHeightPx = 420;
  syntaxState: 'unknown' | 'ok' | 'error' = 'unknown';
  syntaxMessage = 'Run validation';

  private view: EditorView | null = null;
  private readonly editableCompartment = new Compartment();
  private readonly wrapCompartment = new Compartment();
  private value = '';
  isDisabled = false;
  editorHeightPx = 280;
  private syntaxCheckTimer: ReturnType<typeof setTimeout> | null = null;
  private previewResizeObserver: ResizeObserver | null = null;
  private previewMutationObserver: MutationObserver | null = null;
  private previewResizeInterval: ReturnType<typeof setInterval> | null = null;
  private onChange: (v: string) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(private readonly sanitizer: DomSanitizer) {}

  ngAfterViewInit(): void {
    this.createView();
  }

  ngOnDestroy(): void {
    if (this.syntaxCheckTimer) {
      clearTimeout(this.syntaxCheckTimer);
      this.syntaxCheckTimer = null;
    }
    this.disconnectPreviewWatchers();
    this.view?.destroy();
    this.view = null;
  }

  writeValue(obj: string | null | undefined): void {
    const next = obj ?? '';
    this.value = next;
    this.previewHtml = this.toPreviewHtml(next);
    this.previewSrcDoc = this.sanitizer.bypassSecurityTrustHtml(this.previewHtml);
    const v = this.view;
    if (!v) {
      return;
    }
    const cur = v.state.doc.toString();
    if (cur === next) {
      return;
    }
    v.dispatch({
      changes: { from: 0, to: v.state.doc.length, insert: next },
    });
  }

  registerOnChange(fn: (v: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    if (!this.view) {
      return;
    }
    this.view.dispatch({
      effects: this.editableCompartment.reconfigure(EditorView.editable.of(!isDisabled)),
    });
  }

  toggleExpanded(): void {
    this.expanded = !this.expanded;
    this.editorHeightPx = this.expanded ? 620 : 280;
    setTimeout(() => this.view?.requestMeasure(), 0);
  }

  togglePreview(): void {
    this.previewEnabled = !this.previewEnabled;
    if (this.previewEnabled) {
      setTimeout(() => this.adjustPreviewHeight(), 0);
    } else {
      this.disconnectPreviewWatchers();
    }
  }

  toggleWrap(): void {
    this.wrapEnabled = !this.wrapEnabled;
    if (!this.view) {
      return;
    }
    this.view.dispatch({
      effects: this.wrapCompartment.reconfigure(
        this.wrapEnabled ? EditorView.lineWrapping : [],
      ),
    });
  }

  async copyHtml(): Promise<void> {
    const source = this.view?.state.doc.toString() ?? this.value;
    if (!source.trim()) {
      return;
    }
    await navigator.clipboard.writeText(source);
    this.copySuccess = true;
    setTimeout(() => (this.copySuccess = false), 1800);
  }

  async formatAll(): Promise<void> {
    const source = this.view?.state.doc.toString() ?? this.value;
    if (!source.trim()) {
      return;
    }
    this.formatting = true;
    try {
      const normalized = this.normalizeMarkup(source);
      const withCss = await this.formatCssInSource(normalized);
      const pretty = await prettier.format(withCss, {
        parser: 'html',
        plugins: [prettierHtml, prettierPostcss],
        printWidth: 110,
        tabWidth: 2,
        useTabs: false,
      });
      this.replaceDoc(pretty.trim());
      await this.validateSyntax();
    } catch (error) {
      console.warn('Failed to format complete HTML/CSS source', error);
    } finally {
      this.formatting = false;
    }
  }

  async formatHtml(): Promise<void> {
    const source = this.view?.state.doc.toString() ?? this.value;
    if (!source.trim()) {
      return;
    }

    this.formatting = true;
    try {
      const pretty = await prettier.format(this.normalizeMarkup(source), {
        parser: 'html',
        plugins: [prettierHtml, prettierPostcss],
        printWidth: 110,
        tabWidth: 2,
        useTabs: false,
      });
      this.replaceDoc(pretty.trim());
      await this.validateSyntax();
    } catch (error) {
      // Keep this non-fatal so users can continue editing even with malformed snippets.
      console.warn('Failed to format HTML', error);
    } finally {
      this.formatting = false;
    }
  }

  async formatCssBlocks(): Promise<void> {
    const source = this.view?.state.doc.toString() ?? this.value;
    if (!source.trim()) {
      return;
    }

    this.formatting = true;
    try {
      const next = await this.formatCssInSource(this.normalizeMarkup(source));
      this.replaceDoc(next.trim());
      await this.validateSyntax();
    } catch (error) {
      console.warn('Failed to format CSS blocks', error);
    } finally {
      this.formatting = false;
    }
  }

  async validateSyntax(): Promise<void> {
    const source = this.normalizeMarkup(this.view?.state.doc.toString() ?? this.value);
    if (!source.trim()) {
      this.syntaxState = 'unknown';
      this.syntaxMessage = 'Run validation';
      return;
    }

    try {
      await this.validateHtmlTolerant(source);
      await this.validateCssInSource(source);
      this.syntaxState = 'ok';
      this.syntaxMessage = 'HTML/CSS syntax looks valid';
    } catch (error) {
      this.syntaxState = 'error';
      this.syntaxMessage = this.extractValidationMessage(error);
    }
  }

  private async validateHtmlTolerant(source: string): Promise<void> {
    try {
      await prettier.format(source, {
        parser: 'html',
        plugins: [prettierHtml, prettierPostcss],
      });
      return;
    } catch {
      // Handlebars placeholders can confuse strict parsing in edge cases.
      const placeholderSafe = source.replace(/\{\{[\s\S]*?\}\}/g, 'TEMPLATE_VAR');
      await prettier.format(placeholderSafe, {
        parser: 'html',
        plugins: [prettierHtml, prettierPostcss],
      });
    }
  }

  private async validateCssInSource(source: string): Promise<void> {
    const re = /<style\b[^>]*>([\s\S]*?)<\/style>/gi;
    const matches = Array.from(source.matchAll(re));
    for (const m of matches) {
      const cssSource = (m[1] ?? '').trim();
      if (!cssSource) {
        continue;
      }
      await prettier.format(cssSource, {
        parser: 'css',
        plugins: [prettierPostcss],
      });
    }
  }

  private extractValidationMessage(error: unknown): string {
    const msg =
      (error as { message?: string })?.message ??
      'Invalid syntax detected in HTML or embedded CSS.';
    const first = msg.split('\n')[0]?.trim();
    return first || 'Invalid syntax detected in HTML or embedded CSS.';
  }

  private async formatCssInSource(source: string): Promise<string> {
    const re = /<style\b[^>]*>([\s\S]*?)<\/style>/gi;
    const matches = Array.from(source.matchAll(re));
    if (!matches.length) {
      return source;
    }

    let next = source;
    for (const m of matches) {
      const full = m[0];
      const cssSource = m[1] ?? '';
      const cssPretty = await prettier.format(cssSource, {
        parser: 'css',
        plugins: [prettierPostcss],
        printWidth: 100,
        tabWidth: 2,
        useTabs: false,
      });
      const replaced = full.replace(cssSource, `\n${cssPretty.trim()}\n`);
      next = next.replace(full, replaced);
    }
    return next;
  }

  private replaceDoc(next: string): void {
    this.value = next;
    this.previewHtml = this.toPreviewHtml(next);
    this.previewSrcDoc = this.sanitizer.bypassSecurityTrustHtml(this.previewHtml);
    const v = this.view;
    if (!v) {
      this.onChange(next);
      return;
    }
    v.dispatch({
      changes: { from: 0, to: v.state.doc.length, insert: next },
    });
    setTimeout(() => this.adjustPreviewHeight(), 0);
  }

  private scheduleSyntaxValidation(): void {
    if (this.syntaxCheckTimer) {
      clearTimeout(this.syntaxCheckTimer);
    }
    this.syntaxCheckTimer = setTimeout(() => {
      void this.validateSyntax();
    }, 550);
  }

  private normalizeMarkup(input: string): string {
    return normalizeNotificationEmailMarkup(input);
  }

  private toPreviewHtml(input: string): string {
    return toNotificationEmailPreviewHtml(input);
  }

  onPreviewLoaded(): void {
    this.attachPreviewWatchers();
    this.adjustPreviewHeight();
  }

  private adjustPreviewHeight(): void {
    const frame = this.previewFrame?.nativeElement;
    if (!frame) {
      return;
    }

    try {
      const doc = frame.contentDocument;
      if (!doc) {
        return;
      }
      const body = doc.body;
      const docEl = doc.documentElement;
      const contentHeight = Math.max(
        body?.scrollHeight ?? 0,
        body?.offsetHeight ?? 0,
        body?.clientHeight ?? 0,
        docEl?.scrollHeight ?? 0,
        docEl?.offsetHeight ?? 0,
        docEl?.clientHeight ?? 0,
      );
      // Keep preview practical but show complete email layout by default.
      this.previewHeightPx = Math.min(1200, Math.max(420, contentHeight + 16));
    } catch {
      // Ignore cross-context iframe reads (shouldn't happen for srcdoc).
    }
  }

  private attachPreviewWatchers(): void {
    this.disconnectPreviewWatchers();

    const frame = this.previewFrame?.nativeElement;
    const doc = frame?.contentDocument;
    if (!frame || !doc) {
      return;
    }

    const body = doc.body;
    const docEl = doc.documentElement;
    if (!body || !docEl) {
      return;
    }

    if ('ResizeObserver' in window) {
      this.previewResizeObserver = new ResizeObserver(() => this.adjustPreviewHeight());
      this.previewResizeObserver.observe(body);
      this.previewResizeObserver.observe(docEl);
    }

    this.previewMutationObserver = new MutationObserver(() => this.adjustPreviewHeight());
    this.previewMutationObserver.observe(docEl, {
      childList: true,
      subtree: true,
      attributes: true,
      characterData: true,
    });

    this.previewResizeInterval = setInterval(() => this.adjustPreviewHeight(), 700);
  }

  private disconnectPreviewWatchers(): void {
    this.previewResizeObserver?.disconnect();
    this.previewResizeObserver = null;

    this.previewMutationObserver?.disconnect();
    this.previewMutationObserver = null;

    if (this.previewResizeInterval) {
      clearInterval(this.previewResizeInterval);
      this.previewResizeInterval = null;
    }
  }

  private createView(): void {
    const updateListener = EditorView.updateListener.of((u) => {
      if (u.docChanged) {
        const s = u.state.doc.toString();
        this.value = s;
        this.previewHtml = this.toPreviewHtml(s);
        this.onChange(s);
        this.scheduleSyntaxValidation();
      }
      if (u.focusChanged && !u.view.hasFocus) {
        this.onTouched();
      }
    });

    const polishedTheme = EditorView.theme({
      '&': {
        height: '100%',
        fontSize: '12.5px',
      },
      '.cm-scroller': {
        fontFamily:
          'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace',
      },
      '.cm-content': {
        minHeight: '100%',
        padding: '12px 0 12px 12px',
        caretColor: '#60a5fa',
      },
      '.cm-gutters': {
        backgroundColor: 'var(--gray-50, #f8fafc)',
        borderRight: '1px solid var(--gray-200, #e2e8f0)',
        borderTopLeftRadius: '11px',
        borderBottomLeftRadius: '11px',
        color: '#64748b',
      },
      '.cm-activeLine, .cm-activeLineGutter': {
        backgroundColor: 'rgba(37, 99, 235, 0.08)',
      },
      '.cm-selectionBackground, ::selection': {
        backgroundColor: 'rgba(59, 130, 246, 0.24) !important',
      },
      '&.cm-focused': {
        outline: 'none',
      },
    });

    const polishedHighlight = HighlightStyle.define([
      { tag: tags.comment, color: '#64748b', fontStyle: 'italic' },
      { tag: tags.string, color: '#16a34a' },
      { tag: [tags.number, tags.bool], color: '#0ea5e9' },
      { tag: [tags.keyword, tags.atom], color: '#8b5cf6' },
      { tag: [tags.tagName, tags.className], color: '#2563eb' },
      { tag: [tags.attributeName, tags.propertyName], color: '#f97316' },
      { tag: [tags.operator, tags.punctuation], color: '#94a3b8' },
    ]);

    this.view = new EditorView({
      parent: this.host.nativeElement,
      state: EditorState.create({
        doc: this.value,
        extensions: [
          basicSetup,
          html(),
          syntaxHighlighting(polishedHighlight),
          this.editableCompartment.of(EditorView.editable.of(!this.isDisabled)),
          this.wrapCompartment.of(EditorView.lineWrapping),
          updateListener,
          polishedTheme,
        ],
      }),
    });

    this.previewHtml = this.toPreviewHtml(this.value);
    this.previewSrcDoc = this.sanitizer.bypassSecurityTrustHtml(this.previewHtml);
    setTimeout(() => this.adjustPreviewHeight(), 0);
  }
}
