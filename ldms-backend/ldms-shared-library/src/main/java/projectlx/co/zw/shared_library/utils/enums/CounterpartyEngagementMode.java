package projectlx.co.zw.shared_library.utils.enums;

/**
 * How a supplier or customer engages with trading counterparties when not in standalone org mode.
 *
 * <ul>
 *   <li>{@link #RECORD_ONLY} — register counterparties for delivery records only; they do not log in.</li>
 *   <li>{@link #PLATFORM_ORG} — register new platform organisations or link existing ones for bilateral workflows.</li>
 * </ul>
 */
public enum CounterpartyEngagementMode {
    RECORD_ONLY,
    PLATFORM_ORG
}
