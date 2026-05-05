package projectlx.co.zw.audittrail.repository;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.audittrail.model.AuditLogChurnHistory;
import projectlx.co.zw.audittrail.utils.enums.AuditLogChurnStatus;

public interface AuditLogChurnHistoryRepository extends JpaRepository<AuditLogChurnHistory, Long> {
    Page<AuditLogChurnHistory> findAllByOrderByTriggeredAtDesc(Pageable pageable);

    @Query(
            value = "SELECT h FROM AuditLogChurnHistory h WHERE "
                    + "(:triggerType IS NULL OR LOWER(h.triggerType) = LOWER(:triggerType)) AND "
                    + "(:status IS NULL OR h.churnStatus = :status) AND "
                    + "(:triggeredBy IS NULL OR LOWER(h.triggeredBy) LIKE LOWER(CONCAT('%', :triggeredBy, '%'))) AND "
                    + "(:batchReference IS NULL OR LOWER(h.batchReference) LIKE LOWER(CONCAT('%', :batchReference, '%'))) AND "
                    + "(:fromTs IS NULL OR h.triggeredAt >= :fromTs) AND "
                    + "(:toTs IS NULL OR h.triggeredAt <= :toTs)")
    Page<AuditLogChurnHistory> search(
            @Param("triggerType") String triggerType,
            @Param("status") AuditLogChurnStatus status,
            @Param("triggeredBy") String triggeredBy,
            @Param("batchReference") String batchReference,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            Pageable pageable);
}
