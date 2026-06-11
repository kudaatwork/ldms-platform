import type {
  PurchaseOrderRow,
  PurchaseOrderStatus,
  PurchaseRequisitionRow,
  RequisitionStatus,
  SalesOrderRow,
  SalesOrderStatus,
} from '../models/inventory.model';
import { salesOrderStatusLabel } from './inventory-status.util';

export type ProcurementStageState = 'done' | 'active' | 'pending' | 'rejected';

export interface ProcurementStageStep {
  id: string;
  label: string;
  detail?: string;
  state: ProcurementStageState;
  icon: string;
}

const REQUISITION_ORDER: RequisitionStatus[] = [
  'DRAFT',
  'SUBMITTED',
  'APPROVED',
  'PUBLISHED_TO_SUPPLIER',
  'SUPPLIER_CONFIRMED',
  'CUSTOMER_ACKNOWLEDGED',
  'PARTIALLY_FULFILLED',
  'FULFILLED',
  'CLOSED',
];

const REQUISITION_LABELS: Record<RequisitionStatus, string> = {
  DRAFT: 'Draft requisition',
  SUBMITTED: 'Internal approval',
  APPROVED: 'Approved internally',
  PUBLISHED_TO_SUPPLIER: 'Sent to supplier',
  SUPPLIER_CONFIRMED: 'Supplier quotation',
  CUSTOMER_ACKNOWLEDGED: 'Quote acknowledged',
  PARTIALLY_FULFILLED: 'Purchase order raised',
  FULFILLED: 'Fulfilled',
  CLOSED: 'Closed',
  CANCELLED: 'Cancelled',
  REJECTED: 'Rejected',
  EXPIRED: 'Expired',
};

const PO_LABELS: Record<PurchaseOrderStatus, string> = {
  DRAFT: 'Draft PO',
  SUBMITTED: 'Submitted',
  PENDING_CUSTOMER_APPROVAL: 'Customer approval',
  CUSTOMER_APPROVED: 'Customer approved',
  PENDING_SUPPLIER_APPROVAL: 'Supplier approval',
  APPROVED: 'PO approved',
  PARTIALLY_RECEIVED: 'Partially received',
  RECEIVED: 'Goods received',
  CANCELLED: 'Cancelled',
  REJECTED: 'Rejected',
};

function requisitionIndex(status: RequisitionStatus): number {
  if (status === 'REJECTED' || status === 'CANCELLED' || status === 'EXPIRED') {
    return -1;
  }
  const idx = REQUISITION_ORDER.indexOf(status);
  return idx >= 0 ? idx : 0;
}

function stepState(
  index: number,
  currentIndex: number,
  isRejected: boolean,
): ProcurementStageState {
  if (isRejected) {
    return index <= currentIndex ? 'rejected' : 'pending';
  }
  if (index < currentIndex) {
    return 'done';
  }
  if (index === currentIndex) {
    return 'active';
  }
  return 'pending';
}

export function requisitionStageProgressLabel(row: PurchaseRequisitionRow): string {
  if (row.status === 'SUBMITTED' && row.requiredApprovalStages && row.requiredApprovalStages > 0) {
    const stage = row.currentApprovalStage ?? 0;
    return `Stage ${Math.min(stage + 1, row.requiredApprovalStages)} of ${row.requiredApprovalStages}`;
  }
  return row.statusLabel;
}

export function salesOrderStageProgressLabel(row: SalesOrderRow): string {
  const required = row.requiredApprovalStages ?? 0;
  if (row.status === 'PENDING_APPROVAL' && required > 0) {
    const stage = row.currentApprovalStage ?? 0;
    return `Stage ${Math.min(stage + 1, required)} of ${required}`;
  }
  return row.statusLabel;
}

export function purchaseOrderStageProgressLabel(row: PurchaseOrderRow): string {
  const required = row.requiredApprovalStages ?? 0;
  if (
    (row.status === 'SUBMITTED' || row.status === 'PENDING_CUSTOMER_APPROVAL') &&
    required > 0
  ) {
    const stage = row.currentCustomerApprovalStage ?? 0;
    return `Customer stage ${Math.min(stage + 1, required)} of ${required}`;
  }
  if (row.status === 'PENDING_SUPPLIER_APPROVAL' && required > 0) {
    const stage = row.currentSupplierApprovalStage ?? 0;
    return `Supplier stage ${Math.min(stage + 1, required)} of ${required}`;
  }
  return row.statusLabel;
}

export function buildRequisitionJourney(row: PurchaseRequisitionRow): ProcurementStageStep[] {
  const rejected = row.status === 'REJECTED' || row.status === 'CANCELLED' || row.status === 'EXPIRED';
  const current = requisitionIndex(row.status);
  const macroSteps = REQUISITION_ORDER.slice(0, 7).map((status, index) => {
    let detail: string | undefined;
    if (status === 'SUBMITTED' && row.requiredApprovalStages && row.requiredApprovalStages > 1) {
      const stage = row.currentApprovalStage ?? 0;
      detail = `Stage ${Math.min(stage, row.requiredApprovalStages)} of ${row.requiredApprovalStages} complete`;
    }
    if (status === 'SUPPLIER_CONFIRMED' && row.supplierQuoteId) {
      detail = 'Supplier quote on file';
    }
    return {
      id: status,
      label: REQUISITION_LABELS[status],
      detail,
      state: stepState(index, current, rejected),
      icon:
        status === 'SUPPLIER_CONFIRMED'
          ? 'request_quote'
          : status === 'CUSTOMER_ACKNOWLEDGED'
            ? 'thumb_up'
            : status === 'PUBLISHED_TO_SUPPLIER'
              ? 'send'
              : 'approval',
    };
  });

  if (rejected) {
    macroSteps.push({
      id: row.status,
      label: REQUISITION_LABELS[row.status],
      detail: 'Workflow stopped',
      state: 'rejected',
      icon: 'block',
    });
  }

  return macroSteps;
}

export function buildSalesOrderJourney(row: SalesOrderRow): ProcurementStageStep[] {
  const rejected = row.status === 'CANCELLED';
  const steps: ProcurementStageStep[] = [
    {
      id: 'CREATED',
      label: 'Created from payment',
      detail: row.purchaseOrderNumber ? `PO ${row.purchaseOrderNumber}` : undefined,
      state: 'done',
      icon: 'receipt_long',
    },
    {
      id: 'SUPPLIER_APPROVAL',
      label: 'Supplier approval',
      detail:
        row.requiredApprovalStages && row.requiredApprovalStages > 0
          ? `${row.currentApprovalStage ?? 0} of ${row.requiredApprovalStages} stages`
          : undefined,
      state: salesOrderApprovalState(row),
      icon: 'verified_user',
    },
    {
      id: 'APPROVED',
      label: salesOrderStatusLabel('APPROVED'),
      state: salesOrderMilestoneState(row, 'APPROVED'),
      icon: 'task_alt',
    },
    {
      id: 'FULFILLMENT',
      label: 'Fulfillment & dispatch',
      state: salesOrderFulfillmentState(row),
      icon: 'local_shipping',
    },
    {
      id: 'DELIVERED',
      label: 'Delivered',
      state: salesOrderMilestoneState(row, 'DELIVERED'),
      icon: 'inventory',
    },
  ];

  if (rejected) {
    steps.push({
      id: row.status,
      label: salesOrderStatusLabel(row.status),
      detail: 'Workflow stopped',
      state: 'rejected',
      icon: 'block',
    });
  }

  return steps;
}

export function buildPurchaseOrderJourney(row: PurchaseOrderRow): ProcurementStageStep[] {
  const rejected = row.status === 'REJECTED' || row.status === 'CANCELLED';
  const steps: ProcurementStageStep[] = [
    { id: 'DRAFT', label: 'Draft PO', state: 'done', icon: 'edit_note' },
    {
      id: 'CUSTOMER_APPROVAL',
      label: 'Customer approval',
      detail:
        row.requiredApprovalStages && row.requiredApprovalStages > 0
          ? `${row.currentCustomerApprovalStage ?? 0} of ${row.requiredApprovalStages} stages`
          : undefined,
      state: customerApprovalState(row),
      icon: 'verified_user',
    },
    {
      id: 'SUPPLIER_APPROVAL',
      label: 'Supplier approval',
      detail:
        row.requiredApprovalStages && row.requiredApprovalStages > 0
          ? `${row.currentSupplierApprovalStage ?? 0} of ${row.requiredApprovalStages} stages`
          : undefined,
      state: supplierApprovalState(row),
      icon: 'store',
    },
    {
      id: 'APPROVED',
      label: PO_LABELS.APPROVED,
      state: poMilestoneState(row, 'APPROVED'),
      icon: 'task_alt',
    },
    {
      id: 'PAYMENT',
      label: 'Payment & verification',
      detail: 'Customer records payment; finance verifies',
      state: paymentState(row),
      icon: 'payments',
    },
    {
      id: 'DELIVERY',
      label: 'Goods receipt',
      state: deliveryState(row),
      icon: 'local_shipping',
    },
  ];

  if (rejected) {
    steps.push({
      id: row.status,
      label: PO_LABELS[row.status],
      detail: 'Workflow stopped',
      state: 'rejected',
      icon: 'block',
    });
  }

  return steps;
}

function customerApprovalState(row: PurchaseOrderRow): ProcurementStageState {
  if (row.status === 'REJECTED' || row.status === 'CANCELLED') {
    return 'rejected';
  }
  if (row.customerApprovalComplete || poPast(row, 'PENDING_SUPPLIER_APPROVAL')) {
    return 'done';
  }
  if (row.status === 'SUBMITTED' || row.status === 'PENDING_CUSTOMER_APPROVAL') {
    return 'active';
  }
  return 'pending';
}

function supplierApprovalState(row: PurchaseOrderRow): ProcurementStageState {
  if (row.status === 'REJECTED' || row.status === 'CANCELLED') {
    return 'rejected';
  }
  if (row.supplierApprovalComplete || poPast(row, 'APPROVED')) {
    return 'done';
  }
  if (row.status === 'PENDING_SUPPLIER_APPROVAL') {
    return 'active';
  }
  return 'pending';
}

function poMilestoneState(row: PurchaseOrderRow, milestone: PurchaseOrderStatus): ProcurementStageState {
  if (row.status === 'REJECTED' || row.status === 'CANCELLED') {
    return 'rejected';
  }
  const order: PurchaseOrderStatus[] = [
    'DRAFT',
    'SUBMITTED',
    'PENDING_CUSTOMER_APPROVAL',
    'CUSTOMER_APPROVED',
    'PENDING_SUPPLIER_APPROVAL',
    'APPROVED',
    'PARTIALLY_RECEIVED',
    'RECEIVED',
  ];
  const current = order.indexOf(row.status);
  const target = order.indexOf(milestone);
  if (current < 0 || target < 0) {
    return 'pending';
  }
  if (current > target) {
    return 'done';
  }
  if (current === target) {
    return 'active';
  }
  return 'pending';
}

function paymentState(row: PurchaseOrderRow): ProcurementStageState {
  if (poPast(row, 'PARTIALLY_RECEIVED')) {
    return 'done';
  }
  if (row.status === 'APPROVED') {
    return 'active';
  }
  return 'pending';
}

function deliveryState(row: PurchaseOrderRow): ProcurementStageState {
  if (row.status === 'RECEIVED') {
    return 'done';
  }
  if (row.status === 'PARTIALLY_RECEIVED') {
    return 'active';
  }
  return 'pending';
}

function salesOrderApprovalState(row: SalesOrderRow): ProcurementStageState {
  if (row.status === 'CANCELLED') {
    return 'rejected';
  }
  if (salesOrderPast(row, 'APPROVED')) {
    return 'done';
  }
  if (row.status === 'PENDING_APPROVAL') {
    return 'active';
  }
  return 'pending';
}

function salesOrderFulfillmentState(row: SalesOrderRow): ProcurementStageState {
  if (row.status === 'CANCELLED') {
    return 'rejected';
  }
  if (salesOrderPast(row, 'DELIVERED')) {
    return 'done';
  }
  if (row.status === 'CONFIRMED' || row.status === 'PARTIALLY_SHIPPED' || row.status === 'SHIPPED') {
    return 'active';
  }
  return 'pending';
}

function salesOrderMilestoneState(row: SalesOrderRow, milestone: SalesOrderStatus): ProcurementStageState {
  if (row.status === 'CANCELLED') {
    return 'rejected';
  }
  const order: SalesOrderStatus[] = [
    'PENDING_APPROVAL',
    'APPROVED',
    'CONFIRMED',
    'PARTIALLY_SHIPPED',
    'SHIPPED',
    'DELIVERED',
    'FULFILLED',
  ];
  const current = order.indexOf(row.status);
  const target = order.indexOf(milestone);
  if (current < 0 || target < 0) {
    return row.status === milestone ? 'active' : 'pending';
  }
  if (current > target) {
    return 'done';
  }
  if (current === target) {
    return 'active';
  }
  return 'pending';
}

function salesOrderPast(row: SalesOrderRow, status: SalesOrderStatus): boolean {
  const order: SalesOrderStatus[] = [
    'PENDING_APPROVAL',
    'APPROVED',
    'CONFIRMED',
    'PARTIALLY_SHIPPED',
    'SHIPPED',
    'DELIVERED',
    'FULFILLED',
  ];
  const current = order.indexOf(row.status);
  const target = order.indexOf(status);
  if (current < 0) {
    return false;
  }
  return current > target;
}

function poPast(row: PurchaseOrderRow, status: PurchaseOrderStatus): boolean {
  const order: PurchaseOrderStatus[] = [
    'DRAFT',
    'SUBMITTED',
    'PENDING_CUSTOMER_APPROVAL',
    'CUSTOMER_APPROVED',
    'PENDING_SUPPLIER_APPROVAL',
    'APPROVED',
    'PARTIALLY_RECEIVED',
    'RECEIVED',
  ];
  return order.indexOf(row.status) > order.indexOf(status);
}
