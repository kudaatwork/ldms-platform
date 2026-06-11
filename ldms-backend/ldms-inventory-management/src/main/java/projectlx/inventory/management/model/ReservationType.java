package projectlx.inventory.management.model;

public enum ReservationType {
    SALES_ORDER("SALES_ORDER"),
    QUOTE("QUOTE"),
    MANUAL("MANUAL"),
    BLANKET_ORDER("BLANKET_ORDER"),
    CONSIGNMENT("CONSIGNMENT"),
    TRANSFER("TRANSFER"),
    SYSTEM("SYSTEM");        // System-generated reservation

    private final String description;

    ReservationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
