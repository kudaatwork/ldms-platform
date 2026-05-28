package projectlx.co.zw.organizationmanagement.business.logic.support;

import org.springframework.util.StringUtils;

/**
 * Shared helpers for organisation notification recipient addresses.
 */
public final class OrganizationNotificationEmailSupport {

    private OrganizationNotificationEmailSupport() {
    }

    public static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    public static boolean isSameEmail(String first, String second) {
        String normalizedFirst = normalizeEmail(first);
        String normalizedSecond = normalizeEmail(second);
        return StringUtils.hasText(normalizedFirst) && normalizedFirst.equals(normalizedSecond);
    }
}
