package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.inventory.management.model.PurchaseRequisitionAmendment;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;

/**
 * Repository interface for Purchase Requisition Amendment entity.
 */
public interface PurchaseRequisitionAmendmentRepository extends JpaRepository<PurchaseRequisitionAmendment, Long> {

    /**
     * Find all amendments for a specific PR
     */
    List<PurchaseRequisitionAmendment> findByPurchaseRequisitionIdAndEntityStatusNot(
            Long purchaseRequisitionId,
            EntityStatus entityStatus
    );

    /**
     * Find amendments ordered by amendment number
     */
    @Query("SELECT pra FROM PurchaseRequisitionAmendment pra " +
           "WHERE pra.purchaseRequisition.id = :prId " +
           "AND pra.entityStatus != :entityStatus " +
           "ORDER BY pra.amendmentNumber ASC")
    List<PurchaseRequisitionAmendment> findByPurchaseRequisitionOrderByAmendmentNumber(
            @Param("prId") Long prId,
            @Param("entityStatus") EntityStatus entityStatus
    );

    /**
     * Get next amendment number for a PR
     */
    @Query("SELECT COALESCE(MAX(pra.amendmentNumber), 0) + 1 FROM PurchaseRequisitionAmendment pra " +
           "WHERE pra.purchaseRequisition.id = :prId")
    Integer getNextAmendmentNumber(@Param("prId") Long prId);
}
