package projectlx.inventory.management.model;

/**
 * Defines how a purchase requisition or line item will be fulfilled.
 */
public enum FulfillmentMethod {
    PURCHASE("Purchase - Create PO from supplier"),
    FROM_STOCK("From Stock - Fulfill from existing inventory"),
    TRANSFER("Transfer - Internal stock transfer between warehouses"),
    DEFERRED("Deferred - To be procured later"),
    NOT_REQUIRED("Not Required - No longer needed");

    private final String description;

    FulfillmentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
