export type TransporterContractStatus = 'active' | 'expired' | 'upcoming' | 'open_ended';

export interface TransporterContractPresentation {
  startLabel: string;
  endLabel: string;
  linkedSinceLabel: string;
  status: TransporterContractStatus;
  statusLabel: string;
  rangeLabel: string;
}

function formatDate(value: unknown): string {
  if (value == null || value === '') {
    return '—';
  }
  const d = new Date(String(value));
  if (Number.isNaN(d.getTime())) {
    return String(value);
  }
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function toDate(value: unknown): Date | null {
  if (value == null || value === '') {
    return null;
  }
  const d = new Date(String(value));
  return Number.isNaN(d.getTime()) ? null : d;
}

function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function presentTransporterContract(dto: Record<string, unknown>): TransporterContractPresentation {
  const start = toDate(dto['contractStartDate']);
  const end = toDate(dto['contractEndDate']);
  const linked = toDate(dto['contractLinkedAt'] ?? dto['createdAt']);
  const today = startOfDay(new Date());

  let status: TransporterContractStatus = 'open_ended';
  let statusLabel = 'Open-ended contract';

  if (start && startOfDay(start) > today) {
    status = 'upcoming';
    statusLabel = 'Starts soon';
  } else if (end && startOfDay(end) < today) {
    status = 'expired';
    statusLabel = 'Contract ended';
  } else if (start) {
    status = end ? 'active' : 'open_ended';
    statusLabel = end ? 'Active contract' : 'Open-ended contract';
  }

  const startLabel = formatDate(dto['contractStartDate']);
  const endLabel = end ? formatDate(dto['contractEndDate']) : 'Open-ended';
  const rangeLabel = end ? `${startLabel} → ${endLabel}` : `${startLabel} → ongoing`;

  return {
    startLabel,
    endLabel,
    linkedSinceLabel: formatDate(linked),
    status,
    statusLabel,
    rangeLabel,
  };
}

export interface TransporterContractDateBounds {
  minDate: string;
  maxDate: string | null;
  rangeLabel: string;
  endRequired: boolean;
}

export function resolveTransporterContractDateBounds(partner?: {
  contractStartDate?: string;
  contractEndDate?: string;
  contractRangeLabel?: string;
} | null): TransporterContractDateBounds | null {
  const minDate = String(partner?.contractStartDate ?? '').trim();
  if (!minDate) {
    return null;
  }
  const maxDateRaw = String(partner?.contractEndDate ?? '').trim();
  return {
    minDate,
    maxDate: maxDateRaw || null,
    rangeLabel: partner?.contractRangeLabel ?? (maxDateRaw ? `${minDate} → ${maxDateRaw}` : `${minDate} → ongoing`),
    endRequired: !!maxDateRaw,
  };
}

export function isDateWithinInclusiveBounds(date: string, minDate: string, maxDate: string | null): boolean {
  const value = String(date ?? '').trim();
  if (!value || !minDate) {
    return false;
  }
  if (value < minDate) {
    return false;
  }
  return !(maxDate && value > maxDate);
}
