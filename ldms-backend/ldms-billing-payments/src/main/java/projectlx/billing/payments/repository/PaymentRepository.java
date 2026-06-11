package projectlx.billing.payments.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import projectlx.billing.payments.model.Payment;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceIdAndEntityStatusNotOrderByPaymentDateDesc(Long invoiceId, EntityStatus entityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
}
