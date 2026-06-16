package projectlx.fleet.management.business.logic.support;

import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.utils.enums.ComplianceType;
import projectlx.fleet.management.utils.enums.FleetAssetType;
import projectlx.fleet.management.utils.enums.FleetOwnershipType;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Resolves which compliance documents are mandatory before a fleet asset
 * can transition from {@code PENDING_COMPLIANCE} to {@code ACTIVE}.
 *
 * <p>Requirements are generic for international use; the portal surfaces local examples
 * (e.g. ZINARA, VID, RMT, EMA) as hints only.</p>
 */
public final class FleetRequiredComplianceSupport {

    private FleetRequiredComplianceSupport() {}

    /** Always required for any commercial vehicle entering service. */
    public static final Set<ComplianceType> BASE_REQUIRED = Set.of(
            ComplianceType.VEHICLE_REGISTRATION,
            ComplianceType.ROAD_LICENSE,
            ComplianceType.ROADWORTHINESS,
            ComplianceType.INSURANCE,
            ComplianceType.GOODS_OPERATOR_LICENCE,
            ComplianceType.PERMIT
    );

    public static Set<ComplianceType> requiredForAsset(FleetAsset asset) {
        Set<ComplianceType> required = new LinkedHashSet<>(BASE_REQUIRED);

        if (asset.getOwnershipType() == FleetOwnershipType.CONTRACTED) {
            required.add(ComplianceType.LEASE_HIRE_AGREEMENT);
        }

        if (requiresHazardousCargoPermits(asset.getAssetType())) {
            required.add(ComplianceType.HAZARDOUS_SUBSTANCES_PERMIT);
            required.add(ComplianceType.FIRE_SAFETY_CLEARANCE);
        }

        if (hasAssignedDriver(asset)) {
            required.add(ComplianceType.LICENSE);
            required.add(ComplianceType.DEFENSIVE_DRIVING_CERTIFICATE);
            required.add(ComplianceType.DRIVER_MEDICAL_CERTIFICATE);
        }

        return required;
    }

    public static boolean requiresHazardousCargoPermits(FleetAssetType assetType) {
        return assetType == FleetAssetType.TANKER;
    }

    public static boolean hasAssignedDriver(FleetAsset asset) {
        return asset.getDriverName() != null && !asset.getDriverName().isBlank();
    }

    /** @deprecated Use {@link #requiredForAsset(FleetAsset)} — retained for tests referencing the legacy trio. */
    @Deprecated
    public static final Set<ComplianceType> REQUIRED_TYPES = BASE_REQUIRED;
}
