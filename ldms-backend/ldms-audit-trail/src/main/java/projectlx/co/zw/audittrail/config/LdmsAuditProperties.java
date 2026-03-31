package projectlx.co.zw.audittrail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ldms.audit")
public class LdmsAuditProperties {

    /**
     * When true, would allow this service to emit audit events (avoid — causes recursive audit of inserts).
     */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
