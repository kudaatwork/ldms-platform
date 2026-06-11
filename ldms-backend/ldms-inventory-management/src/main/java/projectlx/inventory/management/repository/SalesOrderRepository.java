package projectlx.inventory.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SalesOrder> findByIdAndEntityStatusNot(Long salesOrderId, EntityStatus entityStatus);

    Optional<SalesOrder> findByIdAndSupplierOrganizationIdAndEntityStatusNot(Long salesOrderId,
                                                                            Long supplierOrganizationId,
                                                                            EntityStatus entityStatus);
    // ========================================
    // NEW METHODS: PO → SO Linkage
    // ========================================

    /**
     * Find Sales Order by originating Purchase Order ID
     * Used to check if SO already exists for a PO
     *
     * @param purchaseOrderId The PO ID
     * @return Optional containing the SO if found
     */
    Optional<SalesOrder> findByPurchaseOrderId(Long purchaseOrderId);

    /**
     * Find Sales Order by PO ID, excluding deleted
     *
     * @param purchaseOrderId The PO ID
     * @param entityStatus Status to exclude (typically DELETED)
     * @return Optional containing the SO if found
     */
    Optional<SalesOrder> findByPurchaseOrderIdAndEntityStatusNot(Long purchaseOrderId,
                                                                 EntityStatus entityStatus);

    /**
     * Find all Sales Orders for a given supplier organization
     *
     * @param supplierOrgId The supplier organization ID
     * @param entityStatus Status to exclude
     * @return List of Sales Orders
     */
    List<SalesOrder> findBySupplierOrganizationIdAndEntityStatusNot(Long supplierOrgId,
                                                                    EntityStatus entityStatus);

    List<SalesOrder> findByCustomerIdAndEntityStatusNotOrderByCreatedAtDesc(Long customerId,
                                                                            EntityStatus entityStatus);

    /**
     * Find Sales Orders awaiting confirmation
     * Useful for supplier dashboard showing pending fulfillment decisions
     *
     * @param supplierOrgId The supplier organization ID
     * @param entityStatus Status to exclude
     * @return List of PENDING Sales Orders
     */
    List<SalesOrder> findBySupplierOrganizationIdAndStatusAndEntityStatusNot(
            Long supplierOrgId,
            SalesOrderStatus status,
            EntityStatus entityStatus
    );

    /**
     * Check if Sales Order exists for Purchase Order
     * Quick existence check without loading full entity
     *
     * @param purchaseOrderId The PO ID
     * @return true if SO exists
     */
    boolean existsByPurchaseOrderId(Long purchaseOrderId);

}
