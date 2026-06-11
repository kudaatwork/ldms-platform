package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.logic.api.PurchaseOrderStatusManager;
import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.inventory.management.model.PurchaseOrderStatus;
import projectlx.inventory.management.repository.PurchaseOrderRepository;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderStatusManagerImpl implements PurchaseOrderStatusManager {

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Override
    public void transition(PurchaseOrder order, PurchaseOrderStatus targetStatus,
                           String username, Locale locale) {
        PurchaseOrderStatus current = order.getStatus();
        if (!canTransition(current, targetStatus)) {
            throw new IllegalStateException(String.format("Invalid status transition from %s to %s", current, targetStatus));
        }

        order.setStatus(targetStatus);

        // Set approval tracking when transitioning to APPROVED
        if (targetStatus == PurchaseOrderStatus.APPROVED) {
            if (order.getApprovedByUserId() == null) {
                order.setApprovedByUserId(order.getUpdatedByUserId());
            }
            if (order.getApprovedAt() == null) {
                order.setApprovedAt(LocalDateTime.now());
            }
        }

        // Set customer approval tracking
        if (targetStatus == PurchaseOrderStatus.CUSTOMER_APPROVED) {
            if (order.getCustomerApprovedAt() == null) {
                order.setCustomerApprovedAt(LocalDateTime.now());
            }
            order.setCustomerApprovalComplete(true);
        }

        // Set supplier approval tracking
        if (targetStatus == PurchaseOrderStatus.APPROVED
                && current == PurchaseOrderStatus.PENDING_SUPPLIER_APPROVAL) {
            if (order.getSupplierApprovedAt() == null) {
                order.setSupplierApprovedAt(LocalDateTime.now());
            }
            order.setSupplierApprovalComplete(true);
        }

        // Set received date when fully received
        if (targetStatus == PurchaseOrderStatus.RECEIVED) {
            order.setReceivedDate(LocalDateTime.now());
        }

        purchaseOrderRepository.save(order);
        log.info("Updated purchase order {} status from {} to {}", order.getId(), current, targetStatus);
    }

    @Override
    public boolean canTransition(PurchaseOrderStatus current, PurchaseOrderStatus target) {
        if (current == target) return true;
        return switch (current) {
            case DRAFT -> target == PurchaseOrderStatus.SUBMITTED || target == PurchaseOrderStatus.CANCELLED;
            case SUBMITTED -> target == PurchaseOrderStatus.APPROVED
                    || target == PurchaseOrderStatus.PENDING_CUSTOMER_APPROVAL
                    || target == PurchaseOrderStatus.REJECTED
                    || target == PurchaseOrderStatus.CANCELLED;
            case PENDING_CUSTOMER_APPROVAL -> target == PurchaseOrderStatus.CUSTOMER_APPROVED
                    || target == PurchaseOrderStatus.REJECTED
                    || target == PurchaseOrderStatus.CANCELLED;
            case CUSTOMER_APPROVED -> target == PurchaseOrderStatus.PENDING_SUPPLIER_APPROVAL
                    || target == PurchaseOrderStatus.CANCELLED;
            case PENDING_SUPPLIER_APPROVAL -> target == PurchaseOrderStatus.APPROVED
                    || target == PurchaseOrderStatus.REJECTED
                    || target == PurchaseOrderStatus.CANCELLED;
            case APPROVED -> target == PurchaseOrderStatus.PARTIALLY_RECEIVED
                    || target == PurchaseOrderStatus.RECEIVED
                    || target == PurchaseOrderStatus.CANCELLED;
            case PARTIALLY_RECEIVED -> target == PurchaseOrderStatus.RECEIVED;
            case RECEIVED, CANCELLED, REJECTED -> false;
        };
    }
}