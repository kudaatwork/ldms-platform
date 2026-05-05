package projectlx.co.zw.audittrail.utils.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ldms.audit")
public class LdmsAuditProperties {

    /**
     * When true, would allow this service to emit audit events (avoid — causes recursive audit of inserts).
     */
    private boolean enabled = false;

    private final Churn churn = new Churn();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Churn getChurn() {
        return churn;
    }

    public static class Churn {

        /** When false, scheduled churn job triggers are no-ops (manual UI/API still allowed unless gated elsewhere). */
        private boolean schedulerEnabled = true;

        /** Chunk size for bulk deletes inside Spring Batch (IDs per transaction). */
        private int chunkSize = 10_000;

        /** Async batch JobLauncher thread pool size. */
        private int jobPoolSize = 4;

        public boolean isSchedulerEnabled() {
            return schedulerEnabled;
        }

        public void setSchedulerEnabled(boolean schedulerEnabled) {
            this.schedulerEnabled = schedulerEnabled;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getJobPoolSize() {
            return jobPoolSize;
        }

        public void setJobPoolSize(int jobPoolSize) {
            this.jobPoolSize = jobPoolSize;
        }
    }
}
