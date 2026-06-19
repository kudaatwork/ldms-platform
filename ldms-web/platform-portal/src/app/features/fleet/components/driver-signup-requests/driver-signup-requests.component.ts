import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { finalize, Subject, takeUntil } from 'rxjs';
import type { DriverSignupRequestRow } from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';
import {
  FleetDocumentViewerDialogComponent,
  type FleetDocumentViewerDialogData,
} from '../fleet-document-viewer-dialog/fleet-document-viewer-dialog.component';

interface SignupDocLink {
  label: string;
  uploadId: number;
}

@Component({
  selector: 'app-driver-signup-requests',
  templateUrl: './driver-signup-requests.component.html',
  styleUrl: './driver-signup-requests.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
  ],
})
export class DriverSignupRequestsComponent implements OnInit, OnDestroy {
  requests: DriverSignupRequestRow[] = [];
  loading = false;
  error = '';
  actioningId: number | null = null;
  actionError = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fleet: FleetPortalService,
    private readonly dialog: MatDialog,
    private readonly dialogRef: MatDialogRef<DriverSignupRequestsComponent>,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private load(): void {
    this.loading = true;
    this.error = '';
    this.fleet
      .listDriverSignupRequests()
      .pipe(
        finalize(() => (this.loading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => (this.requests = rows),
        error: (err: Error) => (this.error = err.message ?? 'Could not load signup requests.'),
      });
  }

  approve(request: DriverSignupRequestRow): void {
    if (this.actioningId != null) return;
    this.actioningId = request.id;
    this.actionError = '';

    this.fleet
      .approveDriverSignupRequest(request.id)
      .pipe(
        finalize(() => (this.actioningId = null)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (updated) => this.updateRow(updated),
        error: (err: Error) => (this.actionError = err.message ?? 'Could not approve request.'),
      });
  }

  reject(request: DriverSignupRequestRow): void {
    if (this.actioningId != null) return;
    this.actioningId = request.id;
    this.actionError = '';

    this.fleet
      .rejectDriverSignupRequest(request.id)
      .pipe(
        finalize(() => (this.actioningId = null)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (updated) => this.updateRow(updated),
        error: (err: Error) => (this.actionError = err.message ?? 'Could not reject request.'),
      });
  }

  documentLinks(request: DriverSignupRequestRow): SignupDocLink[] {
    const links: SignupDocLink[] = [];
    if (request.nationalIdFrontUploadId) {
      links.push({ label: 'ID front', uploadId: request.nationalIdFrontUploadId });
    }
    if (request.nationalIdBackUploadId) {
      links.push({ label: 'ID back', uploadId: request.nationalIdBackUploadId });
    }
    if (request.licenseFrontUploadId) {
      links.push({ label: 'Licence front', uploadId: request.licenseFrontUploadId });
    }
    if (request.licenseBackUploadId) {
      links.push({ label: 'Licence back', uploadId: request.licenseBackUploadId });
    }
    return links;
  }

  viewDocument(request: DriverSignupRequestRow, link: SignupDocLink): void {
    this.dialog.open<FleetDocumentViewerDialogComponent, FleetDocumentViewerDialogData>(
      FleetDocumentViewerDialogComponent,
      {
        width: '720px',
        maxWidth: '95vw',
        data: {
          fileUploadId: link.uploadId,
          title: `${request.fullName} — ${link.label}`,
        },
      },
    );
  }

  private updateRow(updated: DriverSignupRequestRow): void {
    const idx = this.requests.findIndex((r) => r.id === updated.id);
    if (idx >= 0) {
      this.requests = [
        ...this.requests.slice(0, idx),
        updated,
        ...this.requests.slice(idx + 1),
      ];
    }
  }

  get pendingCount(): number {
    return this.requests.filter((r) => r.status === 'PENDING').length;
  }

  close(): void {
    this.dialogRef.close();
  }
}
