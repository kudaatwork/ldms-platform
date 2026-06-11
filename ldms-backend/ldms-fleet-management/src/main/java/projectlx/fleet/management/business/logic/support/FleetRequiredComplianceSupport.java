package projectlx.fleet.management.business.logic.support;

import projectlx.fleet.management.utils.enums.ComplianceType;

import java.util.Set;

/**
 * Defines the compliance types that are mandatory before a fleet asset
 * can transition from PENDING_COMPLIANCE to ACTIVE.
 *
 * All three documents must be provided in the complete-registration call:
 * INSURANCE, ROADWORTHINESS, PERMIT.
 */
public final class FleetRequiredComplianceSupport {

    private FleetRequiredComplianceSupport() {}

    public static final Set<ComplianceType> REQUIRED_TYPES = Set.of(
            ComplianceType.INSURANCE,
            ComplianceType.ROADWORTHINESS,
            ComplianceType.PERMIT
    );
}
