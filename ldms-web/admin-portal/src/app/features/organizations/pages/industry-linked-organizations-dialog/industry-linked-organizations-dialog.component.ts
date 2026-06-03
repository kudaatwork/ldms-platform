import { ScrollingModule } from '@angular/cdk/scrolling';
import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterModule } from '@angular/router';
import { Subject, debounceTime, finalize, takeUntil } from 'rxjs';
import { OrganizationsAdminService } from '../../services/organizations-admin.service';
import type { KycQueueRow } from '../../models/organization.model';

export interface IndustryLinkedOrganizationsDialogData {
  industryId: number;
  industryName: string;
}

@Component({
  selector: 'app-industry-linked-organizations-dialog',
  templateUrl: './industry-linked-organizations-dialog.component.html',
  styleUrl: './industry-linked-organizations-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ScrollingModule,
    RouterModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
  ],
})
export class IndustryLinkedOrganizationsDialogComponent implements OnInit, OnDestroy {
  private static readonly PAGE_SIZE_OPTIONS = [25, 50, 100];

  readonly pageSizeOptions = IndustryLinkedOrganizationsDialogComponent.PAGE_SIZE_OPTIONS;
  readonly virtualRowHeight = 64;

  loading = false;
  loadError = '';
  rows: KycQueueRow[] = [];
  totalElements = 0;
  pageIndex = 0;
  pageSize = 50;
  searchQuery = '';

  private readonly destroy$ = new Subject<void>();
  private readonly searchReload$ = new Subject<void>();
  private latestLoadToken = 0;

  constructor(
    private readonly orgService: OrganizationsAdminService,
    private readonly router: Router,
    private readonly dialogRef: MatDialogRef<IndustryLinkedOrganizationsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) readonly data: IndustryLinkedOrganizationsDialogData,
  ) {}

  ngOnInit(): void {
    this.searchReload$
      .pipe(debounceTime(300), takeUntil(this.destroy$))
      .subscribe(() => {
        this.pageIndex = 0;
        this.loadPage();
      });
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get hasSearch(): boolean {
    return this.searchQuery.trim().length > 0;
  }

  get rangeStart(): number {
    if (this.totalElements === 0 || this.rows.length === 0) {
      return 0;
    }
    return this.pageIndex * this.pageSize + 1;
  }

  get rangeEnd(): number {
    if (this.totalElements === 0 || this.rows.length === 0) {
      return 0;
    }
    return Math.min(this.pageIndex * this.pageSize + this.rows.length, this.totalElements);
  }

  close(): void {
    this.dialogRef.close();
  }

  refresh(): void {
    this.loadPage();
  }

  onSearchQueryChange(): void {
    this.searchReload$.next();
  }

  clearSearch(): void {
    if (!this.searchQuery) {
      return;
    }
    this.searchQuery = '';
    this.searchReload$.next();
  }

  onPage(event: PageEvent): void {
    if (event.pageIndex === this.pageIndex && event.pageSize === this.pageSize) {
      return;
    }
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadPage();
  }

  openFullList(): void {
    void this.router.navigate(['/organizations'], {
      queryParams: { industryId: this.data.industryId, industry: this.data.industryName },
    });
    this.dialogRef.close();
  }

  organizationLink(row: KycQueueRow): string[] {
    return ['/organizations', String(row.id)];
  }

  trackRow(_index: number, row: KycQueueRow): number {
    return row.id;
  }

  avatarLetter(name: string): string {
    const trimmed = String(name ?? '').trim();
    return trimmed ? trimmed.charAt(0).toUpperCase() : '?';
  }

  formatCount(value: number): string {
    return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(value);
  }

  private loadPage(): void {
    const loadToken = ++this.latestLoadToken;
    this.loading = true;
    this.loadError = '';

    this.orgService
      .queryTablePage({
        page: this.pageIndex,
        size: this.pageSize,
        searchQuery: this.searchQuery.trim(),
        columnFilters: {},
        industryId: this.data.industryId,
        kycQueueOnly: false,
        organizationDirectoryOnly: true,
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          if (loadToken === this.latestLoadToken) {
            this.loading = false;
          }
        }),
      )
      .subscribe({
        next: ({ rows, totalElements }) => {
          if (loadToken !== this.latestLoadToken) {
            return;
          }
          if (rows.length === 0 && totalElements > 0 && this.pageIndex > 0) {
            this.pageIndex = 0;
            this.loadPage();
            return;
          }
          this.rows = rows;
          this.totalElements = totalElements;
        },
        error: (err: Error) => {
          if (loadToken !== this.latestLoadToken) {
            return;
          }
          this.loadError = err.message ?? 'Failed to load linked organisations';
          this.rows = [];
          this.totalElements = 0;
        },
      });
  }
}
