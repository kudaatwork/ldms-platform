package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.inventory.management.model.ProcurementApprovalDocumentType;
import projectlx.inventory.management.model.ProcurementApprovalReview;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;

public interface ProcurementApprovalReviewRepository extends JpaRepository<ProcurementApprovalReview, Long> {

    List<ProcurementApprovalReview> findByDocumentTypeAndDocumentIdAndEntityStatusNotOrderByStageNumberAsc(
            ProcurementApprovalDocumentType documentType,
            Long documentId,
            EntityStatus entityStatus);

    int countByDocumentTypeAndDocumentIdAndDecisionAndEntityStatusNot(
            ProcurementApprovalDocumentType documentType,
            Long documentId,
            String decision,
            EntityStatus entityStatus);
}
