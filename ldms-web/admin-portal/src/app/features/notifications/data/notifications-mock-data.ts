import type {
  NotificationLogRow,
  NotificationTemplateRow,
} from '../models/notification-admin.models';

/** Exact template keys required for mock parity with seeded templates */
export const MOCK_TEMPLATE_CODES = [
  'ORG_SUBMITTED',
  'KYC_APPROVED',
  'KYC_REJECTED',
  'PO_CREATED',
  'PO_APPROVED',
  'SHIPMENT_CREATED',
  'TRIP_STARTED',
  'TRIP_BORDER_CLEARED',
  'TRIP_DELIVERED',
  'GRV_CREATED',
  'INVOICE_CREATED',
  'INVOICE_OVERDUE',
  'WELCOME_EMAIL',
] as const;

export const MOCK_NOTIFICATION_TEMPLATES: NotificationTemplateRow[] = MOCK_TEMPLATE_CODES.map(
  (key, i) => ({
    id: i + 1,
    templateKey: key,
    description: `Seeded template for ${key.replace(/_/g, ' ').toLowerCase()}`,
    channels: i % 3 === 0 ? ['EMAIL', 'IN_APP'] : i % 3 === 1 ? ['EMAIL', 'SMS'] : ['EMAIL'],
    emailSubject:
      key === 'WELCOME_EMAIL'
        ? 'Welcome to Project LX'
        : key.includes('INVOICE')
          ? 'Invoice notification'
          : `Notification: ${key}`,
    isActive: key !== 'INVOICE_OVERDUE',
  }),
);

export const MOCK_NOTIFICATION_LOG: NotificationLogRow[] = [
  {
    id: 1,
    recipientDisplay: 'ops@projectlx.co.zw',
    channel: 'EMAIL',
    templateKey: 'KYC_APPROVED',
    status: 'SENT',
    sentAt: '2026-04-18T10:22:00',
    retryCount: 0,
  },
  {
    id: 2,
    recipientDisplay: 'finance@acme.co.zw',
    channel: 'EMAIL',
    templateKey: 'INVOICE_CREATED',
    status: 'PENDING',
    sentAt: '2026-04-18T11:05:00',
    retryCount: 0,
  },
  {
    id: 3,
    recipientDisplay: 'driver@hauliers.co.zw',
    channel: 'SMS',
    templateKey: 'TRIP_STARTED',
    status: 'FAILED',
    sentAt: '2026-04-17T16:40:00',
    retryCount: 2,
  },
  {
    id: 4,
    recipientDisplay: 'viewer@demo.org',
    channel: 'IN_APP',
    templateKey: 'PO_CREATED',
    status: 'SKIPPED',
    sentAt: '2026-04-16T09:15:00',
    retryCount: 0,
  },
];
