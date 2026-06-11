package projectlx.inventory.management.model;

public enum TransferStatus {
    REQUESTED("REQUESTED"),
    APPROVED("APPROVED"),
    IN_TRANSIT("IN_TRANSIT"),
    COMPLETED("COMPLETED"),
    REJECTED("REJECTED"),
    CANCELLED("CANCELLED");

    private final String description;

    TransferStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
