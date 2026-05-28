import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
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
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatTableModule],
})
export class IndustryLinkedOrganizationsDialogComponent implements OnInit {
  readonly displayedColumns = ['name', 'classification', 'status'];

  loading = false;
  loadError = '';
  rows: KycQueueRow[] = [];
  totalElements = 0;

  constructor(
    private readonly orgService: OrganizationsAdminService,
    private readonly router: Router,
    private readonly dialogRef: MatDialogRef<IndustryLinkedOrganizationsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) readonly data: IndustryLinkedOrganizationsDialogData,
  ) {}

  ngOnInit(): void {
    this.loadLinkedOrganizations();
  }

  close(): void {
    this.dialogRef.close();
  }

  openFullList(): void {
    void this.router.navigate(['/organizations'], {
      queryParams: { industryId: this.data.industryId, industry: this.data.industryName },
    });
    this.dialogRef.close();
  }

  private loadLinkedOrganizations(): void {
    this.loading = true;
    this.loadError = '';
    this.orgService
      .queryTablePage({
        page: 0,
        size: 100,
        searchQuery: '',
        columnFilters: {},
        industryId: this.data.industryId,
        kycQueueOnly: false,
        organizationDirectoryOnly: true,
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ rows, totalElements }) => {
          this.rows = rows;
          this.totalElements = totalElements;
        },
        error: (err: Error) => {
          this.loadError = err.message ?? 'Failed to load linked organisations';
          this.rows = [];
          this.totalElements = 0;
        },
      });
  }
}
