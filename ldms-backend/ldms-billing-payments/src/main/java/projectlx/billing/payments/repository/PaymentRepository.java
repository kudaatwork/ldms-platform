package projectlx.billing.payments.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.billing.payments.model.Payment;
import projectlx.billing.payments.utils.enums.PaymentRecordStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceIdAndEntityStatusNotOrderByPaymentDateDesc(Long invoiceId, EntityStatus entityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    @Query("""
            SELECT p FROM Payment p
            INNER JOIN Invoice i ON i.id = p.invoiceId
            WHERE p.status = :status
              AND p.entityStatus <> projectlx.co.zw.shared_library.utils.enums.EntityStatus.DELETED
              AND i.entityStatus <> projectlx.co.zw.shared_library.utils.enums.EntityStatus.DELETED
              AND i.supplierId = :supplierOrganizationId
            ORDER BY p.createdAt DESC
            """)
    List<Payment> findPendingProcurementForSupplier(
            @Param("supplierOrganizationId") Long supplierOrganizationId,
            @Param("status") PaymentRecordStatus status);
}
