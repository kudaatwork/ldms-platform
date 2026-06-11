package projectlx.inventory.management.model;

public enum IdempotencyStatus {
    IN_PROGRESS("IN_PROGRESS"),
    PROCESSED("PROCESSED");

    private final String description;

    IdempotencyStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
