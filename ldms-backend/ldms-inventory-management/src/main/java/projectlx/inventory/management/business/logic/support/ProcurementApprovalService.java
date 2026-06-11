package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.model.ProcurementApprovalDocumentType;
import projectlx.inventory.management.model.ProcurementApprovalReview;
import projectlx.inventory.management.repository.ProcurementApprovalReviewRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcurementApprovalService {

    private final ProcurementApprovalReviewRepository procurementApprovalReviewRepository;

    public record StageApprovalResult(int completedStage, boolean allStagesComplete, int requiredStages) {}

    public StageApprovalResult recordApproval(ProcurementApprovalDocumentType documentType,
                                              Long documentId,
                                              int currentStage,
                                              int requiredStages,
                                              Long reviewedByUserId,
                                              String reviewedByUsername,
                                              String notes) {
        int nextStage = currentStage + 1;
        ProcurementApprovalReview review = new ProcurementApprovalReview();
        review.setDocumentType(documentType);
        review.setDocumentId(documentId);
        review.setStageNumber(nextStage);
        review.setDecision("APPROVED");
        review.setReviewedByUserId(reviewedByUserId);
        review.setReviewedByUsername(reviewedByUsername);
        review.setReviewedAt(LocalDateTime.now());
        review.setNotes(notes);
        review.setEntityStatus(EntityStatus.ACTIVE);
        review.setCreatedBy(reviewedByUsername);
        procurementApprovalReviewRepository.save(review);

        boolean complete = nextStage >= requiredStages;
        return new StageApprovalResult(nextStage, complete, requiredStages);
    }

    public void recordRejection(ProcurementApprovalDocumentType documentType,
                                Long documentId,
                                int stageNumber,
                                Long reviewedByUserId,
                                String reviewedByUsername,
                                String notes) {
        ProcurementApprovalReview review = new ProcurementApprovalReview();
        review.setDocumentType(documentType);
        review.setDocumentId(documentId);
        review.setStageNumber(stageNumber);
        review.setDecision("REJECTED");
        review.setReviewedByUserId(reviewedByUserId);
        review.setReviewedByUsername(reviewedByUsername);
        review.setReviewedAt(LocalDateTime.now());
        review.setNotes(notes);
        review.setEntityStatus(EntityStatus.ACTIVE);
        review.setCreatedBy(reviewedByUsername);
        procurementApprovalReviewRepository.save(review);
    }
}
