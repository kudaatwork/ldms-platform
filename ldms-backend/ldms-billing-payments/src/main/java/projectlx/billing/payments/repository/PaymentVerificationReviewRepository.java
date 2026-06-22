package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.PaymentVerificationReview;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;

public interface PaymentVerificationReviewRepository extends JpaRepository<PaymentVerificationReview, Long> {

    List<PaymentVerificationReview> findByPaymentIdAndEntityStatusNotOrderByStageNumberAsc(
            Long paymentId, EntityStatus entityStatus);

    boolean existsByPaymentIdAndReviewedByUsernameAndEntityStatusNot(
            Long paymentId, String reviewedByUsername, EntityStatus entityStatus);
}
