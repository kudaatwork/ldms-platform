package projectlx.co.zw.audittrail.utils.requests;

import java.time.LocalDateTime;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

public class AuditLogChurnHistoryFiltersRequest extends MultipleFiltersRequest {
    private String triggerType;
    private String status;
    private String triggeredBy;
    private String batchReference;
    private LocalDateTime from;
    private LocalDateTime to;

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }
}
