import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { toNotificationEmailPreviewHtml } from '../../utils/notification-email-preview.util';

@Component({
  selector: 'app-notification-email-html-preview',
  templateUrl: './notification-email-html-preview.component.html',
  styleUrl: './notification-email-html-preview.component.scss',
  standalone: false,
})
export class NotificationEmailHtmlPreviewComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input() html: string | null | undefined = '';
  @Input() subject: string | null | undefined = '';

  @ViewChild('previewFrame') previewFrame?: ElementRef<HTMLIFrameElement>;

  previewSrcDoc: SafeHtml = '';
  previewHeightPx = 420;

  private previewResizeObserver: ResizeObserver | null = null;
  private previewMutationObserver: MutationObserver | null = null;
  private previewResizeInterval: ReturnType<typeof setInterval> | null = null;

  constructor(private readonly sanitizer: DomSanitizer) {}

  get hasContent(): boolean {
    return Boolean(String(this.html ?? '').trim());
  }

  get subjectLine(): string {
    return String(this.subject ?? '').trim();
  }

  ngOnChanges(): void {
    this.refreshPreview();
  }

  ngAfterViewInit(): void {
    this.refreshPreview();
  }

  ngOnDestroy(): void {
    this.disconnectPreviewWatchers();
  }

  onPreviewLoaded(): void {
    this.attachPreviewWatchers();
    this.adjustPreviewHeight();
  }

  private refreshPreview(): void {
    const raw = String(this.html ?? '').trim();
    if (!raw) {
      this.previewSrcDoc = this.sanitizer.bypassSecurityTrustHtml('');
      return;
    }

    const previewHtml = toNotificationEmailPreviewHtml(raw);
    this.previewSrcDoc = this.sanitizer.bypassSecurityTrustHtml(previewHtml);
    setTimeout(() => this.adjustPreviewHeight(), 0);
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
}
