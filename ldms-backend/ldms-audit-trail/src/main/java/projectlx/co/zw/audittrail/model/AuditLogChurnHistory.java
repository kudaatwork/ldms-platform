package projectlx.co.zw.audittrail.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import projectlx.co.zw.audittrail.utils.enums.AuditLogChurnStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Entity
@Table(name = "audit_log_churn_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogChurnHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_reference", nullable = false, length = 64)
    private String batchReference;

    @Column(name = "trigger_type", nullable = false, length = 50)
    private String triggerType;

    @Column(name = "triggered_by", nullable = false, length = 255)
    private String triggeredBy;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "deleted_log_count", nullable = false)
    private Long deletedLogCount;

    @Column(name = "oldest_request_timestamp")
    private LocalDateTime oldestRequestTimestamp;

    @Column(name = "newest_request_timestamp")
    private LocalDateTime newestRequestTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "churn_status", nullable = false, length = 50)
    private AuditLogChurnStatus churnStatus;

    @Column(name = "job_execution_id")
    private Long jobExecutionId;

    @Column(name = "failure_reason", length = 2500)
    private String failureReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", nullable = false, length = 255)
    private String modifiedBy;
}
