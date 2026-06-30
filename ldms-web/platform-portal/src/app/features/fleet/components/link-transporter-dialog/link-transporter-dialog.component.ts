import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { Subject, debounceTime, distinctUntilChanged, finalize, switchMap, takeUntil } from 'rxjs';
import { TransporterPartnerRow } from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';

/**
 * Search transport companies already on the platform and send them a contract offer.
 * The link stays pending until the transporter accepts from their Connection requests page.
 */
@Component({
  selector: 'app-link-transporter-dialog',
  templateUrl: './link-transporter-dialog.component.html',
  styleUrl: './link-transporter-dialog.component.scss',
  standalone: false,
})
export class LinkTransporterDialogComponent implements OnInit, OnDestroy {
  readonly form: FormGroup;

  searchQuery = '';
  searching = false;
  searchError = '';
  results: TransporterPartnerRow[] = [];
  selected: TransporterPartnerRow | null = null;

  submitting = false;
  saveError = '';

  private readonly search$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly fleet: FleetPortalService,
    private readonly dialogRef: MatDialogRef<LinkTransporterDialogComponent, string | undefined>,
  ) {
    this.form = this.fb.group({
      contractStartDate: ['', Validators.required],
      contractEndDate: [''],
    });
  }

  ngOnInit(): void {
    this.search$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((q) => {
          this.searching = true;
          this.searchError = '';
          return this.fleet.searchTransportCandidates(q).pipe(finalize(() => (this.searching = false)));
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => (this.results = rows),
        error: (err: Error) => {
          this.searchError = err.message ?? 'Could not search transporters.';
          this.results = [];
        },
      });
    this.search$.next('');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchChange(value: string): void {
    this.searchQuery = value;
    this.search$.next(value.trim());
  }

  select(row: TransporterPartnerRow): void {
    this.selected = row;
  }

  hasError(control: string, error: string): boolean {
    const c = this.form.get(control);
    return !!c && c.touched && c.hasError(error);
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  send(): void {
    this.saveError = '';
    if (!this.selected) {
      this.saveError = 'Select a transporter to send an offer to.';
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { contractStartDate, contractEndDate } = this.form.value;
    if (contractEndDate && contractEndDate < contractStartDate) {
      this.saveError = 'Contract end date cannot be before the start date.';
      return;
    }
    this.submitting = true;
    this.fleet
      .linkTransporter(this.selected.id, contractStartDate, contractEndDate || undefined)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (message) => this.dialogRef.close(message),
        error: (err: Error) => (this.saveError = err.message ?? 'Could not send the contract offer.'),
      });
  }
}
