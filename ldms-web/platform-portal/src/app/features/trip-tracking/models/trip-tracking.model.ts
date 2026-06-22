/** Shipment statuses from ldms-shipment-management. */
export type ShipmentStatus =
  | 'PENDING'
  | 'PENDING_FLEET'
  | 'ALLOCATED'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'CANCELLED';

export type TripStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'IN_TRANSIT'
  | 'AT_BORDER_HOLD'
  | 'ROADSIDE_HOLD'
  | 'ARRIVED'
  | 'COUNTING_STOCK'
  | 'COUNT_COMPLETE'
  | 'OTP_PENDING'
  | 'RETURN_IN_TRANSIT'
  | 'RETURNED'
  | 'DELIVERED'
  | 'CANCELLED';

export type TripEventType =
  | 'DEPARTURE'
  | 'DEPARTED'
  | 'CHECKPOINT'
  | 'BREAK'
  | 'DELAY'
  | 'ARRIVED_AT_BORDER'
  | 'BORDER_CLEARED'
  | 'ROADSIDE_FUEL_STOP'
  | 'ROADSIDE_MECHANIC_STOP'
  | 'ROADSIDE_RESUMED'
  | 'DRIVER_BREAK'
  | 'DRIVER_RESUMED'
  | 'ARRIVAL'
  | 'ARRIVED'
  | 'DELIVERED'
  | 'INCIDENT'
  | 'OTHER'
  | 'NOTE';

export type TripTrackingTab = 'shipments' | 'trips';

/** View row displayed in the shipments table. */
export interface ShipmentRow {
  id: number;
  shipmentNumber: string;
  organizationId?: number;
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
  fleetDriverId?: number;
  fleetAssetId?: number;
  transportCompanyOrganizationId?: number;
  transportCompanyName?: string;
  driverName: string;
  vehicleRegistration: string;
  createdAtLabel: string;
  canAssignTransport: boolean;
  canAllocate: boolean;
  canStartTrip: boolean;
  crossBorder?: boolean;
  tripId?: number;
}

/** View row displayed in the active trips table. */
export interface TripRow {
  id: number;
  tripNumber: string;
  shipmentId: number;
  shipmentNumber: string;
  route: string;
  productName: string;
  productCode?: string;
  quantity?: number;
  cargoLabel: string;
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
  canLiveTrack?: boolean;
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
  /** ISO timestamp for duration maths (optional). */
  recordedAtIso?: string;
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
  canLiveTrack?: boolean;
}

export interface TripWorkspaceMetrics {
  totalShipments: number;
  allocatedShipments: number;
  activeTrips: number;
  deliveredToday: number;
}

/** One live corridor load rendered on the dashboard fleet map. */
export interface TripLiveMapTrack {
  tripId: number;
  shipmentNumber: string;
  statusLabel?: string;
  routeLabel?: string;
}

// ── Request payloads ─────────────────────────────────────────────────────────

/** POST /shipment/assign-transport-company */
export interface AssignTransportCompanyPayload {
  shipmentId: number;
  transportCompanyOrganizationId: number;
}

/** POST /shipment/allocate */
export interface AllocateShipmentPayload {
  shipmentId: number;
  fleetDriverId: number;
  fleetAssetId: number;
}

/** POST /trip/start */
export interface StartTripPayload {
  shipmentId: number;
  inventoryTransferId?: number;
  salesOrderId?: number;
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
  /** When true, returns all non-terminal active corridor trips for the organisation. */
  activeOnly?: boolean;
  search?: string;
}

/** Corridor waypoint for live map rendering. */
export interface TripRouteWaypoint {
  label: string;
  latitude: number;
  longitude: number;
  type: string;
  speedKmh?: number;
  recordedAt?: string;
}

/** GET /trip-live/snapshot/{id} */
export interface TripLiveSnapshot {
  tripId: number;
  tripNumber: string;
  status: string;
  shipmentId?: number;
  shipmentNumber?: string;
  productName?: string;
  productCode?: string;
  quantity?: number;
  fromWarehouseName: string;
  toWarehouseName: string;
  vehicleRegistration?: string;
  driverName?: string;
  fleetAssetId?: number;
  latitude?: number;
  longitude?: number;
  speedKmh: number;
  maxSpeedKmh?: number;
  speedLimitExceeded?: boolean;
  headingDeg: number;
  overallProgressPct: number;
  distanceTravelledKm?: number;
  fuelLevelPct?: number;
  fuelRemainingLiters?: number;
  simulationActive: boolean;
  simulationPaused?: boolean;
  moving: boolean;
  onBreak?: boolean;
  routeWaypoints: TripRouteWaypoint[];
  trail?: TripRouteWaypoint[];
  journeyStartedAt?: string;
  totalElapsedSeconds?: number;
  transitSeconds?: number;
  waitingSeconds?: number;
  idleSeconds?: number;
  journeyPhase?: 'TRANSIT' | 'WAITING' | 'IDLE' | 'COMPLETED';
  estimatedArrivalSeconds?: number;
  currentSegmentIndex?: number;
  segmentProgressPct?: number;
  completedWaypointCount?: number;
  totalWaypointCount?: number;
  awaitingArrivalConfirmation?: boolean;
  returnJourneyActive?: boolean;
  deliveryPhaseLabel?: string;
  /** True when the truck is within the geofence radius of the destination. */
  nearDestination?: boolean;
  /** When true, the UI should show the "Have you arrived?" floating prompt. */
  arrivalPromptVisible?: boolean;
  /** Distance in km remaining to the destination. */
  distanceToDestinationKm?: number;
  /** Client-side live clock anchor (set when snapshot is received). */
  lastTimingTickMs?: number;
}

/** GET /fuel-session/live/{tripId} */
export interface FuelLiveSnapshot {
  tripId: number;
  fleetDriverId?: number;
  fleetAssetId?: number;
  fuelLevelPct: number;
  fuelRemainingLiters: number;
  tankCapacityLiters: number;
  distanceTravelledKm: number;
  consumptionRateLPer100Km: number;
  moving: boolean;
  status: string;
}
