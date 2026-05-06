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

export type NotificationDeliveryStatus = 'PENDING' | 'SENT' | 'FAILED' | 'SKIPPED';

export interface NotificationLogRow {
  id: number;
  recipientDisplay: string;
  channel: string;
  templateKey: string;
  status: NotificationDeliveryStatus | string;
  sentAt: string;
  retryCount: number;
}

export interface TemplateListResponse {
  statusCode?: number;
  isSuccess?: boolean;
  templateList?: NotificationTemplateRow[];
}

export interface NotificationLogFilters {
  search?: string;
  from?: Date | null;
  to?: Date | null;
}

export type NotificationLogExportFormat = 'csv' | 'excel';

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

export interface TemplateResponse extends TemplateListResponse {
  template?: NotificationTemplateRow;
  addTemplateMetadata?: TemplateCreationMetadataDto;
}

export type TemplateExportFormat = 'csv' | 'excel';

export interface TemplateImportSummary {
  statusCode?: number;
  isSuccess?: boolean;
  message?: string;
  total?: number;
  success?: number;
  failed?: number;
  errors?: string[];
}
