import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Title } from '@angular/platform-browser';
import { Subject, finalize, takeUntil } from 'rxjs';
import {
  BotFaqAdminService,
  BotFaqCategory,
  BotFaqRow,
  BotKnowledgeStatus,
} from '../../services/bot-faq-admin.service';

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
  reloadingKnowledge = false;
  error = '';
  faqs: BotFaqRow[] = [];
  knowledge: BotKnowledgeStatus | null = null;
  showForm = false;
  editingId: number | null = null;
  search = '';

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
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: (rows) => {
          this.faqs = rows;
          this.loadKnowledgeStatus();
          this.cdr.markForCheck();
        },
        error: (e: Error) => {
          this.error = e.message;
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

  get filteredFaqs(): BotFaqRow[] {
    const q = this.search.trim().toLowerCase();
    if (!q) {
      return this.faqs;
    }
    return this.faqs.filter(
      (f) =>
        f.question.toLowerCase().includes(q) ||
        f.answer.toLowerCase().includes(q) ||
        (f.keywords ?? '').toLowerCase().includes(q),
    );
  }

  openCreate(): void {
    this.editingId = null;
    this.showForm = true;
    this.form.reset({
      question: '',
      answer: '',
      category: 'GENERAL',
      keywords: '',
      published: true,
    });
    this.cdr.markForCheck();
  }

  openEdit(row: BotFaqRow): void {
    this.editingId = row.id;
    this.showForm = true;
    this.form.reset({
      question: row.question,
      answer: row.answer,
      category: row.category,
      keywords: row.keywords ?? '',
      published: row.published,
    });
    this.cdr.markForCheck();
  }

  cancelForm(): void {
    this.showForm = false;
    this.editingId = null;
    this.cdr.markForCheck();
  }

  save(): void {
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
      this.editingId == null
        ? this.faqService.createFaq(payload)
        : this.faqService.updateFaq(this.editingId, payload);

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
          this.snackBar.open(this.editingId ? 'FAQ updated' : 'FAQ created', undefined, { duration: 2500 });
          this.showForm = false;
          this.editingId = null;
          this.load();
        },
        error: (e: Error) => this.snackBar.open(e.message, undefined, { duration: 4000 }),
      });
  }

  delete(row: BotFaqRow): void {
    if (!confirm(`Delete FAQ "${row.question}"?`)) {
      return;
    }
    this.faqService
      .deleteFaq(row.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.snackBar.open('FAQ deleted', undefined, { duration: 2500 });
          this.load();
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
}
