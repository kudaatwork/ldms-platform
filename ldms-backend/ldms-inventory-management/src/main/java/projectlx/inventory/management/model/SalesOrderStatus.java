package projectlx.inventory.management.model;

/**
 * Sales Order Status Enum
 *
 * BACK-TO-BACK FULFILLMENT FLOW:
 * 1. PO Approved → SO created in AWAITING_RECEIPT status
 * 2. Goods Received (GRV created) → SO transitions to PENDING
 * 3. Supplier confirms availability → SO transitions to CONFIRMED (stock reserved)
 * 4. Shipment dispatched → PARTIALLY_SHIPPED / SHIPPED
 * 5. Delivery confirmed → DELIVERED → FULFILLED
 *
 * KEY RULE: SO cannot be CONFIRMED until goods are physically received.
 * This prevents reserving stock that doesn't exist yet.
 */
public enum SalesOrderStatus {
    /**
     * SO created from PO approval, awaiting goods receipt
     * Stock does NOT exist yet - waiting for GRV to confirm physical receipt
     */
    AWAITING_RECEIPT("AWAITING_RECEIPT"),

    /**
     * Goods have been received (GRV created), awaiting supplier confirmation
     * Stock now exists but is not yet reserved for this SO
     */
    PENDING("PENDING"),

    /**
     * Supplier confirmed they can fulfill - stock is now RESERVED
     * This is when inventory reservation happens
     */
    CONFIRMED("CONFIRMED"),

    PENDING_APPROVAL("PENDING_APPROVAL"),
    APPROVED("APPROVED"),

    PARTIALLY_SHIPPED("PARTIALLY_SHIPPED"),
    SHIPPED("SHIPPED"),
    DELIVERED("DELIVERED"),
    FULFILLED("FULFILLED"),
    CANCELLED("CANCELLED");

    private final String description;

    SalesOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
