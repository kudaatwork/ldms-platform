package projectlx.billing.payments.utils.enums;

/**
 * Customer-facing fused pricing tier for prepaid wallet actions.
 * Light / Standard / Heavy simplify the rate card; Tracking and Messaging are specialised tiers.
 */
public enum PlatformBillingTier {
    LIGHT,
    STANDARD,
    HEAVY,
    TRACKING,
    MESSAGING
}
