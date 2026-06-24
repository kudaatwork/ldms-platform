package projectlx.user.management.utils.support;

import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * Defines which {@code user_role} rows are applicable to which
 * {@code OrganizationClassification} values.
 *
 * <p>A role with <strong>no</strong> classifications is considered platform-only
 * (admin portal). A role with one or more classifications is available to
 * organisation workspaces of those types.
 *
 * <p>The platform {@code Administrator} group (organisation_id IS NULL) receives
 * the full catalog. Organisation-scoped {@code Administrator} groups receive only
 * roles whose classifications include the organisation's classification.
 */
public final class RoleClassificationPolicy {

    private RoleClassificationPolicy() {
    }

    /** All known organisation classifications. */
    public static final Set<String> ALL_CLASSIFICATIONS = Set.of(
            "SUPPLIER",
            "CUSTOMER",
            "SERVICE_STATION",
            "ROADSIDE_SUPPORT_SERVICE",
            "TRANSPORT_COMPANY",
            "CLEARING_AGENT",
            "GOVERNMENT_AGENCY"
    );

    /**
     * Returns the set of organisation classifications a role applies to.
     * An empty set means the role is platform-only.
     */
    public static Set<String> classificationsForRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return Set.of();
        }
        String role = roleCode.trim().toUpperCase(Locale.ROOT);

        // Platform-only roles (admin portal, no organisation workspace)
        if (isPlatformOnlyRole(role)) {
            return Set.of();
        }

        // All other roles are applicable to every organisation classification
        return ALL_CLASSIFICATIONS;
    }

    /**
     * Checks whether a role is applicable to a given organisation classification.
     */
    public static boolean isApplicableToClassification(String roleCode, String classification) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }
        Set<String> classifications = classificationsForRole(roleCode);
        if (classifications.isEmpty()) {
            return false;
        }
        if (!StringUtils.hasText(classification)) {
            return false;
        }
        return classifications.contains(classification.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Returns true if the role is strictly platform-only (no organisation classification).
     */
    public static boolean isPlatformOnlyRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return true;
        }
        String role = roleCode.trim().toUpperCase(Locale.ROOT);
        return Set.of(
                "ADMIN",
                "READ_ONLY",
                "KYC_STAGE1",
                "KYC_STAGE2",
                "KYC_STAGE3",
                "KYC_STAGE4",
                "KYC_STAGE5",
                "CHURN_OUT_AUDIT_LOGS"
        ).contains(role);
    }
}
