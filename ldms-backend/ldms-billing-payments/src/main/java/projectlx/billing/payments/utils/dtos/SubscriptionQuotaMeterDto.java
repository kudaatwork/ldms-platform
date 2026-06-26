package projectlx.billing.payments.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Per-attribute subscription quota usage (e.g. milestone, messaging, tracking-day credits)
 * surfaced to organisations so they can see how much of each included bucket remains.
 */
@Getter
@Setter
@ToString
public class SubscriptionQuotaMeterDto {
    /** Stable dimension code: MESSAGING | MILESTONE | TRACKING. */
    private String code;
    /** Human-friendly label, e.g. "Milestone events". */
    private String label;
    /** Monthly included credits configured on the package. */
    private Integer includedMonthly;
    /** Credits consumed in the current billing period. */
    private Long usedThisPeriod;
    /** Credits still included this period. */
    private Integer remainingThisPeriod;
    /** True when the included quota is fully consumed. */
    private Boolean exhausted;
}
