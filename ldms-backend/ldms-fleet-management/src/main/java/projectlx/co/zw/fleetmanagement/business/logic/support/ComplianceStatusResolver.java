package projectlx.co.zw.fleetmanagement.business.logic.support;

import projectlx.co.zw.fleetmanagement.utils.enums.ComplianceStatus;

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
