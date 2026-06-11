package projectlx.inventory.management.model;

/**
 * Enumeration for different types of business documents that can reference stock transactions
 */
public enum ReferenceDocumentType {
    PURCHASE_ORDER("PURCHASE_ORDER"),
    SALES_ORDER("SALES_ORDER"),
    GOODS_RECEIVED_VOUCHER("GOODS_RECEIVED_VOUCHER"),
    PURCHASE_RETURN("PURCHASE_RETURN"),
    SALES_RETURN("SALES_RETURN"),
    STOCK_ADJUSTMENT("STOCK_ADJUSTMENT"),
    INVENTORY_TRANSFER("INVENTORY_TRANSFER"),
    OPENING_BALANCE("OPENING_BALANCE");

    private final String displayName;

    ReferenceDocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get enum from display name for backward compatibility
     */
    public static ReferenceDocumentType fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (ReferenceDocumentType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No ReferenceDocumentType with display name: " + displayName);
    }
}