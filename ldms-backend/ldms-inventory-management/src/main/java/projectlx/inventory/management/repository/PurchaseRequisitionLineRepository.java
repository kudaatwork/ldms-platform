package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.inventory.management.model.FulfillmentMethod;
import projectlx.inventory.management.model.PurchaseRequisitionLine;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;

/**
 * Repository interface for Purchase Requisition Line entity.
 */
public interface PurchaseRequisitionLineRepository extends
        JpaRepository<PurchaseRequisitionLine, Long>,
        JpaSpecificationExecutor<PurchaseRequisitionLine> {

    /**
     * Find all lines for a specific PR
     */
    List<PurchaseRequisitionLine> findByPurchaseRequisitionIdAndEntityStatusNot(
            Long purchaseRequisitionId,
            EntityStatus entityStatus
    );

    /**
     * Find lines by fulfillment method for a PR
     */
    List<PurchaseRequisitionLine> findByPurchaseRequisitionIdAndFulfillmentMethodAndEntityStatusNot(
            Long purchaseRequisitionId,
            FulfillmentMethod fulfillmentMethod,
            EntityStatus entityStatus
    );

    /**
     * Find lines with remaining quantity to fulfill
     */
    @Query("SELECT prl FROM PurchaseRequisitionLine prl " +
           "WHERE prl.purchaseRequisition.id = :prId " +
           "AND prl.remainingQuantity > 0 " +
           "AND prl.entityStatus != :entityStatus")
    List<PurchaseRequisitionLine> findLinesWithRemainingQuantity(
            @Param("prId") Long prId,
            @Param("entityStatus") EntityStatus entityStatus
    );

    /**
     * Find lines eligible for PO creation
     */
    @Query("SELECT prl FROM PurchaseRequisitionLine prl " +
           "WHERE prl.purchaseRequisition.id = :prId " +
           "AND prl.fulfillmentMethod = 'PURCHASE' " +
           "AND prl.remainingQuantity > 0 " +
           "AND prl.entityStatus != :entityStatus " +
           "ORDER BY prl.lineNumber ASC")
    List<PurchaseRequisitionLine> findLinesEligibleForPurchaseOrder(
            @Param("prId") Long prId,
            @Param("entityStatus") EntityStatus entityStatus
    );

    /**
     * Find lines by product for stock availability check
     */
    List<PurchaseRequisitionLine> findByProductIdAndEntityStatusNot(
            Long productId,
            EntityStatus entityStatus
    );
}
