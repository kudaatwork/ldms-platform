import { HttpClient, HttpResponse } from '@angular/common/http';
import { Component, OnDestroy } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, of } from 'rxjs';
import { catchError, delay, finalize, takeUntil } from 'rxjs/operators';
import { environment } from '../../../../../environments/environment';

export type CodeLanguage = 'curl' | 'javascript' | 'python' | 'java' | 'php' | 'csharp' | 'ruby';
export type ApiSection = 'overview' | 'contract' | 'samples' | 'connect' | 'partner' | 'playground';
export type MockScenario = 'active_trip' | 'no_trip' | 'invalid_key';

export interface TelemetryField {
  name: string;
  type: string;
  required: boolean;
  description: string;
  example: string;
}

export interface IntegrationPath {
  id: string;
  icon: string;
  title: string;
  tagline: string;
  steps: string[];
  bestFor: string;
}

export interface ProviderBridge {
  name: string;
  icon: string;
  description: string;
  howTo: string;
}

export interface ApiPlaygroundResult {
  status: number;
  durationMs: number;
  body: unknown;
  requestUrl: string;
  requestBody: string;
}

interface MockTelemetryResponse {
  statusCode: number;
  success: boolean;
  message: string;
  errorMessages?: string[];
  tripId?: number;
  tripNumber?: string;
}

interface PlaygroundHttpError {
  status: number;
  body: unknown;
  fromError: true;
}

const TELEMETRY_ENDPOINT = '/ldms-trip-tracking/v1/system/telemetry/ingest';

const DEMO_KEYS: Record<MockScenario, string> = {
  active_trip: 'demo-ingest-key-active',
  no_trip: 'demo-ingest-key-idle',
  invalid_key: 'demo-ingest-key-invalid',
};

@Component({
  selector: 'app-fleet-tracking-api-page',
  templateUrl: './fleet-tracking-api-page.component.html',
  styleUrl: './fleet-tracking-api-page.component.scss',
  standalone: false,
})
export class FleetTrackingApiPageComponent implements OnDestroy {
  activeSection: ApiSection = 'overview';
  activeLanguage: CodeLanguage = 'curl';
  mockMode = true;
  mockScenario: MockScenario = 'active_trip';
  sending = false;
  copiedToken: string | null = null;

  playground = {
    ingestKey: DEMO_KEYS.active_trip,
    latitude: -17.8252,
    longitude: 31.0335,
    speedKmh: 62.5,
    headingDeg: 145,
    fuelLevelPct: 78,
  };

  lastResult: ApiPlaygroundResult | null = null;

  readonly sections: { id: ApiSection; label: string; icon: string }[] = [
    { id: 'overview', label: 'Overview', icon: 'rocket_launch' },
    { id: 'contract', label: 'REST contract', icon: 'description' },
    { id: 'samples', label: 'Code samples', icon: 'code' },
    { id: 'connect', label: 'Connect trackers', icon: 'hub' },
    { id: 'partner', label: 'Partner SDK', icon: 'handshake' },
    { id: 'playground', label: 'Try it', icon: 'play_circle' },
  ];

  readonly languages: { id: CodeLanguage; label: string }[] = [
    { id: 'curl', label: 'cURL' },
    { id: 'javascript', label: 'JavaScript' },
    { id: 'python', label: 'Python' },
    { id: 'java', label: 'Java' },
    { id: 'csharp', label: 'C#' },
    { id: 'ruby', label: 'Ruby' },
    { id: 'php', label: 'PHP' },
  ];

  readonly mockScenarios: { id: MockScenario; label: string; hint: string }[] = [
    { id: 'active_trip', label: 'Active trip (200)', hint: 'Telemetry accepted and trip updated' },
    { id: 'no_trip', label: 'No trip (202)', hint: 'Device valid but no IN_TRANSIT trip' },
    { id: 'invalid_key', label: 'Invalid key (401)', hint: 'Ingest key not recognised' },
  ];

  readonly requestFields: TelemetryField[] = [
    {
      name: 'ingestKey',
      type: 'string',
      required: true,
      description: 'Secret key issued when you install a tracking device. Identifies the device — no JWT required.',
      example: 'a1b2c3d4e5f6…',
    },
    {
      name: 'latitude',
      type: 'decimal',
      required: true,
      description: 'GPS latitude in decimal degrees (WGS-84).',
      example: '-17.8252',
    },
    {
      name: 'longitude',
      type: 'decimal',
      required: true,
      description: 'GPS longitude in decimal degrees (WGS-84).',
      example: '31.0335',
    },
    {
      name: 'speedKmh',
      type: 'decimal',
      required: false,
      description: 'Ground speed in km/h. Used for corridor progress and ETA.',
      example: '62.5',
    },
    {
      name: 'headingDeg',
      type: 'decimal',
      required: false,
      description: 'Compass heading 0–360°. Used for map rotation.',
      example: '145',
    },
    {
      name: 'fuelLevelPct',
      type: 'decimal',
      required: false,
      description: 'Tank level 0–100% when the device reports fuel telemetry.',
      example: '78',
    },
    {
      name: 'recordedAt',
      type: 'datetime',
      required: false,
      description: 'ISO-8601 timestamp of the GPS fix. Defaults to server time if omitted.',
      example: '2026-06-17T14:32:00',
    },
  ];

  readonly responseCodes = [
    { code: 200, label: 'OK', detail: 'Telemetry ingested. An IN_TRANSIT trip was found and the live map updated.' },
    { code: 202, label: 'Accepted', detail: 'Telemetry received but no active trip for this device\'s vehicle.' },
    { code: 400, label: 'Bad request', detail: 'Missing ingestKey, device not ACTIVE, or GPS tracking disabled.' },
    { code: 401, label: 'Unauthorized', detail: 'Ingest key not found or revoked.' },
    { code: 503, label: 'Unavailable', detail: 'Fleet device lookup service temporarily unreachable.' },
  ];

  readonly integrationPaths: IntegrationPath[] = [
    {
      id: 'rest',
      icon: 'http',
      title: 'REST HTTP push',
      tagline: 'Simplest path — POST JSON from your app or firmware.',
      steps: [
        'Install a device and link it to a vehicle on the Device installation page.',
        'Copy the ingest key shown once after install.',
        'POST telemetry JSON to the ingest endpoint every 5–30 seconds while the vehicle is moving.',
      ],
      bestFor: 'Mobile apps, custom firmware, cron-based forwarders, low-code integrations.',
    },
    {
      id: 'mqtt',
      icon: 'wifi_tethering',
      title: 'MQTT bridge',
      tagline: 'Publish to the LDMS broker — ideal for onboard hardware.',
      steps: [
        'Install a hardware device (OBD, GPS, combo unit) and select a MQTT-capable provider.',
        'Note the MQTT topic: ldms/iot/{orgId}/{assetId}/gps',
        'Publish JSON payloads to that topic on the LDMS MQTT broker (port 1883).',
      ],
      bestFor: 'Dedicated GPS trackers, fuel sensors, Traccar/Wialon MQTT forwarders.',
    },
    {
      id: 'forward',
      icon: 'sync_alt',
      title: 'Existing platform forward',
      tagline: 'Keep your current fleet platform — forward positions to LDMS.',
      steps: [
        'Configure your existing telematics platform (Traccar, Geotab, Wialon, CalAmp) to forward GPS events.',
        'Point the forwarder at the LDMS REST ingest endpoint or MQTT topic.',
        'Map your external device ID to the LDMS device record using the External device ID field.',
      ],
      bestFor: 'Organisations already running a GPS platform who want corridor visibility in LDMS.',
    },
  ];

  readonly providerBridges: ProviderBridge[] = [
    {
      name: 'Traccar',
      icon: 'gps_fixed',
      description: 'Open-source GPS server with HTTP/MQTT forwarding.',
      howTo: 'Add an LDMS forwarder in Traccar Events → set URL to the REST ingest endpoint and include the ingest key in the JSON body.',
    },
    {
      name: 'Geotab',
      icon: 'local_shipping',
      description: 'Enterprise telematics with MyGeotab add-ins and data feeds.',
      howTo: 'Use a MyGeotab add-in or third-party bridge to POST position updates to LDMS on each GPS event.',
    },
    {
      name: 'Wialon',
      icon: 'public',
      description: 'Fleet monitoring platform popular in emerging markets.',
      howTo: 'Configure a Wialon notification → HTTP request with latitude, longitude, speed, and your LDMS ingest key.',
    },
    {
      name: 'CalAmp',
      icon: 'router',
      description: 'Hardware OEM with LMU device families.',
      howTo: 'Use CalAmp PULS or a middleware layer to translate device messages into LDMS REST payloads.',
    },
    {
      name: 'Generic MQTT',
      icon: 'hub',
      description: 'Any MQTT-capable tracker publishing JSON.',
      howTo: 'Publish to your assigned topic ldms/iot/{orgId}/{assetId}/gps with lat/lng/speed fields.',
    },
    {
      name: 'Custom HTTP',
      icon: 'code',
      description: 'Your own firmware or middleware.',
      howTo: 'POST directly — see code samples. Accepts Content-Type: application/json, no Authorization header.',
    },
  ];

  readonly partnerDeliverables = [
    'SDK documentation or API reference (PDF, Swagger, or vendor portal link)',
    'Sample raw payloads or packet captures from a test device',
    'Sandbox credentials or a loaner device for our integration team',
    'Expected push frequency and field mapping (lat/lng/speed/heading/fuel)',
  ];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly http: HttpClient,
    private readonly snackBar: MatSnackBar,
    private readonly title: Title,
  ) {
    this.title.setTitle('Tracking Integration API | LX Platform');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get ingestUrl(): string {
    return `${environment.apiUrl}${TELEMETRY_ENDPOINT}`;
  }

  get requestPayload(): Record<string, unknown> {
    const body: Record<string, unknown> = {
      ingestKey: this.playground.ingestKey.trim(),
      latitude: this.playground.latitude,
      longitude: this.playground.longitude,
      speedKmh: this.playground.speedKmh,
      headingDeg: this.playground.headingDeg,
    };
    if (this.playground.fuelLevelPct != null) {
      body['fuelLevelPct'] = this.playground.fuelLevelPct;
    }
    return body;
  }

  get requestPayloadJson(): string {
    return JSON.stringify(this.requestPayload, null, 2);
  }

  get activeCodeSample(): string {
    return this.buildCodeSample(this.activeLanguage);
  }

  setSection(section: ApiSection): void {
    this.activeSection = section;
    if (section === 'playground') {
      setTimeout(() => document.getElementById('api-playground')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50);
    }
  }

  setLanguage(lang: CodeLanguage): void {
    this.activeLanguage = lang;
  }

  applyMockScenario(scenario: MockScenario): void {
    this.mockScenario = scenario;
    this.playground.ingestKey = DEMO_KEYS[scenario];
    this.mockMode = true;
  }

  copyText(token: string, value: string): void {
    void navigator.clipboard.writeText(value).then(() => {
      this.copiedToken = token;
      this.snackBar.open('Copied to clipboard', undefined, { duration: 1800 });
      setTimeout(() => {
        if (this.copiedToken === token) {
          this.copiedToken = null;
        }
      }, 2000);
    });
  }

  sendRequest(): void {
    const started = performance.now();
    const url = this.ingestUrl;
    const body = this.requestPayload;
    const bodyJson = JSON.stringify(body);

    this.sending = true;
    this.lastResult = null;

    if (this.mockMode) {
      of(this.buildMockResponse(body))
        .pipe(
          delay(480 + Math.random() * 320),
          finalize(() => {
            this.sending = false;
          }),
          takeUntil(this.destroy$),
        )
        .subscribe((mock) => {
          this.lastResult = {
            status: mock.statusCode,
            durationMs: Math.round(performance.now() - started),
            body: mock,
            requestUrl: url,
            requestBody: bodyJson,
          };
        });
      return;
    }

    this.http
      .post<unknown>(url, body, { observe: 'response' })
      .pipe(
        catchError((err) => {
          const status = err.status ?? 0;
          const errBody = err.error ?? { message: err.message ?? 'Request failed' };
          return of({ status, body: errBody, fromError: true as const });
        }),
        finalize(() => {
          this.sending = false;
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((res: HttpResponse<unknown> | PlaygroundHttpError) => {
        const status = res instanceof HttpResponse ? res.status : res.status;
        const responseBody = res instanceof HttpResponse ? res.body : res.body;
        this.lastResult = {
          status,
          durationMs: Math.round(performance.now() - started),
          body: responseBody,
          requestUrl: url,
          requestBody: bodyJson,
        };
      });
  }

  statusClass(code: number): string {
    if (code >= 200 && code < 300) {
      return 'fta-status--ok';
    }
    if (code === 401 || code === 403) {
      return 'fta-status--auth';
    }
    if (code >= 400) {
      return 'fta-status--err';
    }
    return 'fta-status--neutral';
  }

  private buildMockResponse(body: Record<string, unknown>): MockTelemetryResponse {
    const key = String(body['ingestKey'] ?? '');

    if (key === DEMO_KEYS.invalid_key || key.includes('invalid')) {
      return {
        statusCode: 401,
        success: false,
        message: 'Invalid ingest key',
        errorMessages: ['The ingest key was not found or has been revoked.'],
      };
    }

    if (key === DEMO_KEYS.no_trip) {
      return {
        statusCode: 202,
        success: true,
        message: 'Telemetry received; no active trip for this device',
      };
    }

    return {
      statusCode: 200,
      success: true,
      message: 'Telemetry ingested and trip updated',
      tripId: 10482,
      tripNumber: 'TRP-20260617-0042',
    };
  }

  private buildCodeSample(lang: CodeLanguage): string {
    const url = this.ingestUrl;
    const payload = this.requestPayloadJson;

    switch (lang) {
      case 'curl':
        return `curl -X POST '${url}' \\
  -H 'Content-Type: application/json' \\
  -d '${payload.replace(/\n/g, '').replace(/'/g, "'\\''")}'`;

      case 'javascript':
        return `const response = await fetch('${url}', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(${payload}),
});

const result = await response.json();
console.log(response.status, result);`;

      case 'python':
        return `import requests

payload = ${payload}

response = requests.post(
    '${url}',
    json=payload,
    timeout=10,
)
print(response.status_code, response.json())`;

      case 'java':
        return `HttpClient client = HttpClient.newHttpClient();
String json = """
${payload}
""";

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${url}"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(json))
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode() + " " + response.body());`;

      case 'csharp':
        return `using System.Net.Http;
using System.Text;

var url = "${url}";
var json = """
${payload}
""";

using var client = new HttpClient();
var content = new StringContent(json, Encoding.UTF8, "application/json");
var response = await client.PostAsync(url, content);
var body = await response.Content.ReadAsStringAsync();

Console.WriteLine($"{(int)response.StatusCode} {body}");`;

      case 'ruby':
        return `require 'json'
require 'net/http'
require 'uri'

url = URI('${url}')
payload = JSON.parse(<<~JSON)
${payload}
JSON

http = Net::HTTP.new(url.host, url.port)
http.use_ssl = url.scheme == 'https'

request = Net::HTTP::Post.new(url)
request['Content-Type'] = 'application/json'
request.body = payload.to_json

response = http.request(request)
puts "#{response.code} #{response.body}"`;

      case 'php':
        return `<?php
$payload = json_decode('${payload.replace(/'/g, "\\'")}', true);

$ch = curl_init('${url}');
curl_setopt_array($ch, [
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => ['Content-Type: application/json'],
    CURLOPT_POSTFIELDS => json_encode($payload),
    CURLOPT_RETURNTRANSFER => true,
]);
$response = curl_exec($ch);
$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

echo $status . ' ' . $response;`;

      default:
        return payload;
    }
  }
}
