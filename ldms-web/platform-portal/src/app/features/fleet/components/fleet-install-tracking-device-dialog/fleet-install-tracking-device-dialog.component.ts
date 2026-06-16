import { CommonModule } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, Optional } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Subject, finalize, takeUntil } from 'rxjs';
import type {
  EditFleetTrackingDevicePayload,
  FleetDriverRow,
  FleetTrackingDeviceRow,
  FleetVehicleRow,
  InstallFleetTrackingDevicePayload,
  TrackingDeviceType,
  TrackingIntegrationProvider,
} from '../../models/fleet.model';
import { FleetPortalService } from '../../services/fleet-portal.service';
import { environment } from '../../../../../environments/environment';

export type FleetInstallTrackingDeviceDialogData = {
  device?: FleetTrackingDeviceRow;
  assets: FleetVehicleRow[];
  drivers: FleetDriverRow[];
};

type WizardStep = 'form' | 'credentials';

interface DeviceTypeOption {
  value: TrackingDeviceType;
  label: string;
  hint: string;
  icon: string;
  isPhone: boolean;
}

interface ProviderOption {
  value: TrackingIntegrationProvider;
  label: string;
  hint: string;
  forPhone: boolean;
  forHardware: boolean;
}

const DEVICE_TYPE_OPTIONS: DeviceTypeOption[] = [
  {
    value: 'MOBILE_PHONE',
    label: 'Mobile phone',
    hint: 'Driver uses the LDMS mobile app on their phone to push GPS coordinates.',
    icon: 'smartphone',
    isPhone: true,
  },
  {
    value: 'OBD_TELEMATICS',
    label: 'OBD telematics unit',
    hint: 'Plugs into the vehicle OBD-II port — streams GPS, speed, and engine data.',
    icon: 'settings_remote',
    isPhone: false,
  },
  {
    value: 'DEDICATED_GPS',
    label: 'Dedicated GPS tracker',
    hint: 'Hardwired GPS device that transmits location via MQTT or HTTP.',
    icon: 'gps_fixed',
    isPhone: false,
  },
  {
    value: 'FUEL_SENSOR',
    label: 'Fuel level sensor',
    hint: 'Tank sensor that reports fuel level telemetry over MQTT.',
    icon: 'local_gas_station',
    isPhone: false,
  },
  {
    value: 'COMBO_UNIT',
    label: 'Combo unit (GPS + fuel)',
    hint: 'Combined GPS tracker and fuel sensor in one device.',
    icon: 'hub',
    isPhone: false,
  },
];

const PROVIDER_OPTIONS: ProviderOption[] = [
  {
    value: 'LDMS_MOBILE',
    label: 'LDMS Mobile app',
    hint: 'Driver app pushes GPS via REST ingest endpoint.',
    forPhone: true,
    forHardware: false,
  },
  {
    value: 'GENERIC_MQTT',
    label: 'Generic MQTT',
    hint: 'Any MQTT-capable device publishing to the platform broker.',
    forPhone: false,
    forHardware: true,
  },
  {
    value: 'TRACCAR',
    label: 'Traccar',
    hint: 'Open-source GPS tracking platform forwarding to LDMS via MQTT.',
    forPhone: false,
    forHardware: true,
  },
  {
    value: 'GEOTAB',
    label: 'Geotab',
    hint: 'Geotab telematics integration via MQTT bridge.',
    forPhone: false,
    forHardware: true,
  },
  {
    value: 'CALAMP',
    label: 'CalAmp',
    hint: 'CalAmp device forwarding telemetry to the LDMS broker.',
    forPhone: false,
    forHardware: true,
  },
  {
    value: 'WIALON',
    label: 'Wialon',
    hint: 'Wialon GPS platform integration via MQTT.',
    forPhone: false,
    forHardware: true,
  },
  {
    value: 'CUSTOM_HTTP',
    label: 'Custom HTTP push',
    hint: 'Custom firmware POSTing telemetry to the LDMS ingest endpoint.',
    forPhone: false,
    forHardware: true,
  },
];

@Component({
  selector: 'app-fleet-install-tracking-device-dialog',
  templateUrl: './fleet-install-tracking-device-dialog.component.html',
  styleUrl: './fleet-install-tracking-device-dialog.component.scss',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule],
})
export class FleetInstallTrackingDeviceDialogComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  currentStep: WizardStep = 'form';
  saving = false;
  saveError = '';

  installedDevice: FleetTrackingDeviceRow | null = null;
  copiedKey: 'ingestKey' | 'mqttTopic' | 'endpoint' | null = null;

  readonly deviceTypeOptions = DEVICE_TYPE_OPTIONS;
  readonly allProviderOptions = PROVIDER_OPTIONS;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly fleet: FleetPortalService,
    @Optional() private readonly dialogRef: MatDialogRef<FleetInstallTrackingDeviceDialogComponent>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: FleetInstallTrackingDeviceDialogData,
  ) {}

  get isEdit(): boolean {
    return !!this.data?.device;
  }

  get assets(): FleetVehicleRow[] {
    return this.data?.assets ?? [];
  }

  get drivers(): FleetDriverRow[] {
    return this.data?.drivers ?? [];
  }

  get title(): string {
    return this.isEdit ? 'Edit tracking device' : 'Install tracking device';
  }

  get subtitle(): string {
    if (this.currentStep === 'credentials') {
      return 'Device installed — copy the ingest credentials below.';
    }
    return this.isEdit
      ? 'Update the device configuration. Ingest credentials are not re-issued on edit.'
      : 'Register a phone or on-board hardware device to start pushing telemetry.';
  }

  get selectedDeviceType(): TrackingDeviceType {
    return this.form?.get('deviceType')?.value ?? 'MOBILE_PHONE';
  }

  get isPhoneDevice(): boolean {
    const opt = DEVICE_TYPE_OPTIONS.find((o) => o.value === this.selectedDeviceType);
    return opt?.isPhone ?? true;
  }

  get filteredProviders(): ProviderOption[] {
    return this.allProviderOptions.filter((p) => (this.isPhoneDevice ? p.forPhone : p.forHardware));
  }

  get isHardwareDevice(): boolean {
    return !this.isPhoneDevice;
  }

  get selectedProviderHint(): string {
    const selected = this.form?.get('integrationProvider')?.value as TrackingIntegrationProvider;
    return PROVIDER_OPTIONS.find((p) => p.value === selected)?.hint ?? '';
  }

  get ingestEndpoint(): string {
    return `${environment.apiUrl}/ldms-trip-tracking/v1/system/telemetry/ingest`;
  }

  get ingestPayloadHint(): string {
    return `{ "ingestKey": "${this.installedDevice?.ingestKey ?? '<key>'}", "latitude": 0.0, "longitude": 0.0, "speedKmh": 0, "headingDeg": 0 }`;
  }

  ngOnInit(): void {
    this.buildForm();
    if (this.isEdit && this.data.device) {
      this.prefillForm(this.data.device);
    }
    this.watchDeviceTypeChanges();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.saveError = '';

    const fv = this.form.value;

    if (this.isEdit && this.data.device) {
      const payload: EditFleetTrackingDevicePayload = {
        deviceLabel: fv.deviceLabel,
        deviceType: fv.deviceType,
        integrationProvider: fv.integrationProvider,
        fleetAssetId: fv.fleetAssetId || undefined,
        fleetDriverId: fv.fleetDriverId || undefined,
        deviceSerial: fv.deviceSerial?.trim() || undefined,
        externalDeviceId: fv.externalDeviceId?.trim() || undefined,
        tracksGps: fv.tracksGps,
        tracksFuel: fv.tracksFuel,
        notes: fv.notes?.trim() || undefined,
      };
      this.fleet
        .updateTrackingDevice(this.data.device.id, payload)
        .pipe(
          finalize(() => (this.saving = false)),
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: (updated) => {
            this.dialogRef?.close(updated);
          },
          error: (err: Error) => {
            this.saveError = err.message;
          },
        });
    } else {
      const payload: InstallFleetTrackingDevicePayload = {
        deviceLabel: fv.deviceLabel,
        deviceType: fv.deviceType,
        integrationProvider: fv.integrationProvider,
        fleetAssetId: fv.fleetAssetId || undefined,
        fleetDriverId: fv.fleetDriverId || undefined,
        deviceSerial: fv.deviceSerial?.trim() || undefined,
        externalDeviceId: fv.externalDeviceId?.trim() || undefined,
        tracksGps: fv.tracksGps,
        tracksFuel: fv.tracksFuel,
        notes: fv.notes?.trim() || undefined,
      };
      this.fleet
        .installTrackingDevice(payload)
        .pipe(
          finalize(() => (this.saving = false)),
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: (device) => {
            this.installedDevice = device;
            this.currentStep = 'credentials';
          },
          error: (err: Error) => {
            this.saveError = err.message;
          },
        });
    }
  }

  copyText(type: 'ingestKey' | 'mqttTopic' | 'endpoint', value: string | undefined): void {
    if (!value) return;
    navigator.clipboard.writeText(value).then(() => {
      this.copiedKey = type;
      setTimeout(() => {
        if (this.copiedKey === type) {
          this.copiedKey = null;
        }
      }, 2000);
    });
  }

  done(): void {
    this.dialogRef?.close(this.installedDevice);
  }

  cancel(): void {
    this.dialogRef?.close(null);
  }

  hasError(controlName: string, error: string): boolean {
    const ctrl = this.form?.get(controlName);
    return !!(ctrl && ctrl.touched && ctrl.hasError(error));
  }

  private buildForm(): void {
    this.form = this.fb.group({
      deviceLabel: ['', [Validators.required, Validators.maxLength(100)]],
      deviceType: ['MOBILE_PHONE', Validators.required],
      integrationProvider: ['LDMS_MOBILE', Validators.required],
      fleetAssetId: [null],
      fleetDriverId: [null],
      deviceSerial: [''],
      externalDeviceId: [''],
      tracksGps: [true],
      tracksFuel: [false],
      notes: [''],
    });
  }

  private prefillForm(device: FleetTrackingDeviceRow): void {
    this.form.patchValue({
      deviceLabel: device.deviceLabel,
      deviceType: device.deviceType,
      integrationProvider: device.integrationProvider,
      fleetAssetId: device.fleetAssetId ?? null,
      fleetDriverId: device.fleetDriverId ?? null,
      deviceSerial: device.deviceSerial ?? '',
      externalDeviceId: device.externalDeviceId ?? '',
      tracksGps: device.tracksGps,
      tracksFuel: device.tracksFuel,
      notes: device.notes ?? '',
    });
  }

  private watchDeviceTypeChanges(): void {
    this.form.get('deviceType')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((type: TrackingDeviceType) => {
      const isPhone = DEVICE_TYPE_OPTIONS.find((o) => o.value === type)?.isPhone ?? true;
      const providerCtrl = this.form.get('integrationProvider');
      if (isPhone) {
        providerCtrl?.setValue('LDMS_MOBILE');
      } else {
        const currentProvider = providerCtrl?.value as TrackingIntegrationProvider;
        const validHardware = PROVIDER_OPTIONS.filter((p) => p.forHardware).map((p) => p.value);
        if (!validHardware.includes(currentProvider)) {
          providerCtrl?.setValue('GENERIC_MQTT');
        }
      }
    });
  }
}
