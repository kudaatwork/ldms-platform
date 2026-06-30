/** OTP delivery channel options for delivery verification. */
export type OtpChannel = 'SMS' | 'WHATSAPP' | 'EMAIL';

/** Actor role used in stock-counting and returns API calls. */
export type DeliveryActorRole = 'DRIVER' | 'CUSTOMER' | 'RECEIVER' | 'DEPOT_CLERK';

/** The delivery workflow phases (no separate DELIVERY_NOTES — notes go into verifyOtp). */
export type DeliveryWorkflowPhase =
  | 'ARRIVAL'
  | 'STOCK_COUNTING'
  | 'FINISHED_COUNTING'
  | 'SEND_OTP'
  | 'OTP_VERIFICATION'
  | 'START_RETURN'
  | 'RETURNS'
  | 'CONFIRM_RETURN'
  | 'COMPLETE';

/** Current state of a delivery workflow for a trip. */
export interface DeliveryWorkflowState {
  tripId: number;
  currentPhase: DeliveryWorkflowPhase;
  driverConfirmedCounting: boolean;
  customerConfirmedCounting: boolean;
  driverConfirmedFinished: boolean;
  customerConfirmedFinished: boolean;
  otpChannel: OtpChannel;
  otpRecipient: string;
  otpSent: boolean;
  otpVerified: boolean;
  deliveryNotes: string;
  hasReturns: boolean;
  returnLines: DeliveryReturnLine[];
}

/** A single return line item (goods being sent back). */
export interface DeliveryReturnLine {
  productName: string;
  quantity: number;
  reason: string;
}

// ── Backend response types ────────────────────────────────────────────────────

/**
 * Workflow DTO embedded in TripDeliveryWorkflowResponse.
 * Maps directly to backend TripDeliveryWorkflowDto.
 */
export interface TripDeliveryWorkflowDto {
  id?: number;
  tripId?: number;
  driverCountingStartedAt?: string;
  driverCountingFinishedAt?: string;
  customerCountingStartedAt?: string;
  customerCountingFinishedAt?: string;
  otpChannel?: OtpChannel;
  otpRecipient?: string;
  deliveryNotes?: string;
  returnLines?: DeliveryReturnLine[];
  returnInitiatedAt?: string;
  returnCompletedAt?: string;
}

/**
 * Backend response from GET /trip-delivery/{tripId}.
 * Contains both the workflow DTO and the trip DTO.
 */
export interface TripDeliveryWorkflowResponse {
  workflowDto?: TripDeliveryWorkflowDto;
  tripDto?: {
    id: number;
    status: string;
    tripNumber?: string;
  };
  /** Convenience: may also be returned at root level for backward compat. */
  tripId?: number;
  currentPhase?: DeliveryWorkflowPhase;
  driverConfirmedCounting?: boolean;
  customerConfirmedCounting?: boolean;
  driverConfirmedFinished?: boolean;
  customerConfirmedFinished?: boolean;
  otpSent?: boolean;
  otpVerified?: boolean;
  deliveryNotes?: string;
  returnLines?: DeliveryReturnLine[];
}

/**
 * Map the backend TripDeliveryWorkflowResponse to a local DeliveryWorkflowPhase.
 * Uses tripDto.status as the primary signal, with workflow timestamps for sub-steps.
 */
export function resolveWorkflowPhase(res: TripDeliveryWorkflowResponse): DeliveryWorkflowPhase {
  const wf = res.workflowDto;
  const tripStatus = (res.tripDto?.status ?? '').toUpperCase();

  if (tripStatus === 'RETURNED') {
    return 'COMPLETE';
  }
  if (tripStatus === 'RETURN_IN_TRANSIT') {
    return wf?.returnLines?.length ? 'CONFIRM_RETURN' : 'RETURNS';
  }
  if (tripStatus === 'DELIVERED') {
    return 'START_RETURN';
  }
  if (tripStatus === 'OTP_PENDING') {
    return 'OTP_VERIFICATION';
  }
  if (tripStatus === 'COUNT_COMPLETE') {
    return 'SEND_OTP';
  }
  if (tripStatus === 'COUNTING_STOCK') {
    if (wf?.driverCountingFinishedAt && wf?.customerCountingFinishedAt) {
      return 'SEND_OTP';
    }
    if (wf?.driverCountingStartedAt && wf?.customerCountingStartedAt) {
      return 'FINISHED_COUNTING';
    }
    return 'STOCK_COUNTING';
  }
  if (tripStatus === 'ARRIVED') {
    return 'STOCK_COUNTING';
  }
  if (tripStatus === 'IN_TRANSIT') {
    return 'ARRIVAL';
  }

  return 'ARRIVAL';
}

/** Driver profile from fleet-management /drivers/me. */
export interface DriverProfileDto {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  fullName: string;
  licenseNumber: string;
  licenseClass: string;
  phoneNumber: string;
  organizationName: string;
  vehicleRegistration?: string;
  vehicleType?: string;
  employmentType?: string;
  nationalIdNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  addressCity?: string;
  addressProvince?: string;
  addressPostalCode?: string;
  addressCountry?: string;
}

/** Trip row projected for the driver workspace card list. */
export interface DriverTripRow {
  id: number;
  tripNumber: string;
  shipmentNumber: string;
  route: string;
  cargoLabel: string;
  productName: string;
  quantity: number;
  unitOfMeasure: string;
  vehicleRegistration: string;
  status: string;
  statusLabel: string;
  statusTone: 'muted' | 'warn' | 'success' | 'danger' | 'info';
  startedAtLabel: string;
  estimatedArrivalLabel?: string;
  canTriggerArrival: boolean;
  canStartDeliveryWorkflow: boolean;
  canLiveTrack: boolean;
  deliveryWorkflowPhase?: DeliveryWorkflowPhase;
  /** Assigned driver (enriched from fleet when viewed by dispatcher/admin). */
  driverName?: string;
  driverPhone?: string;
  fleetDriverId?: number;
  driverUserId?: number;
  /** When present, this trip is an inventory transfer (not a customer delivery). */
  inventoryTransferId?: number;
}

/** Summary stats for the driver workspace hero. */
export interface DriverWorkspaceMetrics {
  activeTrips: number;
  completedToday: number;
  pendingDeliveries: number;
}

// ── Delivery workflow request payloads ───────────────────────────────────────

export interface TriggerArrivalRequest {
  tripId: number;
  driverUserId?: number;
}

/** POST /trip-delivery/{tripId}/start-counting */
export interface StartCountingRequest {
  actorRole: DeliveryActorRole;
}

/** POST /trip-delivery/{tripId}/finish-counting */
export interface FinishCountingRequest {
  actorRole: DeliveryActorRole;
}

/** POST /trip-delivery/send-otp — single channel, not array */
export interface SendOtpRequest {
  tripId: number;
  channel: OtpChannel;
  recipientContact: string;
  recipientUserId?: number;
}

/** POST /trip-delivery/verify-otp */
export interface VerifyOtpRequest {
  tripId: number;
  otp: string;
  receiverUserId?: number;
  deliveryNotes?: string;
}

/** POST /trip-delivery/{tripId}/record-returns */
export interface RecordReturnsRequest {
  actorRole: DeliveryActorRole;
  returnLines: DeliveryReturnLine[];
}

/** Driver signup access request payload. */
export interface DriverSignupRequest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  licenseNumber: string;
  licenseClass: string;
  companyCode: string;
}

/** A single chat message between the driver and the receiver for a trip. */
export interface TripMessageDto {
  id: number;
  senderUserId: number;
  senderRole: 'DRIVER' | 'RECEIVER' | string;
  senderName: string;
  body: string;
  createdAtLabel: string;
  mine: boolean;
  read: boolean;
}

/** Receiver (consignee) contact details for a trip. */
export interface ReceiverContactDto {
  userId?: number;
  name?: string;
  phoneNumber?: string;
  email?: string;
  destinationName?: string;
  reachable: boolean;
}

/** Combined chat state returned by the messages endpoints. */
export interface TripChatState {
  messages: TripMessageDto[];
  receiverContact?: ReceiverContactDto;
  myRole: string;
  currentUserId?: number;
}

/** Self-service driver profile edit payload (PUT /drivers/me). */
export interface DriverProfileEditRequest {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  licenseNumber: string;
  licenseClass: string;
  nationalIdNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  addressCity?: string;
  addressProvince?: string;
  addressPostalCode?: string;
  addressCountry?: string;
}
