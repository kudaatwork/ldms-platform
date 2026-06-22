package projectlx.billing.payments.business.logic.support;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.model.PaymentVerificationReview;
import projectlx.billing.payments.repository.PaymentVerificationReviewRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class PaymentVerificationSupport {

    private final PaymentVerificationReviewRepository paymentVerificationReviewRepository;

    public record StageVerificationResult(int completedStage, boolean allStagesComplete, int requiredStages) {}

    public StageVerificationResult recordVerificationStage(Long paymentId,
                                                           int currentStage,
                                                           int requiredStages,
                                                           Long reviewedByUserId,
                                                           String reviewedByUsername) {
        int nextStage = currentStage + 1;
        LocalDateTime now = LocalDateTime.now();
        PaymentVerificationReview review = new PaymentVerificationReview();
        review.setPaymentId(paymentId);
        review.setStageNumber(nextStage);
        review.setReviewedByUserId(reviewedByUserId);
        review.setReviewedByUsername(reviewedByUsername);
        review.setReviewedAt(now);
        review.setEntityStatus(EntityStatus.ACTIVE);
        review.setCreatedAt(now);
        review.setCreatedBy(reviewedByUsername);
        paymentVerificationReviewRepository.save(review);
        return new StageVerificationResult(nextStage, nextStage >= requiredStages, requiredStages);
    }

    public boolean hasUserAlreadyVerified(Long paymentId, String username) {
        return paymentVerificationReviewRepository.existsByPaymentIdAndReviewedByUsernameAndEntityStatusNot(
                paymentId, username, EntityStatus.DELETED);
    }
}
