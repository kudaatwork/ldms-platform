/** Corridor waypoint for live map rendering (trip-tracking live snapshot). */
export interface TripRouteWaypoint {
  label: string;
  latitude: number;
  longitude: number;
  type: string;
  speedKmh?: number;
  recordedAt?: string;
}

/** GET /ldms-trip-tracking/v1/backoffice/trip-live/by-shipment/{id} */
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
  lastTimingTickMs?: number;
}

export interface ShipmentLiveView {
  organizationId: number;
  organizationName: string;
  snapshot: TripLiveSnapshot;
}
