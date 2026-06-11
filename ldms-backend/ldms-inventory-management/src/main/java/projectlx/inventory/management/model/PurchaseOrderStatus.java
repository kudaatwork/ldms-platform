package projectlx.inventory.management.model;

public enum PurchaseOrderStatus {
    DRAFT("DRAFT"),
    SUBMITTED("SUBMITTED"),
    PENDING_CUSTOMER_APPROVAL("PENDING_CUSTOMER_APPROVAL"),
    CUSTOMER_APPROVED("CUSTOMER_APPROVED"),
    PENDING_SUPPLIER_APPROVAL("PENDING_SUPPLIER_APPROVAL"),
    APPROVED("APPROVED"),
    PARTIALLY_RECEIVED("PARTIALLY_RECEIVED"),
    RECEIVED("RECEIVED"),
    CANCELLED("CANCELLED"),
    REJECTED("REJECTED");

    private final String description;

    PurchaseOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
