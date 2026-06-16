export type FundRequestType = 'FUEL_TOP_UP' | 'FUNDS' | 'MECHANIC';
export type FundRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface OperationalFundRequestRow {
  id: number;
  requestNumber: string;
  tripId: number;
  requestType: FundRequestType;
  requestTypeLabel: string;
  status: FundRequestStatus;
  statusLabel: string;
  litersRequested?: number;
  amountRequested?: number;
  currencyCode: string;
  driverNotes: string;
  createdAtLabel: string;
  canApprove: boolean;
  canReject: boolean;
}

export interface CreateFundRequestPayload {
  tripId: number;
  fleetDriverId?: number;
  fleetAssetId?: number;
  requestType: FundRequestType;
  litersRequested?: number;
  amountRequested?: number;
  currencyCode?: string;
  latitude?: number;
  longitude?: number;
  driverNotes?: string;
}

export interface FuelTelemetryLogRow {
  id: number;
  source: string;
  sourceLabel: string;
  readingType: string;
  readingTypeLabel: string;
  fuelLevelPct?: number;
  fuelLiters?: number;
  consumedLiters?: number;
  distanceDeltaKm?: number;
  recordedAtLabel: string;
}
