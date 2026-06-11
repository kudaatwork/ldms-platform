import type {
  PurchaseOrderStatus,
  RequisitionStatus,
  SalesOrderStatus,
  StatusTone,
  TransferStatus,
} from '../models/inventory.model';

/** Maps entity lifecycle values to global {@code lx-status-*} suffixes. */
export function entityStatusCssClass(status: string): string {
  switch (String(status ?? '').trim().toUpperCase()) {
    case 'ACTIVE':
      return 'active';
    case 'DELETED':
      return 'rejected';
    case 'INACTIVE':
    case 'DISABLED':
      return 'inactive';
    case 'PENDING':
      return 'pending';
    default:
      return 'inactive';
  }
}

export function entityStatusLabel(status: string): string {
  const normalized = String(status ?? '').trim().toUpperCase();
  if (!normalized) {
    return '—';
  }
  return normalized.charAt(0) + normalized.slice(1).toLowerCase();
}

export function transferStatusCssClass(status: TransferStatus): string {
  const map: Record<TransferStatus, string> = {
    REQUESTED: 'pending',
    APPROVED: 'approved',
    IN_TRANSIT: 'transit',
    COMPLETED: 'approved',
    REJECTED: 'rejected',
    CANCELLED: 'inactive',
  };
  return map[status] ?? 'inactive';
}

export function requisitionStatusCssClass(status: RequisitionStatus): string {
  const map: Record<RequisitionStatus, string> = {
    DRAFT: 'inactive',
    SUBMITTED: 'submitted',
    APPROVED: 'approved',
    PUBLISHED_TO_SUPPLIER: 'pending',
    SUPPLIER_CONFIRMED: 'warn',
    CUSTOMER_ACKNOWLEDGED: 'approved',
    PARTIALLY_FULFILLED: 'pending',
    FULFILLED: 'approved',
    CLOSED: 'inactive',
    REJECTED: 'rejected',
    CANCELLED: 'inactive',
    EXPIRED: 'rejected',
  };
  return map[status] ?? 'inactive';
}

export function salesOrderStatusCssClass(status: SalesOrderStatus): string {
  const map: Record<SalesOrderStatus, string> = {
    AWAITING_RECEIPT: 'pending',
    PENDING: 'pending',
    PENDING_APPROVAL: 'submitted',
    APPROVED: 'approved',
    CONFIRMED: 'stage1',
    PARTIALLY_SHIPPED: 'transit',
    SHIPPED: 'transit',
    DELIVERED: 'approved',
    FULFILLED: 'approved',
    CANCELLED: 'inactive',
  };
  return map[status] ?? 'inactive';
}

export function salesOrderStatusLabel(status: SalesOrderStatus): string {
  const map: Record<SalesOrderStatus, string> = {
    AWAITING_RECEIPT: 'Awaiting receipt',
    PENDING: 'Pending confirmation',
    PENDING_APPROVAL: 'Pending approval',
    APPROVED: 'Approved',
    CONFIRMED: 'Confirmed',
    PARTIALLY_SHIPPED: 'Partially shipped',
    SHIPPED: 'Shipped',
    DELIVERED: 'Delivered',
    FULFILLED: 'Fulfilled',
    CANCELLED: 'Cancelled',
  };
  return map[status] ?? status;
}

export function purchaseOrderStatusCssClass(status: PurchaseOrderStatus): string {
  const map: Record<PurchaseOrderStatus, string> = {
    DRAFT: 'inactive',
    SUBMITTED: 'submitted',
    PENDING_CUSTOMER_APPROVAL: 'pending',
    CUSTOMER_APPROVED: 'stage1',
    PENDING_SUPPLIER_APPROVAL: 'pending',
    APPROVED: 'approved',
    PARTIALLY_RECEIVED: 'pending',
    RECEIVED: 'approved',
    REJECTED: 'rejected',
    CANCELLED: 'inactive',
  };
  return map[status] ?? 'inactive';
}

/** Fallback when only tone metadata is available. */
export function statusToneCssClass(tone: StatusTone): string {
  const map: Record<StatusTone, string> = {
    muted: 'inactive',
    warn: 'pending',
    success: 'approved',
    danger: 'rejected',
  };
  return map[tone] ?? 'inactive';
}
