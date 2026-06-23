package projectlx.billing.payments.utils.enums;

/**
 * Customer-facing pricing tier for prepaid wallet actions.
 * INCLUDED = subscription-bundled admin; MILESTONE = transactional fees;
 * TRACKING / TELEMETRY / MESSAGING = premium add-ons.
 * LIGHT / STANDARD / HEAVY retained for legacy rows only.
 */
public enum PlatformBillingTier {
    INCLUDED,
    MILESTONE,
    LIGHT,
    STANDARD,
    HEAVY,
    TRACKING,
    TELEMETRY,
    MESSAGING
}
