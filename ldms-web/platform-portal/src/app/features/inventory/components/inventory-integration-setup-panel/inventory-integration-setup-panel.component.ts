import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { Subject, catchError, finalize, of, takeUntil } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { InventoryIntegrationCredentialRow } from '../../models/inventory.model';
import { InventoryPortalService } from '../../services/inventory-portal.service';

@Component({
  selector: 'app-inventory-integration-setup-panel',
  templateUrl: './inventory-integration-setup-panel.component.html',
  styleUrl: './inventory-integration-setup-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class InventoryIntegrationSetupPanelComponent implements OnInit, OnDestroy {
  loading = false;
  saving = false;
  error = '';
  credentials: InventoryIntegrationCredentialRow[] = [];
  copiedKeyId: number | null = null;
  showCreateForm = false;
  crossDockMode = false;

  form: FormGroup;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly authState: AuthStateService,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly route: ActivatedRoute,
  ) {
    this.form = this.fb.group({
      credentialLabel: ['', [Validators.required, Validators.maxLength(150)]],
      webhookUrl: [''],
      callbackGrvUrl: [''],
    });
  }

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.crossDockMode = params.get('mode') === 'crossdock';
      this.cdr.markForCheck();
    });
    this.loadCredentials();
  }

  get docQueryParams(): { mode: string } {
    return { mode: this.crossDockMode ? 'crossdock' : 'inventory' };
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get organizationId(): number {
    return Number(this.authState.currentUser?.organizationId ?? 0);
  }

  loadCredentials(): void {
    if (!this.organizationId) {
      return;
    }
    this.loading = true;
    this.error = '';
    this.inventoryService
      .listIntegrationCredentials(this.organizationId)
      .pipe(
        catchError((err: Error) => {
          this.error = err.message ?? 'Could not load integration credentials.';
          return of([]);
        }),
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((rows) => {
        this.credentials = rows;
        this.cdr.markForCheck();
      });
  }

  toggleCreateForm(): void {
    this.showCreateForm = !this.showCreateForm;
    if (!this.showCreateForm) {
      this.form.reset();
    }
    this.cdr.markForCheck();
  }

  createCredential(): void {
    if (this.form.invalid || !this.organizationId || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving = true;
    this.inventoryService
      .createIntegrationCredential({
        organizationId: this.organizationId,
        credentialLabel: String(raw.credentialLabel ?? '').trim(),
        webhookUrl: String(raw.webhookUrl ?? '').trim() || undefined,
        callbackGrvUrl: String(raw.callbackGrvUrl ?? '').trim() || undefined,
      })
      .pipe(
        finalize(() => {
          this.saving = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (row) => {
          this.credentials = [row, ...this.credentials];
          this.showCreateForm = false;
          this.form.reset();
          this.snackBar.open(
            this.crossDockMode
              ? 'Cross-dock integration key created — copy it now; it will not be shown again.'
              : 'Inventory integration key created — copy it now; it will not be shown again.',
            'Dismiss',
            { duration: 6000 },
          );
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not create credential.', 'Dismiss', { duration: 5000 });
        },
      });
  }

  suspendCredential(row: InventoryIntegrationCredentialRow): void {
    this.inventoryService
      .updateIntegrationCredential({
        id: row.id,
        credentialLabel: row.credentialLabel,
        webhookUrl: row.webhookUrl,
        callbackGrvUrl: row.callbackGrvUrl,
        status: 'SUSPENDED',
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.credentials = this.credentials.map((c) => (c.id === updated.id ? updated : c));
          this.snackBar.open('Credential suspended.', 'Dismiss', { duration: 3000 });
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not suspend credential.', 'Dismiss', { duration: 5000 });
        },
      });
  }

  deleteCredential(row: InventoryIntegrationCredentialRow): void {
    this.inventoryService
      .deleteIntegrationCredential(row.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.credentials = this.credentials.filter((c) => c.id !== row.id);
          this.snackBar.open('Credential removed.', 'Dismiss', { duration: 3000 });
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not delete credential.', 'Dismiss', { duration: 5000 });
        },
      });
  }

  copyApiKey(row: InventoryIntegrationCredentialRow): void {
    void navigator.clipboard?.writeText(row.apiKey).then(() => {
      this.copiedKeyId = row.id;
      this.cdr.markForCheck();
      setTimeout(() => {
        this.copiedKeyId = null;
        this.cdr.markForCheck();
      }, 2000);
    });
  }

  statusClass(status: string): string {
    return status === 'ACTIVE' ? 'inv-integ__pill--active' : 'inv-integ__pill--suspended';
  }
}
