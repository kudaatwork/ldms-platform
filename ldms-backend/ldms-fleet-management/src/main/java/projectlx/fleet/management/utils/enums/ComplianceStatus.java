package projectlx.fleet.management.utils.enums;

public enum ComplianceStatus {
    VALID,
    EXPIRING_SOON,
    EXPIRED,
    PENDING,
    /** Document rejected during manual compliance review. */
    REVOKED
}
