import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { debounceTime, distinctUntilChanged, finalize, Subject, switchMap, takeUntil } from 'rxjs';
import type { FleetDriverRow, MarketplaceDriverRow } from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';

/**
 * Driver Marketplace Search Dialog
 *
 * Allows transporters to search the platform's freelance driver pool and hire
 * a driver directly from results.
 *
 * FLOW:
 * 1. Load all available freelance drivers on open.
 * 2. Debounced search input filters the marketplace via GET /fleet/marketplace/search.
 * 3. "Hire driver" button POSTs to /fleet/marketplace/{id}/hire and closes the dialog.
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
  drivers: MarketplaceDriverRow[] = [];
  loading = false;
  error = '';
  query = '';
  hiringId: number | null = null;
  hireError = '';

  private readonly search$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fleet: FleetPortalService,
    private readonly dialogRef: MatDialogRef<DriverMarketplaceSearchComponent, FleetDriverRow | undefined>,
  ) {}

  ngOnInit(): void {
    // Debounced search — fires initial empty search to load available drivers.
    this.search$
      .pipe(
        debounceTime(350),
        distinctUntilChanged(),
        switchMap((q) => {
          this.loading = true;
          this.error = '';
          return this.fleet.searchMarketplaceDrivers(q).pipe(
            finalize(() => (this.loading = false)),
          );
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => (this.drivers = rows),
        error: (err: Error) => (this.error = err.message ?? 'Could not load drivers.'),
      });

    this.search$.next('');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onQueryChange(): void {
    this.search$.next(this.query);
  }

  hire(driver: MarketplaceDriverRow): void {
    if (this.hiringId != null) return;
    this.hiringId = driver.id;
    this.hireError = '';

    this.fleet
      .hireMarketplaceDriver(driver.id)
      .pipe(
        finalize(() => (this.hiringId = null)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row) => this.dialogRef.close(row),
        error: (err: Error) => (this.hireError = err.message ?? 'Could not hire driver.'),
      });
  }

  close(): void {
    this.dialogRef.close();
  }
}
