package projectlx.fleet.management.business.logic.support;

import projectlx.fleet.management.utils.enums.ComplianceStatus;

import java.time.LocalDateTime;

public final class ComplianceStatusResolver {

    private ComplianceStatusResolver() {}

    public static ComplianceStatus resolve(LocalDateTime expiresAt, Long fileUploadId, int expiringSoonDays) {
        if (fileUploadId == null && expiresAt == null) {
            return ComplianceStatus.PENDING;
        }
        if (expiresAt == null) {
            return ComplianceStatus.PENDING;
        }
        LocalDateTime now = LocalDateTime.now();
        if (expiresAt.isBefore(now)) {
            return ComplianceStatus.EXPIRED;
        }
        if (!expiresAt.isAfter(now.plusDays(expiringSoonDays))) {
            return ComplianceStatus.EXPIRING_SOON;
        }
        return ComplianceStatus.VALID;
    }
}
