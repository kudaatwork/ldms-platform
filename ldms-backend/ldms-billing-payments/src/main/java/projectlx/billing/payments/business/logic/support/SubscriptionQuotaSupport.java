package projectlx.billing.payments.business.logic.support;

import org.springframework.util.StringUtils;
import projectlx.billing.payments.model.PlatformActionCharge;
import projectlx.billing.payments.model.SubscriptionPackage;
import projectlx.billing.payments.utils.enums.PlatformBillingTier;

import java.util.List;

/**
 * Maps a platform action's billing tier to the subscription package quota it consumes
 * (milestone / messaging / tracking-day credits) so every package attribute is enforced,
 * not just SMS. Once a dimension's monthly quota is exhausted, the action falls through to
 * pay-as-you-go prepaid wallet billing — the same overflow behaviour already used for SMS.
 */
public final class SubscriptionQuotaSupport {

    /** A subscription quota bucket backed by a configurable package credit field. */
    public enum Dimension {
        MESSAGING,
        MILESTONE,
        TRACKING,
        NONE
    }

    private SubscriptionQuotaSupport() {
    }

    /**
     * Resolves the effective tier for a charge: the catalog value when present, otherwise
     * inferred from well-known action codes so legacy/untiered rows are still enforced.
     */
    public static PlatformBillingTier effectiveTier(PlatformActionCharge catalog) {
        if (catalog == null) {
            return null;
        }
        if (catalog.getBillingTier() != null) {
            return catalog.getBillingTier();
        }
        String code = catalog.getActionCode() == null ? "" : catalog.getActionCode().trim().toUpperCase();
        if (isTrackingCode(code)) {
            return PlatformBillingTier.TRACKING;
        }
        if (SubscriptionMessagingQuotaSupport.isMessagingAction(code)) {
            return PlatformBillingTier.MESSAGING;
        }
        return null;
    }

    public static Dimension dimensionForTier(PlatformBillingTier tier) {
        if (tier == null) {
            return Dimension.NONE;
        }
        return switch (tier) {
            case MESSAGING, STANDARD -> Dimension.MESSAGING;
            case MILESTONE, HEAVY -> Dimension.MILESTONE;
            case TRACKING -> Dimension.TRACKING;
            // INCLUDED, LIGHT and TELEMETRY have no dedicated package credit bucket.
            default -> Dimension.NONE;
        };
    }

    /** Monthly included credits the package grants for the given dimension. */
    public static int quotaFor(Dimension dimension, SubscriptionPackage pkg) {
        if (pkg == null) {
            return 0;
        }
        return switch (dimension) {
            case MESSAGING -> nonNegative(pkg.getIncludedStandardCredits());
            case MILESTONE -> nonNegative(pkg.getIncludedHeavyCredits());
            case TRACKING -> nonNegative(pkg.getIncludedTrackingDayCredits());
            case NONE -> 0;
        };
    }

    /** Tiers whose usage records count against the dimension's quota (incl. legacy aliases). */
    public static List<PlatformBillingTier> tiersFor(Dimension dimension) {
        return switch (dimension) {
            case MESSAGING -> List.of(PlatformBillingTier.MESSAGING, PlatformBillingTier.STANDARD);
            case MILESTONE -> List.of(PlatformBillingTier.MILESTONE, PlatformBillingTier.HEAVY);
            case TRACKING -> List.of(PlatformBillingTier.TRACKING);
            case NONE -> List.of();
        };
    }

    public static boolean isQuotaExhausted(int quota, long used) {
        return quota > 0 && used >= quota;
    }

    private static boolean isTrackingCode(String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        return "TRIP_TRACK".equals(code) || "GPS_PING".equals(code) || "LIVE_MAP_SESSION".equals(code);
    }

    private static int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }
}
