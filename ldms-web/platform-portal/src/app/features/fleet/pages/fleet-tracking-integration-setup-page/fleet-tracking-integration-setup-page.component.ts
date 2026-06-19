import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Title } from '@angular/platform-browser';
import { Subject, catchError, finalize, forkJoin, of, takeUntil } from 'rxjs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import {
  FleetInstallTrackingDeviceDialogComponent,
  type FleetInstallTrackingDeviceDialogData,
} from '../../components/fleet-install-tracking-device-dialog/fleet-install-tracking-device-dialog.component';
import type {
  FleetTrackingDeviceRow,
  FleetTrackingIntegrationCredentialRow,
  FleetVehicleRow,
  TrackingIntegrationProvider,
} from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';

interface IntegratorProviderOption {
  value: TrackingIntegrationProvider;
  label: string;
  hint: string;
}

const INTEGRATOR_PROVIDERS: IntegratorProviderOption[] = [
  { value: 'CUSTOM_HTTP', label: 'Custom HTTP push', hint: 'Firmware or middleware POSTing to the LDMS telemetry ingest endpoint.' },
  { value: 'GENERIC_MQTT', label: 'Generic MQTT', hint: 'Device or bridge publishing GPS JSON to the platform broker topic.' },
  { value: 'TRACCAR', label: 'Traccar forwarder', hint: 'Traccar instance forwarding positions into LDMS.' },
  { value: 'GEOTAB', label: 'Geotab', hint: 'Geotab telematics bridge.' },
  { value: 'WIALON', label: 'Wialon', hint: 'Wialon platform MQTT/HTTP bridge.' },
  { value: 'CALAMP', label: 'CalAmp', hint: 'CalAmp hardware forwarding telemetry.' },
];

@Component({
  selector: 'app-fleet-tracking-integration-setup-page',
  templateUrl: './fleet-tracking-integration-setup-page.component.html',
  styleUrl: './fleet-tracking-integration-setup-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class FleetTrackingIntegrationSetupPageComponent implements OnInit, OnDestroy {
  loading = false;
  saving = false;
  error = '';
  credentials: FleetTrackingIntegrationCredentialRow[] = [];
  vehicles: FleetVehicleRow[] = [];
  copiedCredentialId: number | null = null;
  showCreateForm = false;
  issuedKey: { label: string; ingestKey: string; mqttTopic?: string } | null = null;

  readonly integratorProviders = INTEGRATOR_PROVIDERS;
  form: FormGroup;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly title: Title,
    private readonly fb: FormBuilder,
    private readonly fleetService: FleetPortalService,
    private readonly authState: AuthStateService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group({
      credentialLabel: ['', [Validators.required, Validators.maxLength(150)]],
      integrationProvider: ['CUSTOM_HTTP', Validators.required],
      fleetAssetId: [null as number | null, Validators.required],
      externalDeviceId: [''],
      notes: [''],
    });
  }

  ngOnInit(): void {
    this.title.setTitle('Tracking integration setup | LX Platform');
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get organizationId(): number {
    return Number(this.authState.currentUser?.organizationId ?? 0);
  }

  loadPage(): void {
    if (!this.organizationId) {
      this.error = 'Sign in with an organisation account to manage integrator keys.';
      return;
    }
    this.loading = true;
    this.error = '';
    forkJoin({
      credentials: this.fleetService
        .listTrackingIntegrationCredentials(this.organizationId)
        .pipe(catchError((err: Error) => {
          this.error = err.message ?? 'Could not load integrator keys.';
          return of([] as FleetTrackingIntegrationCredentialRow[]);
        })),
      vehicles: this.fleetService.listOwnFleet().pipe(catchError(() => of([] as FleetVehicleRow[]))),
    })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe(({ credentials, vehicles }) => {
        this.credentials = credentials;
        this.vehicles = vehicles;
        this.cdr.markForCheck();
      });
  }

  toggleCreateForm(): void {
    this.showCreateForm = !this.showCreateForm;
    this.issuedKey = null;
    if (!this.showCreateForm) {
      this.form.reset({
        integrationProvider: 'CUSTOM_HTTP',
        fleetAssetId: null,
      });
    }
    this.cdr.markForCheck();
  }

  createIntegratorKey(): void {
    if (this.form.invalid || this.saving || !this.organizationId) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving = true;
    this.fleetService
      .createTrackingIntegrationCredential({
        organizationId: this.organizationId,
        credentialLabel: String(raw.credentialLabel ?? '').trim(),
        integrationProvider: raw.integrationProvider as TrackingIntegrationProvider,
        fleetAssetId: Number(raw.fleetAssetId),
        externalDeviceId: String(raw.externalDeviceId ?? '').trim() || undefined,
        notes: String(raw.notes ?? '').trim() || undefined,
      })
      .pipe(
        finalize(() => {
          this.saving = false;
          this.cdr.markForCheck();
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (credential) => {
          this.credentials = [credential, ...this.credentials];
          this.showCreateForm = false;
          this.form.reset({ integrationProvider: 'CUSTOM_HTTP', fleetAssetId: null });
          if (credential.ingestKey) {
            this.issuedKey = {
              label: credential.credentialLabel,
              ingestKey: credential.ingestKey,
              mqttTopic: credential.mqttTopic,
            };
          }
          this.snackBar.open('Integrator ingest key created — copy it now.', 'Dismiss', { duration: 6000 });
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not create integrator key.', 'Dismiss', { duration: 5000 });
        },
      });
  }

  openAdvancedInstall(): void {
    const data: FleetInstallTrackingDeviceDialogData = {
      assets: this.vehicles,
      drivers: [],
    };
    this.dialog
      .open(FleetInstallTrackingDeviceDialogComponent, {
        width: '780px',
        maxWidth: '96vw',
        maxHeight: '92vh',
        disableClose: true,
        data,
      })
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((device: FleetTrackingDeviceRow | null | undefined) => {
        if (device?.integrationProvider && device.integrationProvider !== 'LDMS_MOBILE') {
          this.loadPage();
          if (device.ingestKey) {
            this.issuedKey = {
              label: device.deviceLabel,
              ingestKey: device.ingestKey,
              mqttTopic: device.mqttTopic,
            };
          }
          this.cdr.markForCheck();
        }
      });
  }

  suspendCredential(credential: FleetTrackingIntegrationCredentialRow): void {
    this.fleetService
      .suspendTrackingIntegrationCredential(credential.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.credentials = this.credentials.map((c) => (c.id === updated.id ? updated : c));
          this.snackBar.open('Integrator key suspended.', 'Dismiss', { duration: 3000 });
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not suspend key.', 'Dismiss', { duration: 5000 });
        },
      });
  }

  deleteCredential(credential: FleetTrackingIntegrationCredentialRow): void {
    this.fleetService
      .deleteTrackingIntegrationCredential(credential.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.credentials = this.credentials.filter((c) => c.id !== credential.id);
          this.snackBar.open('Integrator key removed.', 'Dismiss', { duration: 3000 });
          this.cdr.markForCheck();
        },
        error: (err: Error) => {
          this.snackBar.open(err.message ?? 'Could not remove key.', 'Dismiss', { duration: 5000 });
        },
      });
  }

  copyIngestKey(credential: FleetTrackingIntegrationCredentialRow): void {
    const key = credential.ingestKey;
    if (!key || key.includes('…')) {
      this.snackBar.open('Full ingest key is only shown at creation.', 'Dismiss', { duration: 4000 });
      return;
    }
    void navigator.clipboard?.writeText(key).then(() => {
      this.copiedCredentialId = credential.id;
      this.cdr.markForCheck();
      setTimeout(() => {
        this.copiedCredentialId = null;
        this.cdr.markForCheck();
      }, 2000);
    });
  }

  copyIssuedKey(): void {
    if (!this.issuedKey?.ingestKey) {
      return;
    }
    void navigator.clipboard?.writeText(this.issuedKey.ingestKey);
    this.snackBar.open('Ingest key copied.', 'Dismiss', { duration: 2500 });
  }

  dismissIssuedKey(): void {
    this.issuedKey = null;
    this.cdr.markForCheck();
  }

  maskKey(key?: string): string {
    if (!key) {
      return '—';
    }
    if (key.includes('…')) {
      return key;
    }
    if (key.length <= 16) {
      return key;
    }
    return `${key.slice(0, 8)}…${key.slice(-4)}`;
  }

  statusClass(status: string): string {
    return status === 'ACTIVE' ? 'flt-integ__pill--active' : 'flt-integ__pill--suspended';
  }

  selectedProviderHint(): string {
    const value = this.form.get('integrationProvider')?.value as TrackingIntegrationProvider | undefined;
    return this.integratorProviders.find((p) => p.value === value)?.hint ?? '';
  }
}
