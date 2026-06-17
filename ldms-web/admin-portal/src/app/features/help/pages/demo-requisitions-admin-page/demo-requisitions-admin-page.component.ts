import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, catchError, finalize, of, startWith, switchMap } from 'rxjs';
import {
  AdminDemoRequisition,
  DemoRequisitionStatus,
  HelpSupportAdminService,
} from '../../services/help-support-admin.service';

type StatusFilter = 'ALL' | DemoRequisitionStatus;

@Component({
  selector: 'app-demo-requisitions-admin-page',
  templateUrl: './demo-requisitions-admin-page.component.html',
  styleUrl: './demo-requisitions-admin-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DemoRequisitionsAdminPageComponent implements OnInit {
  loading = true;
  detailLoading = false;
  actionBusy = false;
  error = '';
  detailError = '';
  requisitions: AdminDemoRequisition[] = [];
  selected: AdminDemoRequisition | null = null;
  adminNotesDraft = '';
  scheduledAtDraft = '';

  readonly search = signal('');
  readonly statusFilter = signal<StatusFilter>('ALL');
  readonly mobileShowDetail = signal(false);

  readonly statusFilters: { id: StatusFilter; label: string }[] = [
    { id: 'ALL', label: 'All' },
    { id: 'NEW', label: 'New' },
    { id: 'CONTACTED', label: 'Contacted' },
    { id: 'SCHEDULED', label: 'Scheduled' },
    { id: 'COMPLETED', label: 'Completed' },
    { id: 'CANCELLED', label: 'Cancelled' },
  ];

  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new Subject<void>();

  constructor(
    private readonly title: Title,
    private readonly helpApi: HelpSupportAdminService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Demo requisitions | LX Admin');
    this.reload$
      .pipe(
        startWith(undefined),
        switchMap(() => {
          this.loading = true;
          this.error = '';
          this.cdr.markForCheck();
          return this.helpApi.fetchAllDemoRequisitions().pipe(
            catchError((err: Error) => {
              this.error = err.message;
              return of([] as AdminDemoRequisition[]);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((rows) => this.applyRequisitions(rows));
  }

  get filteredRequisitions(): AdminDemoRequisition[] {
    const q = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    return this.requisitions.filter((row) => {
      if (status !== 'ALL' && row.status !== status) {
        return false;
      }
      if (!q) {
        return true;
      }
      const haystack = [
        row.requisitionNumber,
        row.fullName,
        row.email,
        row.phone,
        row.address,
        row.demoRequest,
      ]
        .join(' ')
        .toLowerCase();
      return haystack.includes(q);
    });
  }

  get newCount(): number {
    return this.requisitions.filter((r) => r.status === 'NEW').length;
  }

  get activeCount(): number {
    return this.requisitions.filter((r) => r.status === 'NEW' || r.status === 'CONTACTED' || r.status === 'SCHEDULED')
      .length;
  }

  reload(): void {
    this.reload$.next();
  }

  setStatusFilter(id: StatusFilter): void {
    this.statusFilter.set(id);
  }

  selectRequisition(row: AdminDemoRequisition): void {
    if (this.selected?.id === row.id && !this.detailLoading) {
      this.mobileShowDetail.set(true);
      return;
    }
    this.detailLoading = true;
    this.detailError = '';
    this.mobileShowDetail.set(true);
    this.cdr.markForCheck();
    this.helpApi
      .fetchDemoRequisitionById(row.id)
      .pipe(
        finalize(() => {
          this.detailLoading = false;
          this.cdr.markForCheck();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (detail) => {
          this.selected = detail;
          this.adminNotesDraft = detail.adminNotes ?? '';
          this.scheduledAtDraft = this.toDatetimeLocal(detail.scheduledAt);
          this.patchListRow(detail);
        },
        error: (err: Error) => {
          this.detailError = err.message;
          this.selected = row;
          this.adminNotesDraft = row.adminNotes ?? '';
          this.scheduledAtDraft = this.toDatetimeLocal(row.scheduledAt);
        },
      });
  }

  backToList(): void {
    this.mobileShowDetail.set(false);
  }

  updateStatus(status: DemoRequisitionStatus): void {
    if (!this.selected || this.actionBusy) {
      return;
    }
    this.actionBusy = true;
    this.cdr.markForCheck();
    const scheduledAt =
      status === 'SCHEDULED' && this.scheduledAtDraft
        ? new Date(this.scheduledAtDraft).toISOString()
        : undefined;
    this.helpApi
      .updateDemoRequisitionStatus(this.selected.id, status, {
        adminNotes: this.adminNotesDraft.trim() || undefined,
        scheduledAt,
      })
      .pipe(
        finalize(() => {
          this.actionBusy = false;
          this.cdr.markForCheck();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updated) => {
          this.selected = updated;
          this.patchListRow(updated);
          this.snackBar.open('Requisition updated.', 'Close', {
            duration: 3000,
            panelClass: ['app-snackbar-success'],
          });
        },
        error: (err: Error) => {
          this.snackBar.open(err.message, 'Close', {
            duration: 5000,
            panelClass: ['app-snackbar-error'],
          });
        },
      });
  }

  initials(name: string): string {
    const parts = String(name ?? '')
      .trim()
      .split(/\s+/)
      .filter(Boolean);
    if (!parts.length) {
      return '?';
    }
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  statusLabel(status: DemoRequisitionStatus): string {
    const map: Record<DemoRequisitionStatus, string> = {
      NEW: 'New',
      CONTACTED: 'Contacted',
      SCHEDULED: 'Scheduled',
      COMPLETED: 'Completed',
      CANCELLED: 'Cancelled',
    };
    return map[status] ?? status;
  }

  statusClass(status: DemoRequisitionStatus): string {
    return `dr-pill dr-pill--${status.toLowerCase()}`;
  }

  formatWhen(iso?: string | null): string {
    if (!iso) {
      return '—';
    }
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) {
      return iso;
    }
    return d.toLocaleString(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    });
  }

  relativeWhen(iso?: string | null): string {
    if (!iso) {
      return '';
    }
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) {
      return '';
    }
    const diffMs = Date.now() - d.getTime();
    const mins = Math.floor(diffMs / 60_000);
    if (mins < 1) {
      return 'Just now';
    }
    if (mins < 60) {
      return `${mins}m ago`;
    }
    const hours = Math.floor(mins / 60);
    if (hours < 24) {
      return `${hours}h ago`;
    }
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  }

  statDisplay(value: number): string {
    return value > 99 ? '99+' : String(value);
  }

  private applyRequisitions(rows: AdminDemoRequisition[]): void {
    this.requisitions = rows;
    this.loading = false;
    if (this.selected) {
      const refreshed = rows.find((r) => r.id === this.selected?.id);
      if (refreshed) {
        this.selected = { ...this.selected, ...refreshed };
      }
    }
    this.cdr.markForCheck();
  }

  private patchListRow(updated: AdminDemoRequisition): void {
    const idx = this.requisitions.findIndex((r) => r.id === updated.id);
    if (idx >= 0) {
      this.requisitions = [
        ...this.requisitions.slice(0, idx),
        updated,
        ...this.requisitions.slice(idx + 1),
      ];
    }
  }

  private toDatetimeLocal(iso?: string | null): string {
    if (!iso) {
      return '';
    }
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) {
      return '';
    }
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }
}
