package projectlx.co.zw.audittrail.service.batch;

/** Keys stored in {@link org.springframework.batch.item.ExecutionContext} for churn job coordination. */
public final class AuditLogChurnJobKeys {

    public static final String CHURN_HISTORY_ID = "churnHistoryId";

    public static final String SNAPSHOT_TOTAL_ROWS = "snapshotTotalRows";

    private AuditLogChurnJobKeys() {}
}
