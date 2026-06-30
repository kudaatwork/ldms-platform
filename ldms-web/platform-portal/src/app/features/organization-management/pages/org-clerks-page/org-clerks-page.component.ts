import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, finalize, takeUntil } from 'rxjs';
import { DeleteConfirmDialogComponent } from '../../../../shared/components/delete-confirm-dialog/delete-confirm-dialog.component';
import {
  exportClientTableAsCsv,
  exportFormatLabel,
  LxExportFormat,
} from '../../../../shared/utils/lx-export.util';
import { OrgContextService } from '../../../../core/services/org-context.service';
import { UsersPortalService, UserListRow } from '../../../users/services/users-portal.service';
import { OrganizationService, BranchDetail } from '../../../../core/services/organization.service';
import { BranchStaffDialogComponent } from '../../components/branch-staff-dialog/branch-staff-dialog.component';
import { WorkforceImportDialogComponent } from '../../components/workforce-import-dialog/workforce-import-dialog.component';

@Component({
  selector: 'app-org-clerks-page',
  templateUrl: './org-clerks-page.component.html',
  styleUrl: './org-clerks-page.component.scss',
  standalone: false,
})
export class OrgClerksPageComponent implements OnInit, OnDestroy {
  loading = true;
  actionInProgress = false;
  exporting = false;
  error = '';
  clerks: UserListRow[] = [];
  branches: BranchDetail[] = [];

  searchQuery = '';
  filterFieldsOpen = false;
  columnFilters = {
    name: '',
    branchId: '' as number | '',
    status: '' as boolean | '',
  };

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly usersPortal: UsersPortalService,
    private readonly orgService: OrganizationService,
    private readonly orgContext: OrgContextService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly title: Title,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  get filteredClerks(): UserListRow[] {
    const q = this.searchQuery.trim().toLowerCase();
    return this.clerks.filter((clerk) => {
      if (this.columnFilters.status === true && clerk.status !== 'ACTIVE') {
        return false;
      }
      if (this.columnFilters.status === false && clerk.status === 'ACTIVE') {
        return false;
      }
      const nameNeedle = this.columnFilters.name.trim().toLowerCase();
      if (nameNeedle && !clerk.name.toLowerCase().includes(nameNeedle)) {
        return false;
      }
      if (!q) {
        return true;
      }
      const hay = [clerk.name, clerk.email, clerk.phoneNumber, clerk.username].join(' ').toLowerCase();
      return hay.includes(q);
    });
  }

  get branchOptions(): BranchDetail[] {
    return this.branches;
  }

  ngOnInit(): void {
    this.title.setTitle('Clerks | Organization management');
    this.loadBranches();
    this.reload();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  reload(): void {
    this.loading = true;
    this.error = '';
    const organizationId = this.orgContext.organizationId;
    if (!organizationId) {
      this.error = 'No organisation context found. Please sign in again.';
      this.loading = false;
      return;
    }
    this.usersPortal
      .queryUsers({
        page: 0,
        size: 1000,
        searchQuery: '',
        columnFilters: { userTypeName: 'Branch Clerk' },
        organizationId,
      })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: ({ rows }) => {
          this.clerks = rows;
        },
        error: (err: Error) => {
          this.error = err.message ?? 'Could not load clerks.';
        },
      });
  }

  exportAs(format: LxExportFormat): void {
    const organizationId = this.orgContext.organizationId;
    if (!organizationId) {
      return;
    }
    this.exporting = true;
    this.usersPortal
      .exportUsers(
        {
          page: 0,
          size: 1000,
          searchQuery: '',
          columnFilters: { userTypeName: 'Branch Clerk' },
          organizationId,
        },
        format,
      )
      .pipe(finalize(() => (this.exporting = false)))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `clerks-export.${format === 'xlsx' ? 'xlsx' : format === 'pdf' ? 'pdf' : 'csv'}`;
          a.click();
          URL.revokeObjectURL(url);
        },
        error: (err: Error) => this.snackBar.open(err.message ?? 'Export failed', 'Close', { duration: 5000 }),
      });
  }

  openCreate(): void {
    const organizationId = this.orgContext.organizationId;
    if (!organizationId) {
      this.snackBar.open('No organisation context found.', 'Close', { duration: 4000 });
      return;
    }
    if (!this.branches.length) {
      this.snackBar.open('Create a branch first — clerks belong to a branch.', 'Close', { duration: 5000 });
      return;
    }
    this.dialog
      .open(BranchStaffDialogComponent, {
        data: { role: 'clerk', organizationId, branches: this.branches },
        width: '640px',
        maxWidth: '96vw',
        panelClass: 'lx-dialog-panel',
      })
      .afterClosed()
      .subscribe((result) => {
        if (result?.created || result?.updated) {
          this.reload();
        }
      });
  }

  openEdit(clerk: UserListRow): void {
    const organizationId = this.orgContext.organizationId;
    if (!organizationId) {
      return;
    }
    this.dialog
      .open(BranchStaffDialogComponent, {
        data: { role: 'clerk', organizationId, branches: this.branches, user: clerk },
        width: '640px',
        maxWidth: '96vw',
        panelClass: 'lx-dialog-panel',
      })
      .afterClosed()
      .subscribe((result) => {
        if (result?.created || result?.updated) {
          this.reload();
        }
      });
  }

  openImport(): void {
    const organizationId = this.orgContext.organizationId;
    if (!organizationId) {
      return;
    }
    this.dialog
      .open(WorkforceImportDialogComponent, {
        data: { role: 'clerk', organizationId, branches: this.branches },
        width: '660px',
        maxWidth: '96vw',
        panelClass: 'lx-dialog-panel',
      })
      .afterClosed()
      .subscribe((result) => {
        if (result?.imported) {
          this.reload();
        }
      });
  }

  deleteClerk(clerk: UserListRow): void {
    this.dialog
      .open(DeleteConfirmDialogComponent, {
        data: {
          title: 'Remove clerk?',
          message: `This will remove ${clerk.name} from the organisation. This action cannot be undone.`,
          confirmLabel: 'Remove',
        },
        width: '420px',
        maxWidth: '96vw',
        panelClass: 'lx-dialog-panel',
      })
      .afterClosed()
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.actionInProgress = true;
          this.usersPortal
            .deleteUser(clerk.id)
            .pipe(finalize(() => (this.actionInProgress = false)))
            .subscribe({
              next: () => {
                this.snackBar.open('Clerk removed', 'Close', { duration: 3000, panelClass: ['app-snackbar-success'] });
                this.reload();
              },
              error: (err: Error) => this.snackBar.open(err.message ?? 'Remove failed', 'Close', { duration: 5000 }),
            });
        }
      });
  }

  hasActiveFilters(): boolean {
    return (
      !!this.columnFilters.name.trim() ||
      this.columnFilters.branchId !== '' ||
      this.columnFilters.status !== ''
    );
  }

  clerkBranchName(clerk: UserListRow): string {
    if (clerk.branchId == null) {
      return '—';
    }
    const branch = this.branches.find((b) => b.id === clerk.branchId);
    return branch?.branchName ?? '—';
  }

  private loadBranches(): void {
    this.orgService.listBranches().pipe(takeUntil(this.destroy$)).subscribe({
      next: (branches) => {
        this.branches = branches;
      },
      error: () => {
        this.branches = [];
      },
    });
  }
}
