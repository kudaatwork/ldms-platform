package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.inventory.management.model.SupplierQuote;
import projectlx.inventory.management.model.SupplierQuoteStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface SupplierQuoteRepository extends JpaRepository<SupplierQuote, Long> {

    Optional<SupplierQuote> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<SupplierQuote> findByPurchaseRequisitionIdAndStatusAndEntityStatusNot(
            Long purchaseRequisitionId, SupplierQuoteStatus status, EntityStatus entityStatus);

    List<SupplierQuote> findByPurchaseRequisitionIdAndEntityStatusNotOrderByCreatedAtDesc(
            Long purchaseRequisitionId, EntityStatus entityStatus);

    /**
     * Find all quotes submitted by a supplier organisation, newest first.
     */
    List<SupplierQuote> findBySupplierOrganizationIdAndEntityStatusNotOrderBySubmittedAtDesc(
            Long supplierOrganizationId, EntityStatus entityStatus);

    /**
     * Find all quotes received by a customer organisation, newest first.
     */
    List<SupplierQuote> findByCustomerOrganizationIdAndEntityStatusNotOrderBySubmittedAtDesc(
            Long customerOrganizationId, EntityStatus entityStatus);
}
