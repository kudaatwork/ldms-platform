export interface ClerkProfileDto {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
  organizationName: string;
  branchName: string;
  branchId: number;
}

export interface IncomingDeliveryRow {
  tripId: number;
  tripNumber: string;
  status: string;
  productName: string;
  quantity: number;
  driverName: string;
  vehicleReg?: string;
  originBranch: string;
  eta?: string;
  arrivedAt?: string;
  inventoryTransferId?: number;
}

export interface ClerkWorkspaceMetrics {
  incomingToday: number;
  receivedToday: number;
  pendingDeliveries: number;
}

export interface StockReceiveRequest {
  tripId: number;
  quantityReceived: number;
  notes?: string;
  condition?: 'GOOD' | 'DAMAGED' | 'PARTIAL';
}

/** A single chat message between the clerk (receiver) and the driver for a trip. */
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

/** Contact details for the party the clerk is chatting with (the driver). */
export interface ChatContactDto {
  userId?: number;
  name?: string;
  phoneNumber?: string;
  email?: string;
  destinationName?: string;
  reachable: boolean;
}

/** Combined chat state returned by the trip messages endpoints. */
export interface TripChatState {
  messages: TripMessageDto[];
  contact?: ChatContactDto;
  myRole: string;
  currentUserId?: number;
}
