package projectlx.inventory.management.model;

/**
 * Represents the type of idempotent operation being performed.
 */
public enum IdempotencyOperation {
    // Purchase Order operations
    PURCHASE_ORDER_CREATE("PURCHASE_ORDER_CREATE"),
    PURCHASE_ORDER_UPDATE("PURCHASE_ORDER_UPDATE"),
    RECEIVE_GOODS("RECEIVE_GOODS"),

    // Sales Order operations
    SALES_ORDER_CREATE("SALES_ORDER_CREATE"),
    SALES_ORDER_CONFIRM("SALES_ORDER_CONFIRM"),
    FULFILL_ORDER("FULFILL_ORDER"),
    SALES_ORDER_CANCEL("SALES_ORDER_CANCEL"),

    // Inventory Transfer operations
    COMPLETE_TRANSFER("COMPLETE_TRANSFER"),
    COMPLETE_TRANSFER_WITH_GRV("COMPLETE_TRANSFER_WITH_GRV"),

    // Stock Adjustment operations
    STOCK_ADJUSTMENT_CREATE("STOCK_ADJUSTMENT_CREATE");

    private final String description;

    IdempotencyOperation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
