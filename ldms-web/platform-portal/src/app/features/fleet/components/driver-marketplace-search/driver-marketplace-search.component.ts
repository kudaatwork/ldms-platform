import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  finalize,
  of,
  Subject,
  switchMap,
  takeUntil,
} from 'rxjs';
import type { DriverSignupRequestRow, FleetDriverRow, MarketplaceDriverRow } from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';
import {
  FleetDocumentViewerDialogComponent,
  type FleetDocumentViewerDialogData,
} from '../fleet-document-viewer-dialog/fleet-document-viewer-dialog.component';

interface SignupDocLink {
  label: string;
  uploadId: number;
}

/**
 * Transporter marketplace: search freelance drivers, review applicant profiles,
 * request (approve) new signups, or hire already-approved pool drivers.
 */
@Component({
  selector: 'app-driver-marketplace-search',
  templateUrl: './driver-marketplace-search.component.html',
  styleUrl: './driver-marketplace-search.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
  ],
})
export class DriverMarketplaceSearchComponent implements OnInit, OnDestroy {
  applicants: DriverSignupRequestRow[] = [];
  filteredApplicants: DriverSignupRequestRow[] = [];
  drivers: MarketplaceDriverRow[] = [];
  applicantsLoading = false;
  poolLoading = false;
  applicantsError = '';
  poolError = '';
  query = '';
  hiringId: number | null = null;
  actioningApplicantId: number | null = null;
  actionError = '';
  expandedApplicantId: number | null = null;
  activeTab: 'applicants' | 'pool' = 'applicants';

  private allApplicants: DriverSignupRequestRow[] = [];
  private readonly search$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fleet: FleetPortalService,
    private readonly dialog: MatDialog,
    private readonly dialogRef: MatDialogRef<DriverMarketplaceSearchComponent, FleetDriverRow | undefined>,
  ) {}

  ngOnInit(): void {
    this.loadApplicants();

    this.search$
      .pipe(
        debounceTime(350),
        distinctUntilChanged(),
        switchMap((q) => {
          this.poolLoading = true;
          this.poolError = '';
          return this.fleet.searchMarketplaceDrivers(q).pipe(
            catchError((err: Error) => {
              this.poolError = err.message ?? 'Could not search available drivers.';
              return of([] as MarketplaceDriverRow[]);
            }),
            finalize(() => (this.poolLoading = false)),
          );
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((rows) => {
        this.drivers = rows;
      });

    this.search$.next('');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onQueryChange(): void {
    this.filteredApplicants = this.filterApplicants(this.query);
    this.search$.next(this.query);
  }

  selectTab(tab: 'applicants' | 'pool'): void {
    this.activeTab = tab;
  }

  toggleProfile(applicant: DriverSignupRequestRow): void {
    this.expandedApplicantId = this.expandedApplicantId === applicant.id ? null : applicant.id;
  }

  documentLinks(applicant: DriverSignupRequestRow): SignupDocLink[] {
    const links: SignupDocLink[] = [];
    if (applicant.nationalIdFrontUploadId) {
      links.push({ label: 'ID front', uploadId: applicant.nationalIdFrontUploadId });
    }
    if (applicant.nationalIdBackUploadId) {
      links.push({ label: 'ID back', uploadId: applicant.nationalIdBackUploadId });
    }
    if (applicant.licenseFrontUploadId) {
      links.push({ label: 'Licence front', uploadId: applicant.licenseFrontUploadId });
    }
    if (applicant.licenseBackUploadId) {
      links.push({ label: 'Licence back', uploadId: applicant.licenseBackUploadId });
    }
    return links;
  }

  viewDocument(applicant: DriverSignupRequestRow, link: SignupDocLink): void {
    this.dialog.open<FleetDocumentViewerDialogComponent, FleetDocumentViewerDialogData>(
      FleetDocumentViewerDialogComponent,
      {
        width: '720px',
        maxWidth: '95vw',
        data: {
          fileUploadId: link.uploadId,
          title: `${applicant.fullName} — ${link.label}`,
        },
      },
    );
  }

  requestDriver(applicant: DriverSignupRequestRow): void {
    if (this.actioningApplicantId != null) return;
    this.actioningApplicantId = applicant.id;
    this.actionError = '';

    this.fleet
      .approveDriverSignupRequest(applicant.id)
      .pipe(
        finalize(() => (this.actioningApplicantId = null)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.allApplicants = this.allApplicants.filter((a) => a.id !== applicant.id);
          this.filteredApplicants = this.filterApplicants(this.query);
          this.dialogRef.close({ fullName: applicant.fullName } as FleetDriverRow);
        },
        error: (err: Error) => (this.actionError = err.message ?? 'Could not request driver.'),
      });
  }

  declineApplicant(applicant: DriverSignupRequestRow): void {
    if (this.actioningApplicantId != null) return;
    this.actioningApplicantId = applicant.id;
    this.actionError = '';

    this.fleet
      .rejectDriverSignupRequest(applicant.id)
      .pipe(
        finalize(() => (this.actioningApplicantId = null)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: () => {
          this.allApplicants = this.allApplicants.filter((a) => a.id !== applicant.id);
          this.filteredApplicants = this.filterApplicants(this.query);
        },
        error: (err: Error) => (this.actionError = err.message ?? 'Could not decline applicant.'),
      });
  }

  hire(driver: MarketplaceDriverRow): void {
    if (this.hiringId != null) return;
    this.hiringId = driver.id;
    this.actionError = '';

    this.fleet
      .hireMarketplaceDriver(driver.id)
      .pipe(
        finalize(() => (this.hiringId = null)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row) => this.dialogRef.close(row),
        error: (err: Error) => (this.actionError = err.message ?? 'Could not hire driver.'),
      });
  }

  close(): void {
    this.dialogRef.close();
  }

  get applicantCount(): number {
    return this.filteredApplicants.length;
  }

  get poolCount(): number {
    return this.drivers.length;
  }

  private loadApplicants(): void {
    this.applicantsLoading = true;
    this.applicantsError = '';
    this.fleet
      .listFreelanceSignupMarketplace()
      .pipe(
        finalize(() => (this.applicantsLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.allApplicants = rows.filter((a) => a.status === 'PENDING');
          this.applicants = this.allApplicants;
          this.filteredApplicants = this.filterApplicants(this.query);
        },
        error: (err: Error) => {
          this.applicantsError = err.message ?? 'Could not load freelance applicants.';
        },
      });
  }

  private filterApplicants(term: string): DriverSignupRequestRow[] {
    const q = term.trim().toLowerCase();
    if (!q) {
      return [...this.allApplicants];
    }
    return this.allApplicants.filter((a) => {
      const haystack = [
        a.fullName,
        a.email,
        a.phoneNumber,
        a.licenseNumber,
        a.licenseClass,
        a.nationalIdNumber,
      ]
        .join(' ')
        .toLowerCase();
      return haystack.includes(q);
    });
  }
}
