package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.Invoice;
import projectlx.billing.payments.utils.enums.InvoiceSourceType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

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
}
