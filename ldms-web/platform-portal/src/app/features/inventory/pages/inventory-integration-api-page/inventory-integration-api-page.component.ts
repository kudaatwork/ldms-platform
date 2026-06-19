import { HttpClient, HttpResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, of } from 'rxjs';
import { catchError, delay, finalize, takeUntil } from 'rxjs/operators';
import { environment } from '../../../../../environments/environment';
import {
  INVENTORY_API_BASE,
  INVENTORY_ENDPOINT_GROUPS,
  INVENTORY_INTEGRATION_ENDPOINTS,
  INVENTORY_SYNC_RESPONSE_CODES,
  InvApiField,
} from './inventory-integration-api.catalog';
import {
  INVENTORY_INTEGRATION_WORKFLOWS,
  INVENTORY_MODULE_COMPONENTS,
  INVENTORY_SYNC_PHASES,
  InventoryModuleComponent,
} from './inventory-integration-components.catalog';

export type { InvApiField };
export type InvApiCodeLanguage = 'curl' | 'javascript' | 'python' | 'java' | 'csharp' | 'ruby' | 'php';
export type InvApiSection = 'overview' | 'components' | 'contract' | 'samples' | 'connect' | 'webhooks' | 'playground';
export type InvApiMockScenario = 'dispatch_ok' | 'partial_stock' | 'invalid_key';
export type InvApiMode = 'inventory' | 'crossdock';
export type InvSyncMockScenario = 'product_ok' | 'stock_ok' | 'transfer_ok' | 'invalid_key';

export interface IntegrationPath {
  id: string;
  icon: string;
  title: string;
  tagline: string;
  steps: string[];
  bestFor: string;
}

export interface ErpBridge {
  name: string;
  icon: string;
  description: string;
  howTo: string;
}

export interface InvApiEndpoint {
  method: string;
  path: string;
  summary: string;
  bodyExample?: string;
}

export interface InvApiPlaygroundResult {
  status: number;
  durationMs: number;
  body: unknown;
  requestUrl: string;
  requestBody: string;
}

interface MockDispatchResponse {
  statusCode: number;
  success: boolean;
  message: string;
  errorMessages?: string[];
  dispatchId?: string;
  grvCallbackRegistered?: boolean;
}

interface PlaygroundHttpError {
  status: number;
  body: unknown;
  fromError: true;
}

const DISPATCH_INGEST_ENDPOINT = '/ldms-inventory-management/v1/system/integration/dispatch-ingest';
const API_BASE = INVENTORY_API_BASE;

const DEMO_KEYS: Record<InvApiMockScenario, string> = {
  dispatch_ok: 'demo-inv-key-ok',
  partial_stock: 'demo-inv-key-partial',
  invalid_key: 'demo-inv-key-invalid',
};

const SYNC_DEMO_KEYS: Record<InvSyncMockScenario, string> = {
  product_ok: 'demo-inv-sync-product',
  stock_ok: 'demo-inv-sync-stock',
  transfer_ok: 'demo-inv-sync-transfer',
  invalid_key: 'demo-inv-key-invalid',
};

@Component({
  selector: 'app-inventory-integration-api-page',
  templateUrl: './inventory-integration-api-page.component.html',
  styleUrl: './inventory-integration-api-page.component.scss',
  standalone: false,
})
export class InventoryIntegrationApiPageComponent implements OnInit, OnDestroy {
  apiMode: InvApiMode = 'inventory';
  activeSection: InvApiSection = 'overview';
  activeLanguage: InvApiCodeLanguage = 'curl';
  activeContractGroupId = 'master';
  activeEndpointId = 'product-create';
  mockMode = true;
  mockScenario: InvApiMockScenario = 'dispatch_ok';
  syncMockScenario: InvSyncMockScenario = 'product_ok';
  sending = false;
  copiedToken: string | null = null;

  playground = {
    apiKey: DEMO_KEYS.dispatch_ok,
    dispatchRef: 'DISP-20260617-001',
    customerRef: 'CUST-001',
    warehouseId: 1,
    items: JSON.stringify(
      [{ sku: 'PRD-001', quantity: 50, unitOfMeasure: 'KG' }],
      null,
      2,
    ),
  };

  syncPlayground = {
    apiKey: SYNC_DEMO_KEYS.product_ok,
    sku: 'PRD-001',
    productName: 'Portland Cement 50kg',
    categoryCode: 'BUILD',
    warehouseId: 1,
    quantityOnHand: 500,
    unitOfMeasure: 'BAG',
  };

  lastResult: InvApiPlaygroundResult | null = null;

  readonly endpointGroups = INVENTORY_ENDPOINT_GROUPS;
  readonly inventoryIntegrationEndpoints = INVENTORY_INTEGRATION_ENDPOINTS;
  readonly syncResponseCodes = INVENTORY_SYNC_RESPONSE_CODES;

  readonly integrationPaths: IntegrationPath[] = [
    {
      id: 'rest',
      icon: 'http',
      title: 'REST push from ERP',
      tagline: 'POST JSON whenever master data or stock changes in your system of record.',
      steps: [
        'Generate an integration API key on Inventory integration setup.',
        'Push categories and products first — use SKU and category codes as idempotency keys.',
        'Sync stock levels per warehouse, then transfers and order documents as they are approved.',
      ],
      bestFor: 'SAP, Sage, Dynamics, Odoo, and custom middleware with outbound webhooks.',
    },
    {
      id: 'bulk',
      icon: 'upload_file',
      title: 'Bulk CSV / initial load',
      tagline: 'Seed opening balances and large catalog updates in one batch.',
      steps: [
        'Use warehouse and product endpoints to register locations and SKUs.',
        'Call initial-stock bulk to load opening quantities per warehouse.',
        'Switch to incremental REST pushes for day-to-day deltas.',
      ],
      bestFor: 'Go-live cutover weekends and periodic full-catalog refreshes.',
    },
    {
      id: 'scheduled',
      icon: 'schedule',
      title: 'Scheduled delta sync',
      tagline: 'Cron or ETL job polls ERP and pushes only what changed.',
      steps: [
        'Run a nightly job for stock on-hand and WAC per warehouse.',
        'Push transfer status transitions when TMS events fire.',
        'Subscribe to LDMS outbound webhooks for GRV and stock corrections.',
      ],
      bestFor: 'High-volume distributors with mature ERP scheduling.',
    },
  ];

  readonly erpBridges: ErpBridge[] = [
    {
      name: 'SAP / S/4HANA',
      icon: 'business',
      description: 'IDoc or CPI middleware maps MATMAS and stock segments to LDMS product and inventory-item endpoints.',
      howTo: 'Forward approved material master and MARD stock snapshots every 15 minutes.',
    },
    {
      name: 'Sage / Pastel',
      icon: 'store',
      description: 'Inventory journal and item master exports feed the product and stock-adjustment APIs.',
      howTo: 'Use a lightweight forwarder service on the same LAN as Pastel.',
    },
    {
      name: 'Microsoft Dynamics',
      icon: 'cloud_sync',
      description: 'Dataverse or Logic Apps push item and on-hand entities into LDMS.',
      howTo: 'Trigger on item.created and inventory.adjustment events.',
    },
    {
      name: 'Custom ERP',
      icon: 'integration_instructions',
      description: 'Any system that can POST JSON over HTTPS can integrate — no proprietary SDK required.',
      howTo: 'Start with product-create and inventory-item-create, then expand to transfers and orders.',
    },
  ];

  readonly inventoryEndpoints: InvApiEndpoint[] = INVENTORY_INTEGRATION_ENDPOINTS.map((ep) => ({
    method: ep.method,
    path: `${API_BASE}${ep.pathSuffix}`,
    summary: ep.summary,
    bodyExample: ep.fields
      .filter((f) => f.name !== 'apiKey')
      .slice(0, 3)
      .map((f) => `"${f.name}": ${f.example.includes('[') ? f.example : `"${f.example}"`}`)
      .join(', '),
  }));

  readonly moduleComponents = INVENTORY_MODULE_COMPONENTS;
  readonly syncPhases = INVENTORY_SYNC_PHASES;
  readonly integrationWorkflows = INVENTORY_INTEGRATION_WORKFLOWS;

  readonly allSections: { id: InvApiSection; label: string; icon: string }[] = [
    { id: 'overview', label: 'Overview', icon: 'rocket_launch' },
    { id: 'components', label: 'Components', icon: 'view_module' },
    { id: 'contract', label: 'REST contract', icon: 'description' },
    { id: 'samples', label: 'Code samples', icon: 'code' },
    { id: 'connect', label: 'Connect ERP', icon: 'hub' },
    { id: 'webhooks', label: 'Webhooks', icon: 'webhook' },
    { id: 'playground', label: 'Try it', icon: 'play_circle' },
  ];

  readonly crossdockSections: { id: InvApiSection; label: string; icon: string }[] = [
    { id: 'overview', label: 'Overview', icon: 'rocket_launch' },
    { id: 'contract', label: 'REST contract', icon: 'description' },
    { id: 'samples', label: 'Code samples', icon: 'code' },
    { id: 'webhooks', label: 'Webhooks', icon: 'webhook' },
    { id: 'playground', label: 'Try it', icon: 'play_circle' },
  ];

  readonly languages: { id: InvApiCodeLanguage; label: string }[] = [
    { id: 'curl', label: 'cURL' },
    { id: 'javascript', label: 'JavaScript' },
    { id: 'python', label: 'Python' },
    { id: 'java', label: 'Java' },
    { id: 'csharp', label: 'C#' },
    { id: 'ruby', label: 'Ruby' },
    { id: 'php', label: 'PHP' },
  ];

  readonly mockScenarios: { id: InvApiMockScenario; label: string; hint: string }[] = [
    { id: 'dispatch_ok', label: 'Dispatch accepted (200)', hint: 'All items matched — dispatch registered and GRV callback queued' },
    { id: 'partial_stock', label: 'Partial stock (206)', hint: 'Some items could not be matched — partial fulfillment' },
    { id: 'invalid_key', label: 'Invalid key (401)', hint: 'API key not recognised' },
  ];

  readonly dispatchIngestFields: InvApiField[] = [
    {
      name: 'apiKey',
      type: 'string',
      required: true,
      description: 'Integration API key from Inventory integration setup. Identifies your ERP or WMS — no JWT needed.',
      example: 'inv-live-a1b2c3…',
    },
    {
      name: 'dispatchRef',
      type: 'string',
      required: true,
      description: 'Your external dispatch reference. Used as idempotency key — duplicate submissions with the same ref are ignored.',
      example: 'DISP-20260617-001',
    },
    {
      name: 'customerRef',
      type: 'string',
      required: false,
      description: 'Your customer identifier. Matched to a trading partner or platform customer.',
      example: 'CUST-001',
    },
    {
      name: 'warehouseId',
      type: 'integer',
      required: true,
      description: 'LDMS warehouse ID from which items are dispatched.',
      example: '1',
    },
    {
      name: 'items',
      type: 'array',
      required: true,
      description: 'Line items — each with sku, quantity, and unitOfMeasure.',
      example: '[{ "sku": "PRD-001", "quantity": 50, "unitOfMeasure": "KG" }]',
    },
    {
      name: 'scheduledAt',
      type: 'datetime',
      required: false,
      description: 'ISO-8601 scheduled dispatch timestamp. Defaults to now.',
      example: '2026-06-17T08:00:00',
    },
  ];

  readonly responseCodes = [
    { code: 200, label: 'OK', detail: 'Dispatch ingested. All items matched and a GRV callback has been queued.' },
    { code: 206, label: 'Partial', detail: 'Dispatch accepted with partial line items. Some SKUs were not matched.' },
    { code: 400, label: 'Bad request', detail: 'Missing required fields or warehouseId not found.' },
    { code: 401, label: 'Unauthorized', detail: 'API key not found or revoked.' },
    { code: 409, label: 'Conflict', detail: 'Dispatch with this dispatchRef already exists (idempotency).' },
    { code: 503, label: 'Unavailable', detail: 'Inventory service temporarily unreachable.' },
  ];

  readonly syncMockScenarios: { id: InvSyncMockScenario; label: string; hint: string; endpointId: string }[] = [
    { id: 'product_ok', label: 'Product upsert (200)', hint: 'SKU accepted and product master updated', endpointId: 'product-create' },
    { id: 'stock_ok', label: 'Stock level sync (200)', hint: 'Warehouse stock level updated from ERP', endpointId: 'inventory-item-create' },
    { id: 'transfer_ok', label: 'Transfer create (200)', hint: 'Warehouse transfer registered with route stops', endpointId: 'transfer-create' },
    { id: 'invalid_key', label: 'Invalid key (401)', hint: 'Integration API key not recognised', endpointId: 'product-create' },
  ];

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly http: HttpClient,
    private readonly snackBar: MatSnackBar,
    private readonly title: Title,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
  ) {
    this.title.setTitle('Inventory Integration API | LX Platform');
  }

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const mode = params.get('mode');
      if (mode === 'crossdock') {
        this.apiMode = 'crossdock';
        this.title.setTitle('Cross-dock Integration API | LX Platform');
      } else {
        this.apiMode = 'inventory';
        this.title.setTitle('Inventory Management Integration API | LX Platform');
      }
    });

    this.route.fragment.pipe(takeUntil(this.destroy$)).subscribe((fragment) => {
      if (fragment === 'section-components' && this.apiMode === 'inventory') {
        this.activeSection = 'components';
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get sections(): { id: InvApiSection; label: string; icon: string }[] {
    return this.apiMode === 'inventory' ? this.allSections : this.crossdockSections;
  }

  get activeEndpoint() {
    return (
      this.inventoryIntegrationEndpoints.find((ep) => ep.id === this.activeEndpointId) ??
      this.inventoryIntegrationEndpoints[0]
    );
  }

  get endpointsInActiveGroup() {
    return this.inventoryIntegrationEndpoints.filter((ep) => ep.groupId === this.activeContractGroupId);
  }

  get activeEndpointUrl(): string {
    return `${environment.apiUrl}${API_BASE}${this.activeEndpoint.pathSuffix}`;
  }

  get inventoryEndpointCount(): number {
    return this.inventoryIntegrationEndpoints.length;
  }

  get dispatchIngestUrl(): string {
    return `${environment.apiUrl}${DISPATCH_INGEST_ENDPOINT}`;
  }

  selectContractGroup(groupId: string): void {
    this.activeContractGroupId = groupId;
    const first = this.inventoryIntegrationEndpoints.find((ep) => ep.groupId === groupId);
    if (first) {
      this.activeEndpointId = first.id;
    }
  }

  selectEndpoint(endpointId: string): void {
    this.activeEndpointId = endpointId;
    const ep = this.inventoryIntegrationEndpoints.find((e) => e.id === endpointId);
    if (ep) {
      this.activeContractGroupId = ep.groupId;
    }
  }

  openComponentContract(component: InventoryModuleComponent): void {
    const endpointId = this.resolveEndpointIdForComponent(component);
    if (endpointId) {
      this.selectEndpoint(endpointId);
    }
    this.setSection('contract');
  }

  private resolveEndpointIdForComponent(component: InventoryModuleComponent): string | null {
    const pathMap: Record<string, string> = {
      'POST /product-category/create': 'product-category-create',
      'POST /product-sub-category/create': 'product-sub-category-create',
      'POST /product/create': 'product-create',
      'POST /warehouse-locations': 'warehouse-create',
      'POST /inventory-item/create': 'inventory-item-create',
      'POST /inventory-item/initial-stock': 'initial-stock',
      'POST /stock-adjustment/create': 'stock-adjustment-create',
      'POST /inventory-transfer/create': 'transfer-create',
      'POST /inventory-transfer/start-transit': 'transfer-start',
      'POST /inventory-transfer/complete': 'transfer-complete',
      'POST /purchase-requisition/create': 'requisition-create',
      'POST /purchase-order/create': 'purchase-order-create',
      'POST /sales-order/create': 'sales-order-create',
      'POST /sales-reservation/create': 'sales-reservation-create',
    };
    const first = component.pushEndpoints[0];
    return first ? pathMap[first] ?? null : null;
  }

  componentsForPhase(phaseOrder: number): InventoryModuleComponent[] {
    return this.moduleComponents.filter((c) => c.phase === phaseOrder);
  }

  get productSyncUrl(): string {
    return `${environment.apiUrl}${API_BASE}/product/create`;
  }

  get stockSyncUrl(): string {
    return `${environment.apiUrl}${API_BASE}/inventory-item/create`;
  }

  get heroTitle(): string {
    return this.apiMode === 'inventory' ? 'Inventory Management Integration API' : 'Cross-dock Integration API';
  }

  get heroLead(): string {
    return this.apiMode === 'inventory'
      ? `Connect your ERP or WMS with ${this.inventoryEndpointCount} REST endpoints. Push master data, stock levels, transfers, and orders — LDMS runs the full inventory module on synced data.`
      : 'Push dispatch and movement events from your WMS. LDMS tracks transit — not stock on hand.';
  }

  setApiMode(mode: InvApiMode): void {
    this.apiMode = mode;
    this.activeSection = 'overview';
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { mode: mode === 'crossdock' ? 'crossdock' : 'inventory' },
    });
  }

  applySyncMockScenario(scenario: InvSyncMockScenario): void {
    this.syncMockScenario = scenario;
    this.syncPlayground.apiKey = SYNC_DEMO_KEYS[scenario];
    const match = this.syncMockScenarios.find((s) => s.id === scenario);
    if (match) {
      this.selectEndpoint(match.endpointId);
    }
    this.mockMode = true;
  }

  get syncRequestPayload(): Record<string, unknown> {
    if (this.syncMockScenario === 'stock_ok') {
      return {
        apiKey: this.syncPlayground.apiKey.trim(),
        productSku: this.syncPlayground.sku.trim(),
        warehouseLocationId: Number(this.syncPlayground.warehouseId),
        quantityOnHand: Number(this.syncPlayground.quantityOnHand),
        unitOfMeasure: this.syncPlayground.unitOfMeasure,
      };
    }
    if (this.syncMockScenario === 'transfer_ok') {
      return {
        apiKey: this.syncPlayground.apiKey.trim(),
        productId: 42,
        fromLocationId: 1,
        toLocationId: Number(this.syncPlayground.warehouseId) || 3,
        quantity: Number(this.syncPlayground.quantityOnHand) || 100,
        createdByUserId: 1,
        crossBorder: false,
        routeStops: [
          { stopSequence: 0, stopType: 'ORIGIN', warehouseLocationId: 1 },
          { stopSequence: 1, stopType: 'EN_ROUTE_DEPOT', warehouseLocationId: 2 },
          { stopSequence: 2, stopType: 'DESTINATION', warehouseLocationId: Number(this.syncPlayground.warehouseId) || 3 },
        ],
      };
    }
    return {
      apiKey: this.syncPlayground.apiKey.trim(),
      sku: this.syncPlayground.sku.trim(),
      name: this.syncPlayground.productName.trim(),
      categoryCode: this.syncPlayground.categoryCode.trim(),
      unitOfMeasure: this.syncPlayground.unitOfMeasure,
    };
  }

  get syncRequestPayloadJson(): string {
    return JSON.stringify(this.syncRequestPayload, null, 2);
  }

  sendSyncRequest(): void {
    const started = performance.now();
    const url = this.syncMockScenario === 'stock_ok'
      ? this.stockSyncUrl
      : this.syncMockScenario === 'transfer_ok'
        ? `${environment.apiUrl}${API_BASE}/inventory-transfer/create`
        : this.productSyncUrl;
    const bodyJson = this.syncRequestPayloadJson;
    this.sending = true;
    this.lastResult = null;

    of(this.buildSyncMockResponse(this.syncPlayground.apiKey))
      .pipe(
        delay(380 + Math.random() * 220),
        finalize(() => (this.sending = false)),
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
  }

  private buildSyncMockResponse(apiKey: string): {
    statusCode: number;
    success: boolean;
    message: string;
    productId?: number;
    inventoryItemId?: number;
    transferId?: number;
    errorMessages?: string[];
  } {
    if (apiKey === SYNC_DEMO_KEYS.invalid_key || apiKey.includes('invalid')) {
      return {
        statusCode: 401,
        success: false,
        message: 'Invalid integration API key',
        errorMessages: ['The integration API key was not found or has been revoked.'],
      };
    }
    if (this.syncMockScenario === 'stock_ok') {
      return {
        statusCode: 200,
        success: true,
        message: 'Stock level synchronised',
        inventoryItemId: 8842,
      };
    }
    if (this.syncMockScenario === 'transfer_ok') {
      return {
        statusCode: 200,
        success: true,
        message: 'Transfer created',
        transferId: 310,
      };
    }
    return {
      statusCode: 200,
      success: true,
      message: 'Product master upserted',
      productId: 1204,
    };
  }

  get requestPayload(): Record<string, unknown> {
    let parsedItems: unknown[] = [];
    try {
      parsedItems = JSON.parse(this.playground.items) as unknown[];
    } catch {
      parsedItems = [];
    }
    return {
      apiKey: this.playground.apiKey.trim(),
      dispatchRef: this.playground.dispatchRef.trim(),
      customerRef: this.playground.customerRef.trim() || undefined,
      warehouseId: Number(this.playground.warehouseId),
      items: parsedItems,
    };
  }

  get requestPayloadJson(): string {
    return JSON.stringify(this.requestPayload, null, 2);
  }

  get activeCodeSample(): string {
    return this.buildCodeSample(this.activeLanguage);
  }

  get codeSampleLabel(): string {
    if (this.apiMode === 'inventory') {
      return `${this.activeEndpoint.method} ${this.activeEndpoint.title}`;
    }
    return 'POST dispatch ingest';
  }

  get activeSampleUrl(): string {
    return this.apiMode === 'inventory' ? this.activeEndpointUrl : this.dispatchIngestUrl;
  }

  get activeSamplePayloadJson(): string {
    return this.apiMode === 'inventory' ? this.contractSamplePayloadJson : this.requestPayloadJson;
  }

  get contractSamplePayloadJson(): string {
    const payload: Record<string, unknown> = {};
    for (const field of this.activeEndpoint.fields) {
      if (field.name === 'apiKey') {
        payload[field.name] = this.syncPlayground.apiKey.trim();
        continue;
      }
      if (field.type === 'integer' || field.type === 'decimal') {
        const parsed = Number(field.example);
        payload[field.name] = Number.isFinite(parsed) ? parsed : field.example;
        continue;
      }
      if (field.type === 'array') {
        try {
          payload[field.name] = JSON.parse(field.example);
        } catch {
          payload[field.name] = field.example;
        }
        continue;
      }
      payload[field.name] = field.example;
    }
    return JSON.stringify(payload, null, 2);
  }

  setSection(section: InvApiSection): void {
    this.activeSection = section;
    if (section === 'playground') {
      setTimeout(
        () => document.getElementById('inv-api-playground')?.scrollIntoView({ behavior: 'smooth', block: 'start' }),
        50,
      );
    }
    if (section === 'components') {
      setTimeout(() => document.getElementById('section-components')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50);
    }
  }

  setLanguage(lang: InvApiCodeLanguage): void {
    this.activeLanguage = lang;
  }

  applyMockScenario(scenario: InvApiMockScenario): void {
    this.mockScenario = scenario;
    this.playground.apiKey = DEMO_KEYS[scenario];
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
    const url = this.dispatchIngestUrl;
    const body = this.requestPayload;
    const bodyJson = JSON.stringify(body);

    this.sending = true;
    this.lastResult = null;

    if (this.mockMode) {
      of(this.buildMockResponse(body))
        .pipe(
          delay(420 + Math.random() * 280),
          finalize(() => (this.sending = false)),
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
        finalize(() => (this.sending = false)),
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
    if (code >= 200 && code < 300) return 'iia-status--ok';
    if (code === 401 || code === 403) return 'iia-status--auth';
    if (code >= 400) return 'iia-status--err';
    return 'iia-status--neutral';
  }

  private buildMockResponse(body: Record<string, unknown>): MockDispatchResponse {
    const key = String(body['apiKey'] ?? '');

    if (key === DEMO_KEYS.invalid_key || key.includes('invalid')) {
      return {
        statusCode: 401,
        success: false,
        message: 'Invalid API key',
        errorMessages: ['The integration API key was not found or has been revoked.'],
      };
    }

    if (key === DEMO_KEYS.partial_stock) {
      return {
        statusCode: 206,
        success: true,
        message: 'Dispatch partially accepted — 1 of 1 items matched',
        dispatchId: `DISP-INT-${Date.now()}`,
        grvCallbackRegistered: false,
      };
    }

    return {
      statusCode: 200,
      success: true,
      message: 'Dispatch ingested and GRV callback queued',
      dispatchId: `DISP-INT-${Date.now()}`,
      grvCallbackRegistered: true,
    };
  }

  private buildCodeSample(lang: InvApiCodeLanguage): string {
    const url = this.activeSampleUrl;
    const payload = this.activeSamplePayloadJson;

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

      default:
        return payload;
    }
  }
}
