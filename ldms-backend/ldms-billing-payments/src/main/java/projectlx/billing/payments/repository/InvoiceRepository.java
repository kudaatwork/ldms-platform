package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import projectlx.billing.payments.model.Invoice;
import projectlx.billing.payments.utils.enums.InvoiceSourceType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByOrganizationIdAndEntityStatusNotOrderByIssuedAtDesc(Long organizationId, EntityStatus entityStatus);

    Optional<Invoice> findByGrvIdAndEntityStatusNot(Long grvId, EntityStatus entityStatus);

    Optional<Invoice> findByPurchaseOrderIdAndSourceTypeAndEntityStatusNot(
            Long purchaseOrderId,
            InvoiceSourceType sourceType,
            EntityStatus entityStatus);

    Optional<Invoice> findByIdAndOrganizationIdAndEntityStatusNot(Long id, Long organizationId, EntityStatus entityStatus);

    Optional<Invoice> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    @Query("""
            SELECT COALESCE(SUM(i.totalBase), 0)
            FROM Invoice i
            WHERE i.entityStatus <> projectlx.co.zw.shared_library.utils.enums.EntityStatus.DELETED
              AND i.status IN (projectlx.billing.payments.utils.enums.InvoiceStatus.ISSUED,
                               projectlx.billing.payments.utils.enums.InvoiceStatus.PARTIALLY_PAID)
            """)
    BigDecimal sumPendingInvoiceBaseAmount();
}
