package projectlx.inventory.management.model;

/**
 * Status values for a Purchase Requisition throughout its lifecycle.
 * PRs follow a strict workflow from creation to closure.
 */
public enum PurchaseRequisitionStatus {
    DRAFT("Draft - Being prepared"),
    SUBMITTED("Submitted - Internal approval in progress"),
    APPROVED("Approved - Internal approval complete"),
    PUBLISHED_TO_SUPPLIER("Published - Visible to supplier"),
    SUPPLIER_CONFIRMED("Supplier confirmed availability and quoted"),
    CUSTOMER_ACKNOWLEDGED("Customer acknowledged supplier confirmation"),
    PARTIALLY_FULFILLED("Partially Fulfilled - Some items fulfilled"),
    FULFILLED("Fulfilled - All items fulfilled"),
    CLOSED("Closed - Administratively closed"),
    CANCELLED("Cancelled - Cancelled before or after approval"),
    REJECTED("Rejected - Approval rejected"),
    EXPIRED("Expired - Required by date passed");

    private final String description;

    PurchaseRequisitionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
