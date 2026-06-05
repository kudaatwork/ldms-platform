export type OrganizationMetadataTone = 'default' | 'success' | 'warn' | 'muted' | 'danger';

export type OrganizationMetadataItemKind = 'text' | 'badge' | 'link' | 'upload';

export interface OrganizationMetadataItem {
  label: string;
  value: string;
  tone?: OrganizationMetadataTone;
  kind?: OrganizationMetadataItemKind;
}

export interface OrganizationMetadataSection {
  id: string;
  title: string;
  icon: string;
  items: OrganizationMetadataItem[];
}

/** Parsed organisation profile for relationship spotlight panels. */
export interface OrganizationPartnerMetadata {
  id: number;
  name: string;
  sections: OrganizationMetadataSection[];
}
