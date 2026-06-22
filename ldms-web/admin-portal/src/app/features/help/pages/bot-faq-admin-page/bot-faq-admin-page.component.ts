import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Title } from '@angular/platform-browser';
import { Subject, finalize, takeUntil } from 'rxjs';
import {
  BotFaqAdminService,
  BotFaqCategory,
  BotFaqRow,
  BotKnowledgeDocumentRow,
  BotKnowledgeStatus,
} from '../../services/bot-faq-admin.service';

type AddKnowledgeMode = 'qa' | 'text' | 'pdf';
type KnowledgeListFilter = 'all' | 'qa' | 'docs';

export interface KnowledgeSourceRow {
  id: string;
  kind: 'qa' | 'pdf' | 'text';
  kindLabel: string;
  title: string;
  preview: string;
  meta: string;
  useCount: number;
  published: boolean;
  faqId?: number;
  documentId?: number;
}

@Component({
  selector: 'app-bot-faq-admin-page',
  templateUrl: './bot-faq-admin-page.component.html',
  styleUrl: './bot-faq-admin-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class BotFaqAdminPageComponent implements OnInit, OnDestroy {
  loading = true;
  saving = false;
  savingText = false;
  reloadingKnowledge = false;
  uploadingDocument = false;
  error = '';
  faqs: BotFaqRow[] = [];
  documents: BotKnowledgeDocumentRow[] = [];
  knowledge: BotKnowledgeStatus | null = null;
  addMode: AddKnowledgeMode = 'qa';
  listFilter: KnowledgeListFilter = 'all';
  search = '';
  documentTitle = '';
  selectedDocumentFile: File | null = null;
  textArticleTitle = '';
  textArticleBody = '';
  editingFaqId: number | null = null;

  readonly addModes: Array<{ id: AddKnowledgeMode; label: string; icon: string; hint: string }> = [
    { id: 'qa', label: 'Text Q&A', icon: 'quiz', hint: 'Question and answer pairs the bot retrieves by keyword.' },
    { id: 'text', label: 'Paste text', icon: 'article', hint: 'Free-form guides, SOPs, or notes pasted directly.' },
    { id: 'pdf', label: 'Upload PDF', icon: 'picture_as_pdf', hint: 'PDF files — text is extracted and indexed automatically.' },
  ];

  readonly listFilters: Array<{ id: KnowledgeListFilter; label: string }> = [
    { id: 'all', label: 'All sources' },
    { id: 'qa', label: 'Text Q&A' },
    { id: 'docs', label: 'Documents' },
  ];

  readonly categories: BotFaqCategory[] = [
    'GENERAL',
    'ONBOARDING',
    'OPERATIONS',
    'BILLING',
    'FLEET',
    'TRIPS',
    'SUPPORT',
  ];

  form: FormGroup;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly faqService: BotFaqAdminService,
    private readonly fb: FormBuilder,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly title: Title,
  ) {
    this.form = this.fb.group({
      question: ['', [Validators.required, Validators.maxLength(500)]],
      answer: ['', Validators.required],
      category: ['GENERAL', Validators.required],
      keywords: [''],
      published: [true],
    });
  }

  ngOnInit(): void {
    this.title.setTitle('Bot knowledge | LX Admin');
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.faqService
      .listFaqs()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rows) => {
          this.faqs = rows;
          this.loadKnowledgeStatus();
          this.loadDocuments();
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (e: Error) => {
          this.error = e.message;
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
  }

  private loadDocuments(): void {
    this.faqService
      .listDocuments()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rows) => {
          this.documents = rows;
          this.cdr.markForCheck();
        },
      });
  }

  private loadKnowledgeStatus(): void {
    this.faqService
      .knowledgeStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (k) => {
          this.knowledge = k;
          this.cdr.markForCheck();
        },
      });
  }

  setAddMode(mode: AddKnowledgeMode): void {
    this.addMode = mode;
    if (mode !== 'qa') {
      this.editingFaqId = null;
    }
    this.cdr.markForCheck();
  }

  get activeAddModeHint(): string {
    return this.addModes.find((m) => m.id === this.addMode)?.hint ?? '';
  }

  setListFilter(filter: KnowledgeListFilter): void {
    this.listFilter = filter;
    this.cdr.markForCheck();
  }

  get knowledgeSources(): KnowledgeSourceRow[] {
    const q = this.search.trim().toLowerCase();
    const faqRows: KnowledgeSourceRow[] = this.faqs.map((f) => ({
      id: `faq-${f.id}`,
      kind: 'qa',
      kindLabel: 'Text Q&A',
      title: f.question,
      preview: f.answer,
      meta: f.category,
      useCount: f.useCount,
      published: f.published,
      faqId: f.id,
    }));
    const docRows: KnowledgeSourceRow[] = this.documents.map((d) => ({
      id: `doc-${d.id}`,
      kind: this.isPdfDocument(d) ? 'pdf' : 'text',
      kindLabel: this.isPdfDocument(d) ? 'PDF' : 'Pasted text',
      title: d.title,
      preview: `${d.extractedTextLength.toLocaleString()} characters indexed`,
      meta: this.isPdfDocument(d) ? d.originalFilename : 'Direct text entry',
      useCount: d.useCount,
      published: d.published,
      documentId: d.id,
    }));

    let rows = [...faqRows, ...docRows].sort((a, b) => a.title.localeCompare(b.title));
    if (this.listFilter === 'qa') {
      rows = rows.filter((r) => r.kind === 'qa');
    } else if (this.listFilter === 'docs') {
      rows = rows.filter((r) => r.kind !== 'qa');
    }
    if (q) {
      rows = rows.filter(
        (r) =>
          r.title.toLowerCase().includes(q) ||
          r.preview.toLowerCase().includes(q) ||
          r.meta.toLowerCase().includes(q),
      );
    }
    return rows;
  }

  saveQa(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const val = this.form.value as {
      question: string;
      answer: string;
      category: string;
      keywords: string;
      published: boolean;
    };
    const payload = {
      question: val.question.trim(),
      answer: val.answer.trim(),
      category: val.category,
      keywords: val.keywords?.trim() || undefined,
      published: val.published,
    };
    const req$ =
      this.editingFaqId == null
        ? this.faqService.createFaq(payload)
        : this.faqService.updateFaq(this.editingFaqId, payload);

    req$
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.saving = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.snackBar.open(
            this.editingFaqId ? 'Q&A updated' : 'Q&A saved — the assistant will use it on new messages.',
            undefined,
            { duration: 3000 },
          );
          this.editingFaqId = null;
          this.form.reset({
            question: '',
            answer: '',
            category: 'GENERAL',
            keywords: '',
            published: true,
          });
          this.load();
        },
        error: (e: Error) => this.snackBar.open(e.message, undefined, { duration: 4000 }),
      });
  }

  saveTextArticle(): void {
    const title = this.textArticleTitle.trim();
    const body = this.textArticleBody.trim();
    if (!title || body.length < 20) {
      this.snackBar.open('Enter a title and at least 20 characters of text.', undefined, { duration: 3000 });
      return;
    }
    this.savingText = true;
    this.faqService
      .createTextDocument(title, body)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.savingText = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.textArticleTitle = '';
          this.textArticleBody = '';
          this.snackBar.open('Text knowledge indexed — the assistant will use it on new messages.', undefined, {
            duration: 3500,
          });
          this.loadDocuments();
          this.loadKnowledgeStatus();
        },
        error: (e: Error) => this.snackBar.open(e.message, undefined, { duration: 4000 }),
      });
  }

  reloadKnowledge(): void {
    this.reloadingKnowledge = true;
    this.faqService
      .reloadKnowledge()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.reloadingKnowledge = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (k) => {
          this.knowledge = k;
          this.snackBar.open('Knowledge corpus reloaded', undefined, { duration: 2500 });
          this.cdr.markForCheck();
        },
        error: (e: Error) => this.snackBar.open(e.message, undefined, { duration: 4000 }),
      });
  }

  onDocumentFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedDocumentFile = input.files?.[0] ?? null;
    if (this.selectedDocumentFile && !this.documentTitle.trim()) {
      this.documentTitle = this.selectedDocumentFile.name.replace(/\.pdf$/i, '');
    }
    this.cdr.markForCheck();
  }

  uploadDocument(): void {
    const title = this.documentTitle.trim();
    const file = this.selectedDocumentFile;
    if (!title || !file) {
      this.snackBar.open('Enter a title and choose a PDF file.', undefined, { duration: 3000 });
      return;
    }
    this.uploadingDocument = true;
    this.faqService
      .uploadDocument(title, file)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.uploadingDocument = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => {
          this.documentTitle = '';
          this.selectedDocumentFile = null;
          this.snackBar.open('PDF indexed — the assistant will use it on new messages.', undefined, {
            duration: 3500,
          });
          this.loadDocuments();
          this.loadKnowledgeStatus();
        },
        error: (e: Error) => this.snackBar.open(e.message, undefined, { duration: 4000 }),
      });
  }

  editSource(row: KnowledgeSourceRow): void {
    if (row.kind !== 'qa' || row.faqId == null) {
      return;
    }
    const faq = this.faqs.find((f) => f.id === row.faqId);
    if (!faq) {
      return;
    }
    this.addMode = 'qa';
    this.editingFaqId = faq.id;
    this.form.reset({
      question: faq.question,
      answer: faq.answer,
      category: faq.category,
      keywords: faq.keywords ?? '',
      published: faq.published,
    });
    this.cdr.markForCheck();
  }

  deleteSource(row: KnowledgeSourceRow): void {
    if (row.kind === 'qa' && row.faqId != null) {
      if (!confirm(`Delete Q&A "${row.title}"?`)) {
        return;
      }
      this.faqService
        .deleteFaq(row.faqId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.snackBar.open('Q&A deleted', undefined, { duration: 2500 });
            this.load();
          },
          error: (e: Error) => this.snackBar.open(e.message, undefined, { duration: 4000 }),
        });
      return;
    }
    if (row.documentId != null) {
      if (!confirm(`Remove "${row.title}" from bot knowledge?`)) {
        return;
      }
      this.faqService
        .deleteDocument(row.documentId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.snackBar.open('Document removed', undefined, { duration: 2500 });
            this.loadDocuments();
            this.loadKnowledgeStatus();
          },
          error: (e: Error) => this.snackBar.open(e.message, undefined, { duration: 4000 }),
        });
    }
  }

  isPdfDocument(doc: BotKnowledgeDocumentRow): boolean {
    const ct = (doc.contentType ?? '').toLowerCase();
    const name = (doc.originalFilename ?? '').toLowerCase();
    return ct.includes('pdf') || name.endsWith('.pdf');
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  kindIcon(kind: KnowledgeSourceRow['kind']): string {
    switch (kind) {
      case 'pdf':
        return 'picture_as_pdf';
      case 'text':
        return 'article';
      default:
        return 'quiz';
    }
  }
}
