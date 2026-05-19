export type NotificationChannel = 'EMAIL' | 'SMS' | 'WHATSAPP' | 'IN_APP' | 'SLACK' | 'TEAMS';

export interface NotificationTemplateRow {
  id: number;
  templateKey: string;
  description?: string;
  channels: NotificationChannel[];
  emailBodyHtml?: string | null;
  smsBody?: string | null;
  inAppTitle?: string | null;
  inAppBody?: string | null;
  whatsappTemplateName?: string | null;
  whatsappBody?: string | null;
  emailSubject?: string | null;
  isActive: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export type NotificationDeliveryStatus = 'QUEUED' | 'PENDING' | 'SENT' | 'FAILED' | 'SKIPPED';

export interface NotificationLogRow {
  id: number;
  eventId?: string | null;
  recipientId?: string | null;
  recipientDisplay: string;
  recipientEmail?: string | null;
  recipientPhone?: string | null;
  channel: string;
  templateKey: string;
  status: NotificationDeliveryStatus | string;
  provider?: string | null;
  providerMessageId?: string | null;
  errorMessage?: string | null;
  sentAt: string;
  createdAt?: string | null;
  updatedAt?: string | null;
  retryCount: number;
}

export interface NotificationQueueSummary {
  queueName?: string;
  messagesReady?: number;
  messagesUnacked?: number;
  exchangeName?: string;
  routingKey?: string;
}

export interface TemplateListResponse {
  statusCode?: number;
  isSuccess?: boolean;
  templateList?: NotificationTemplateRow[];
}

export interface NotificationLogFilters {
  page?: number;
  size?: number;
  /** Maps to backend {@code searchValue} (broad search). */
  search?: string;
  /** Exact-ish template key filter (backend: contains match). */
  templateKey?: string;
  /** Channel enum string e.g. EMAIL, SMS (backend: exact). */
  channel?: string;
  /** Delivery status e.g. SENT, FAILED (backend: exact). */
  status?: string;
  /** User id string (backend: exact match on recipientId). */
  recipientId?: string;
  /** Provider substring (backend: case-insensitive contains). */
  provider?: string;
  from?: Date | null;
  to?: Date | null;
}

/** POST body for {@code notification-log/find-by-multiple-filters} (mirrors backend request). */
export interface NotificationLogMultipleFiltersRequest {
  page: number;
  size: number;
  searchValue?: string;
  templateKey?: string;
  channel?: string;
  status?: string;
  recipientId?: string;
  provider?: string;
  from?: string;
  to?: string;
}

/** Spring `Page<>` JSON as nested under `NotificationLogResponse.notificationLogPage`. */
export interface NotificationLogPage {
  content?: NotificationLogRow[];
  totalElements?: number;
}

export interface NotificationLogListResponse {
  statusCode?: number;
  success?: boolean;
  isSuccess?: boolean;
  message?: string;
  notificationLogPage?: NotificationLogPage;
  queueSummary?: NotificationQueueSummary;
}

export type NotificationLogExportFormat = 'csv' | 'excel' | 'pdf';

/** Mirrors backend CreateTemplateRequest — always sent as a single JSON object with all keys (strings may be ''). */
export interface CreateTemplateRequest {
  templateKey: string;
  description: string;
  channels: NotificationChannel[];
  emailSubject: string;
  emailBodyHtml: string;
  smsBody: string;
  inAppTitle: string;
  inAppBody: string;
  whatsappTemplateName: string;
  whatsappBody: string;
}

export interface UpdateTemplateRequest extends CreateTemplateRequest {
  id: number;
  isActive: boolean;
}

/** Mirrors backend TemplateMultipleFiltersRequest (extends MultipleFiltersRequest). */
export interface TemplateMultipleFiltersRequest {
  page?: number;
  size?: number;
  searchValue?: string;
  templateKey?: string;
  channels?: string[];
  inAppTitle?: string;
  whatsappTemplateName?: string;
  isActive?: boolean;
}

export interface ChannelOptionDto {
  value: string;
  label: string;
  description?: string;
}

export interface TemplateFormSectionDto {
  sectionKey: string;
  sectionLabel: string;
  sectionDescription?: string;
  order?: number;
  fieldKeys?: string[];
}

export interface TemplateCreationMetadataDto {
  sections?: TemplateFormSectionDto[];
  channelOptions?: ChannelOptionDto[];
}

/** Spring `Page<>` JSON as nested under `TemplateResponse.templatePage`. */
export interface NotificationTemplatePage {
  content?: NotificationTemplateRow[];
  totalElements?: number;
}

export interface TemplateResponse extends TemplateListResponse {
  template?: NotificationTemplateRow;
  addTemplateMetadata?: TemplateCreationMetadataDto;
  templatePage?: NotificationTemplatePage;
}

export type TemplateExportFormat = 'csv' | 'excel' | 'pdf';

export interface TemplateImportSummary {
  statusCode?: number;
  isSuccess?: boolean;
  message?: string;
  total?: number;
  success?: number;
  failed?: number;
  errors?: string[];
}
