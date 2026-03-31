/** Seven organization classifications — align with backend enums when wired. */
export interface OrgClassificationNav {
  slug: string;
  label: string;
}

export const ORG_CLASSIFICATIONS: readonly OrgClassificationNav[] = [
  { slug: 'producer', label: 'Producer' },
  { slug: 'processor', label: 'Processor' },
  { slug: 'transporter', label: 'Transporter' },
  { slug: 'aggregator', label: 'Aggregator' },
  { slug: 'distributor', label: 'Distributor' },
  { slug: 'retailer', label: 'Retailer' },
  { slug: 'other', label: 'Other' },
] as const;
