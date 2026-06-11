/** Shipment statuses from ldms-shipment-management. */
export type ShipmentStatus =
  | 'PENDING'
  | 'ALLOCATED'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'CANCELLED';

export type TripStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'ARRIVED'
  | 'DELIVERED'
  | 'CANCELLED';

export type TripEventType =
  | 'DEPARTURE'
  | 'CHECKPOINT'
  | 'BREAK'
  | 'DELAY'
  | 'ARRIVAL'
  | 'DELIVERED'
  | 'INCIDENT'
  | 'OTHER';

export type TripTrackingTab = 'shipments' | 'trips';

/** View row displayed in the shipments table. */
export interface ShipmentRow {
  id: number;
  shipmentNumber: string;
  transferReference: string;
  inventoryTransferId?: number;
  fromWarehouse: string;
  toWarehouse: string;
  productName: string;
  quantity: number;
  unitOfMeasure: string;
  status: ShipmentStatus;
  statusLabel: string;
  statusTone: 'muted' | 'warn' | 'success' | 'danger' | 'info';
  driverName: string;
  vehicleRegistration: string;
  createdAtLabel: string;
  canAllocate: boolean;
  canStartTrip: boolean;
}

/** View row displayed in the active trips table. */
export interface TripRow {
  id: number;
  tripNumber: string;
  shipmentId: number;
  shipmentNumber: string;
  route: string;
  driverName: string;
  vehicleRegistration: string;
  status: TripStatus;
  statusLabel: string;
  statusTone: 'muted' | 'warn' | 'success' | 'danger' | 'info';
  lastEventLabel: string;
  lastEventAt: string;
  startedAtLabel: string;
  canTriggerArrival: boolean;
  canVerifyOtp: boolean;
}

/** A single timeline event entry from GET /trip/track/{id}. */
export interface TripTimelineEvent {
  id: number;
  eventType: TripEventType;
  eventTypeLabel: string;
  latitude?: number;
  longitude?: number;
  notes: string;
  recordedBy: string;
  recordedAtLabel: string;
}

/** Full trip detail from GET /trip/track/{id} or GET /trip/find-by-id/{id}. */
export interface TripDetail {
  id: number;
  tripNumber: string;
  shipmentId: number;
  shipmentNumber: string;
  driverName: string;
  vehicleRegistration: string;
  status: TripStatus;
  statusLabel: string;
  statusTone: 'muted' | 'warn' | 'success' | 'danger' | 'info';
  route: string;
  startedAtLabel: string;
  arrivedAtLabel?: string;
  deliveredAtLabel?: string;
  timeline: TripTimelineEvent[];
  canTriggerArrival: boolean;
  canVerifyOtp: boolean;
}

export interface TripWorkspaceMetrics {
  totalShipments: number;
  allocatedShipments: number;
  activeTrips: number;
  deliveredToday: number;
}

// ── Request payloads ─────────────────────────────────────────────────────────

/** POST /shipment/allocate */
export interface AllocateShipmentPayload {
  shipmentId: number;
  fleetDriverId: number;
  fleetAssetId: number;
}

/** POST /trip/start */
export interface StartTripPayload {
  shipmentId: number;
  fleetDriverId: number;
  fleetAssetId: number;
  startedByUserId: number;
}

/** POST /trip/trigger-arrival */
export interface TriggerArrivalPayload {
  tripId: number;
  driverUserId: number;
}

/** POST /trip/verify-delivery-otp */
export interface VerifyDeliveryOtpPayload {
  tripId: number;
  otp: string;
  receiverUserId: number;
}

/** POST /trip/record-event */
export interface RecordTripEventPayload {
  tripId: number;
  eventType: TripEventType;
  latitude?: number;
  longitude?: number;
  notes?: string;
}

/** POST /shipment/find-by-multiple-filters */
export interface ShipmentFilterPayload {
  organizationId?: number;
  status?: ShipmentStatus | '';
  inventoryTransferId?: number;
  search?: string;
}

/** POST /trip/find-by-multiple-filters */
export interface TripFilterPayload {
  organizationId?: number;
  status?: TripStatus | '';
  search?: string;
}
