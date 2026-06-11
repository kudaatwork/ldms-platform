package projectlx.co.zw.organizationmanagement.utils.enums;

/**
 * Outcome when a supplier checks whether a customer registration email can be used.
 */
public enum CustomerRegistrationEmailStatus {
    /** Email is not registered — create a new customer organisation. */
    AVAILABLE,
    /** Target is a customer org and can be linked to this supplier. */
    LINKABLE_CUSTOMER,
    /** Target is a supplier org — duplex link can be offered (buy and sell). */
    DUPLEX_OFFERED,
    /** Target is already linked as a customer of this supplier. */
    ALREADY_LINKED,
    /** Email belongs to an organisation that cannot be linked (e.g. transport-only). */
    NOT_LINKABLE
}
