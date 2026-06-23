/** Known LDMS platform wallet action codes — align with {@code PlatformWalletActionCodes} and Flyway seeds. */
export interface PlatformActionCatalogEntry {
  actionCode: string;
  displayName: string;
  description: string;
  category: string;
  billingTier?: string;
  suggestedCents?: number;
}

export const PLATFORM_ACTION_CATALOG: readonly PlatformActionCatalogEntry[] = [
  // Trips & tracking
  { actionCode: 'TRIP_CREATE', displayName: 'Trip booking fee', description: 'Supplier binds a truck and driver to an order', category: 'TRIPS', billingTier: 'MILESTONE', suggestedCents: 1000 },
  { actionCode: 'TRIP_COMPLETE', displayName: 'Trip completion fee', description: 'Trip marked complete with audit trail', category: 'TRIPS', billingTier: 'MILESTONE', suggestedCents: 750 },
  { actionCode: 'TRIP_ASSIGN_DRIVER', displayName: 'Assign driver to trip', description: 'Driver assignment to an active trip', category: 'TRIPS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'TRIP_TRACK', displayName: 'Trip tracking ping', description: 'GPS / status tracking update for a trip', category: 'TRIPS', billingTier: 'TRACKING', suggestedCents: 150 },
  { actionCode: 'LIVE_MAP_SESSION', displayName: 'Live map session', description: 'Ops live trip map refresh cycle', category: 'TRIPS', billingTier: 'TRACKING', suggestedCents: 150 },
  { actionCode: 'GPS_PING', displayName: 'GPS location ping', description: 'IoT / mobile GPS telemetry point', category: 'IOT', billingTier: 'TRACKING', suggestedCents: 150 },
  { actionCode: 'FUEL_TELEMETRY_DAY', displayName: 'Fuel telemetry day', description: 'Per vehicle per day when fuel sensors are enabled', category: 'IOT', billingTier: 'TELEMETRY', suggestedCents: 175 },

  // Logistics & shipments
  { actionCode: 'SHIPMENT_DISPATCH', displayName: 'Shipment dispatch fee', description: 'Shipment dispatch released to corridor execution', category: 'LOGISTICS', billingTier: 'MILESTONE', suggestedCents: 800 },
  { actionCode: 'SHIPMENT_UPDATE', displayName: 'Shipment update', description: 'Shipment status change event', category: 'LOGISTICS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'CLEARING_AGENT_MATCH', displayName: 'Clearing agent match fee', description: 'Supplier selects a clearing agent for border clearance', category: 'LOGISTICS', billingTier: 'MILESTONE', suggestedCents: 2500 },
  { actionCode: 'ROAD_FUND_TRANSFER', displayName: 'Road fund transfer fee', description: 'Driver road-fund payout via payment gateway', category: 'LOGISTICS', billingTier: 'MILESTONE', suggestedCents: 300 },
  { actionCode: 'FUEL_FUND_REQUEST', displayName: 'Road fund transfer fee', description: 'Emergency operational funds sent to a driver', category: 'LOGISTICS', billingTier: 'MILESTONE', suggestedCents: 300 },
  { actionCode: 'ROADSIDE_INCIDENT', displayName: 'Roadside incident log', description: 'Mechanic or roadside support incident', category: 'LOGISTICS', billingTier: 'INCLUDED', suggestedCents: 0 },

  // Orders & inventory
  { actionCode: 'ORDER_CREATE', displayName: 'Order created', description: 'Purchase or sales order created', category: 'ORDERS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'INVENTORY_GRV_CREATE', displayName: 'Goods received (GRV) fee', description: 'Proof of delivery recorded at destination', category: 'ORDERS', billingTier: 'MILESTONE', suggestedCents: 500 },
  { actionCode: 'INVENTORY_STOCK_RESERVE', displayName: 'Stock reservation', description: 'Reserve stock against a purchase order', category: 'ORDERS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'INVENTORY_TRANSFER', displayName: 'Stock transfer', description: 'Inter-warehouse stock transfer', category: 'ORDERS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'PROCUREMENT_PR_APPROVE', displayName: 'Purchase requisition approval', description: 'Internal approval stage on a purchase requisition', category: 'PROCUREMENT', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'PROCUREMENT_QUOTE_SUBMIT', displayName: 'Supplier quote submitted', description: 'Supplier submits a quote against a requisition', category: 'PROCUREMENT', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'PROCUREMENT_PO_CUSTOMER_APPROVE', displayName: 'PO customer approval', description: 'Customer approval stage on a purchase order', category: 'PROCUREMENT', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'PROCUREMENT_PO_SUPPLIER_APPROVE', displayName: 'PO supplier approval', description: 'Supplier approval stage on a purchase order', category: 'PROCUREMENT', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'PROCUREMENT_SO_APPROVE', displayName: 'Sales order approval', description: 'Supplier approval stage on a sales order', category: 'PROCUREMENT', billingTier: 'INCLUDED', suggestedCents: 0 },

  // Fleet
  { actionCode: 'FLEET_VEHICLE_REGISTER', displayName: 'Register fleet vehicle', description: 'New fleet asset registration', category: 'FLEET', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'FLEET_DRIVER_HIRE', displayName: 'Hire freelance driver', description: 'Add a driver from the marketplace to your roster', category: 'FLEET', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'FLEET_COMPLIANCE_UPLOAD', displayName: 'Fleet compliance document', description: 'Upload fleet or driver compliance record', category: 'FLEET', billingTier: 'INCLUDED', suggestedCents: 0 },

  // Notifications
  { actionCode: 'NOTIFICATION_EMAIL', displayName: 'Email notification', description: 'Outbound email via notifications service', category: 'NOTIFICATIONS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'NOTIFICATION_PUSH', displayName: 'Push notification', description: 'Mobile push notification', category: 'NOTIFICATIONS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'NOTIFICATION_SMS', displayName: 'SMS notification', description: 'Outbound SMS message', category: 'NOTIFICATIONS', billingTier: 'MESSAGING', suggestedCents: 10 },
  { actionCode: 'WHATSAPP_COMMAND', displayName: 'WhatsApp bot command', description: 'Inbound WhatsApp command processed by the bot', category: 'BOT_SERVICE', billingTier: 'MESSAGING', suggestedCents: 8 },

  // Bot & support
  { actionCode: 'BOT_SESSION_START', displayName: 'Start bot session', description: 'Open a new LDMS Assistant conversation session', category: 'BOT_SERVICE', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'HELP_BOT_MESSAGE', displayName: 'LDMS Assistant message', description: 'User message to the LDMS AI assistant', category: 'BOT_SERVICE', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'HELP_SUPPORT_TICKET_OPEN', displayName: 'Support ticket opened', description: 'Open a live support ticket for agent follow-up', category: 'SUPPORT', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'HELP_LIVE_CHAT_MESSAGE', displayName: 'Live support message', description: 'Message in a support ticket conversation', category: 'SUPPORT', billingTier: 'INCLUDED', suggestedCents: 0 },

  // Documents & billing
  { actionCode: 'DOCUMENT_UPLOAD', displayName: 'Document upload', description: 'File stored in document service', category: 'DOCUMENTS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'DOCUMENT_SHARE', displayName: 'Document shared', description: 'Document shared with another party', category: 'DOCUMENTS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'INVOICE_GENERATE', displayName: 'Invoice generation fee', description: 'Customer invoice generated after delivery (GRV)', category: 'BILLING', billingTier: 'MILESTONE', suggestedCents: 500 },

  // Analytics
  { actionCode: 'REPORT_EXPORT', displayName: 'Report export', description: 'Export analytics or operational reports', category: 'ANALYTICS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'BOT_ANALYTICS_EXPORT', displayName: 'Bot analytics export', description: 'Export bot usage and conversation analytics', category: 'ANALYTICS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'SHIPMENT_ANALYTICS_EXPORT', displayName: 'Shipment analytics export', description: 'Export shipment corridor analytics and KPIs', category: 'ANALYTICS', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'REVENUE_REPORT_EXPORT', displayName: 'Revenue report export', description: 'Export platform revenue and wallet earnings report', category: 'ANALYTICS', billingTier: 'INCLUDED', suggestedCents: 0 },

  // Platform
  { actionCode: 'AUDIT_LOG_WRITE', displayName: 'Audit log entry', description: 'Platform audit trail write', category: 'PLATFORM', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'API_INTEGRATION_CALL', displayName: 'Integration API call', description: 'External integration API invocation', category: 'PLATFORM', billingTier: 'INCLUDED', suggestedCents: 0 },
  { actionCode: 'ORG_CUSTOMER_REGISTER', displayName: 'Register customer org', description: 'Supplier registers a customer organisation', category: 'PLATFORM', billingTier: 'INCLUDED', suggestedCents: 0 },
];

export function catalogEntryForCode(actionCode: string): PlatformActionCatalogEntry | undefined {
  const normalized = actionCode.trim().toUpperCase();
  return PLATFORM_ACTION_CATALOG.find((entry) => entry.actionCode === normalized);
}

export function searchPlatformActionCatalog(query: string, limit = 20): PlatformActionCatalogEntry[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return [...PLATFORM_ACTION_CATALOG].slice(0, limit);
  }
  return PLATFORM_ACTION_CATALOG.filter((entry) => {
    const haystack = `${entry.actionCode} ${entry.displayName} ${entry.description} ${entry.category}`.toLowerCase();
    return haystack.includes(q);
  }).slice(0, limit);
}

export function catalogByCategory(): Map<string, PlatformActionCatalogEntry[]> {
  const grouped = new Map<string, PlatformActionCatalogEntry[]>();
  for (const entry of PLATFORM_ACTION_CATALOG) {
    const list = grouped.get(entry.category) ?? [];
    list.push(entry);
    grouped.set(entry.category, list);
  }
  for (const list of grouped.values()) {
    list.sort((a, b) => a.displayName.localeCompare(b.displayName));
  }
  return grouped;
}
