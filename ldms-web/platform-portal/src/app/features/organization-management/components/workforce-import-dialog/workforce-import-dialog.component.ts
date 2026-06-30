import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { from, concatMap, finalize, Observable, of, catchError, map } from 'rxjs';
import { UsersPortalService } from '../../../users/services/users-portal.service';
import { BranchDetail } from '../../../../core/services/organization.service';
import type { BranchStaffRole } from '../branch-staff-dialog/branch-staff-dialog.component';

export interface WorkforceImportDialogData {
  role: BranchStaffRole;
  organizationId: number;
  branches: BranchDetail[];
}

export interface WorkforceImportResult {
  imported: number;
}

interface ParsedRow {
  firstName: string;
  lastName: string;
  email: string;
  username: string;
  phoneNumber: string;
  nationalIdNumber: string;
  gender: string;
  dateOfBirth: string;
  branchRef: string;
  branchId?: number;
  error?: string;
}

const ROLE_TYPE: Record<BranchStaffRole, { name: string; description: string; label: string }> = {
  manager: {
    name: 'Branch Manager',
    description: 'Depot or branch manager — oversees stock receipts and warehouse operations',
    label: 'managers',
  },
  clerk: {
    name: 'Branch Clerk',
    description: 'Depot or branch clerk — receives stock and confirms deliveries',
    label: 'clerks',
  },
};

@Component({
  selector: 'app-workforce-import-dialog',
  templateUrl: './workforce-import-dialog.component.html',
  styleUrl: './workforce-import-dialog.component.scss',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
})
export class WorkforceImportDialogComponent {
  get meta() {
    return ROLE_TYPE[this.data.role];
  }
  rows: ParsedRow[] = [];
  fileName = '';
  parseError = '';
  importing = false;
  done = false;
  successCount = 0;
  failCount = 0;

  constructor(
    private readonly usersPortal: UsersPortalService,
    private readonly dialogRef: MatDialogRef<WorkforceImportDialogComponent, WorkforceImportResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: WorkforceImportDialogData,
  ) {}

  get validRows(): ParsedRow[] {
    return this.rows.filter((r) => !r.error);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.fileName = file.name;
    this.parseError = '';
    this.done = false;
    this.successCount = 0;
    this.failCount = 0;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        this.rows = this.parseCsv(String(reader.result ?? ''));
        if (!this.rows.length) {
          this.parseError = 'No data rows found in the file.';
        }
      } catch {
        this.parseError = 'Could not read the file. Use the template format.';
      }
    };
    reader.readAsText(file);
  }

  downloadTemplate(): void {
    const header = 'firstName,lastName,email,username,phoneNumber,nationalIdNumber,gender,dateOfBirth,branch';
    const sample = 'Jane,Doe,jane.doe@example.co.zw,jane.doe,+263771234567,63-123456-A-12,FEMALE,1990-01-01,Main Branch';
    const blob = new Blob([`${header}\n${sample}\n`], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${this.meta.label}-import-template.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  startImport(): void {
    const rows = this.validRows;
    if (!rows.length || this.importing) {
      return;
    }
    this.importing = true;
    this.successCount = 0;
    this.failCount = 0;

    from(rows)
      .pipe(
        concatMap((row) =>
          this.usersPortal
            .createUser({
              organizationId: this.data.organizationId,
              branchId: row.branchId,
              email: row.email,
              firstName: row.firstName,
              lastName: row.lastName,
              gender: row.gender || 'MALE',
              dateOfBirth: row.dateOfBirth || '1990-01-01',
              phoneNumber: row.phoneNumber,
              nationalIdNumber: row.nationalIdNumber,
              userTypeName: this.meta.name,
              userTypeDescription: this.meta.description,
              preferredLanguage: 'en',
              timezone: 'Africa/Harare',
              issueTemporaryCredentials: true,
            })
            .pipe(
              // Convert success/failure into a tagged result so one bad row doesn't abort the rest.
              concatMapResult(row),
            ),
        ),
        finalize(() => {
          this.importing = false;
          this.done = true;
        }),
      )
      .subscribe({
        next: (ok) => {
          if (ok) {
            this.successCount += 1;
          } else {
            this.failCount += 1;
          }
        },
      });
  }

  close(): void {
    this.dialogRef.close({ imported: this.successCount });
  }

  // ── CSV parsing ─────────────────────────────────────────────────────────────
  private parseCsv(text: string): ParsedRow[] {
    const lines = text.split(/\r?\n/).filter((l) => l.trim().length);
    if (lines.length < 2) {
      return [];
    }
    const header = this.splitCsvLine(lines[0]).map((h) => h.trim().toLowerCase());
    const idx = (name: string) => header.indexOf(name.toLowerCase());
    const col = {
      firstName: idx('firstName'),
      lastName: idx('lastName'),
      email: idx('email'),
      username: idx('username'),
      phoneNumber: idx('phoneNumber'),
      nationalIdNumber: idx('nationalIdNumber'),
      gender: idx('gender'),
      dateOfBirth: idx('dateOfBirth'),
      branch: idx('branch'),
    };

    return lines.slice(1).map((line) => {
      const cells = this.splitCsvLine(line);
      const get = (i: number) => (i >= 0 && i < cells.length ? cells[i].trim() : '');
      const email = get(col.email);
      const firstName = get(col.firstName);
      const lastName = get(col.lastName);
      const branchRef = get(col.branch);
      const branchId = this.resolveBranchId(branchRef);
      const row: ParsedRow = {
        firstName,
        lastName,
        email,
        username: get(col.username) || email.split('@')[0],
        phoneNumber: get(col.phoneNumber),
        nationalIdNumber: get(col.nationalIdNumber),
        gender: get(col.gender) || 'MALE',
        dateOfBirth: (get(col.dateOfBirth) || '1990-01-01').slice(0, 10),
        branchRef,
        branchId,
      };
      row.error = this.validateRow(row);
      return row;
    });
  }

  private validateRow(row: ParsedRow): string | undefined {
    if (!row.firstName || !row.lastName) {
      return 'Missing name';
    }
    if (!row.email || !row.email.includes('@')) {
      return 'Invalid email';
    }
    if (!row.phoneNumber) {
      return 'Missing phone';
    }
    if (!row.branchId) {
      return `Unknown branch "${row.branchRef}"`;
    }
    return undefined;
  }

  private resolveBranchId(ref: string): number | undefined {
    if (!ref) {
      // Fall back to the only branch when there is exactly one.
      return this.data.branches.length === 1 ? this.data.branches[0].id : undefined;
    }
    const needle = ref.trim().toLowerCase();
    const match = this.data.branches.find(
      (b) => b.branchName?.toLowerCase() === needle || b.branchCode?.toLowerCase() === needle,
    );
    return match?.id;
  }

  private splitCsvLine(line: string): string[] {
    const out: string[] = [];
    let cur = '';
    let inQuotes = false;
    for (let i = 0; i < line.length; i++) {
      const ch = line[i];
      if (ch === '"') {
        if (inQuotes && line[i + 1] === '"') {
          cur += '"';
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (ch === ',' && !inQuotes) {
        out.push(cur);
        cur = '';
      } else {
        cur += ch;
      }
    }
    out.push(cur);
    return out;
  }
}

// Helper operator: map a createUser result to boolean success, swallowing errors per-row.
function concatMapResult(_row: ParsedRow) {
  return (source: Observable<unknown>): Observable<boolean> =>
    source.pipe(
      map(() => true),
      catchError(() => of(false)),
    );
}
