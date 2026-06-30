package projectlx.co.zw.organizationmanagement.utils.enums;

/**
 * Lifecycle of a supplier ↔ transporter contract link.
 */
public enum TransporterLinkStatus {
    /** Supplier has sent an offer; the transporter has not yet responded. */
    PENDING,
    /** Transporter accepted the offer (or the link was created directly, e.g. register-new). */
    ACCEPTED,
    /** Transporter declined the offer. */
    DECLINED
}
