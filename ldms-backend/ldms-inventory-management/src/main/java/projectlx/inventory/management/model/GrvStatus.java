package projectlx.inventory.management.model;

public enum GrvStatus {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String description;

    GrvStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
