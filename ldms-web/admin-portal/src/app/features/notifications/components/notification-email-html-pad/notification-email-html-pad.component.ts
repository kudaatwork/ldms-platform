import {
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import type { Compartment } from '@codemirror/state';
import type { EditorView } from '@codemirror/view';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import {
  normalizeNotificationEmailMarkup,
  toNotificationEmailPreviewHtml,
} from '../../utils/notification-email-preview.util';
import {
  loadNotificationEmailEditorDeps,
  type NotificationEmailEditorDeps,
} from './notification-email-editor-deps.loader';

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
  editorLoading = true;
  editorLoadError: string | null = null;

  private deps: NotificationEmailEditorDeps | null = null;
  private view: EditorView | null = null;
  private editableCompartment: Compartment | null = null;
  private wrapCompartment: Compartment | null = null;
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
    void this.initEditor();
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
    if (!this.view || !this.deps || !this.editableCompartment) {
      return;
    }
    this.view.dispatch({
      effects: this.editableCompartment.reconfigure(
        this.deps.EditorView.editable.of(!isDisabled),
      ),
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
    if (!this.view || !this.deps || !this.wrapCompartment) {
      return;
    }
    this.view.dispatch({
      effects: this.wrapCompartment.reconfigure(
        this.wrapEnabled ? this.deps.EditorView.lineWrapping : [],
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
    if (!source.trim() || !this.deps) {
      return;
    }
    this.formatting = true;
    try {
      const normalized = this.normalizeMarkup(source);
      const withCss = await this.formatCssInSource(normalized);
      const pretty = await this.deps.prettierFormat(withCss, {
        parser: 'html',
        plugins: [this.deps.prettierHtmlPlugin, this.deps.prettierPostcssPlugin],
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
    if (!source.trim() || !this.deps) {
      return;
    }

    this.formatting = true;
    try {
      const pretty = await this.deps.prettierFormat(this.normalizeMarkup(source), {
        parser: 'html',
        plugins: [this.deps.prettierHtmlPlugin, this.deps.prettierPostcssPlugin],
        printWidth: 110,
        tabWidth: 2,
        useTabs: false,
      });
      this.replaceDoc(pretty.trim());
      await this.validateSyntax();
    } catch (error) {
      console.warn('Failed to format HTML', error);
    } finally {
      this.formatting = false;
    }
  }

  async formatCssBlocks(): Promise<void> {
    const source = this.view?.state.doc.toString() ?? this.value;
    if (!source.trim() || !this.deps) {
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
    if (!this.deps) {
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

  onPreviewLoaded(): void {
    this.attachPreviewWatchers();
    this.adjustPreviewHeight();
  }

  private async initEditor(): Promise<void> {
    this.editorLoading = true;
    this.editorLoadError = null;
    try {
      this.deps = await loadNotificationEmailEditorDeps();
      this.editableCompartment = new this.deps.Compartment();
      this.wrapCompartment = new this.deps.Compartment();
      this.createView();
      this.editorLoading = false;
    } catch (error) {
      this.editorLoading = false;
      this.editorLoadError = 'Could not load the HTML editor. Refresh the page and try again.';
      console.error('Notification email editor failed to load', error);
    }
  }

  private async validateHtmlTolerant(source: string): Promise<void> {
    if (!this.deps) {
      return;
    }
    try {
      await this.deps.prettierFormat(source, {
        parser: 'html',
        plugins: [this.deps.prettierHtmlPlugin, this.deps.prettierPostcssPlugin],
      });
      return;
    } catch {
      const placeholderSafe = source.replace(/\{\{[\s\S]*?\}\}/g, 'TEMPLATE_VAR');
      await this.deps.prettierFormat(placeholderSafe, {
        parser: 'html',
        plugins: [this.deps.prettierHtmlPlugin, this.deps.prettierPostcssPlugin],
      });
    }
  }

  private async validateCssInSource(source: string): Promise<void> {
    if (!this.deps) {
      return;
    }
    const re = /<style\b[^>]*>([\s\S]*?)<\/style>/gi;
    const matches = Array.from(source.matchAll(re));
    for (const m of matches) {
      const cssSource = (m[1] ?? '').trim();
      if (!cssSource) {
        continue;
      }
      await this.deps.prettierFormat(cssSource, {
        parser: 'css',
        plugins: [this.deps.prettierPostcssPlugin],
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
    if (!this.deps) {
      return source;
    }
    const re = /<style\b[^>]*>([\s\S]*?)<\/style>/gi;
    const matches = Array.from(source.matchAll(re));
    if (!matches.length) {
      return source;
    }

    let next = source;
    for (const m of matches) {
      const full = m[0];
      const cssSource = m[1] ?? '';
      const cssPretty = await this.deps.prettierFormat(cssSource, {
        parser: 'css',
        plugins: [this.deps.prettierPostcssPlugin],
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
    const deps = this.deps;
    const editableCompartment = this.editableCompartment;
    const wrapCompartment = this.wrapCompartment;
    if (!deps || !editableCompartment || !wrapCompartment) {
      return;
    }

    const updateListener = deps.EditorView.updateListener.of((u) => {
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

    const polishedTheme = deps.EditorView.theme({
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

    const polishedHighlight = deps.HighlightStyle.define([
      { tag: deps.tags.comment, color: '#64748b', fontStyle: 'italic' },
      { tag: deps.tags.string, color: '#16a34a' },
      { tag: [deps.tags.number, deps.tags.bool], color: '#0ea5e9' },
      { tag: [deps.tags.keyword, deps.tags.atom], color: '#8b5cf6' },
      { tag: [deps.tags.tagName, deps.tags.className], color: '#2563eb' },
      { tag: [deps.tags.attributeName, deps.tags.propertyName], color: '#f97316' },
      { tag: [deps.tags.operator, deps.tags.punctuation], color: '#94a3b8' },
    ]);

    this.view = new deps.EditorView({
      parent: this.host.nativeElement,
      state: deps.EditorState.create({
        doc: this.value,
        extensions: [
          deps.basicSetup,
          deps.html(),
          deps.syntaxHighlighting(polishedHighlight),
          editableCompartment.of(deps.EditorView.editable.of(!this.isDisabled)),
          wrapCompartment.of(deps.EditorView.lineWrapping),
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
