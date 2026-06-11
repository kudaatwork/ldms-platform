package projectlx.inventory.management.model;

public enum OutboxStatus {
    PENDING("PENDING"),
    SENT("SENT");

    private final String description;

    OutboxStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
