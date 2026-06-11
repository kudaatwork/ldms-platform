package projectlx.inventory.management.model;

public enum ReservationStatus {
    ACTIVE("ACTIVE"),
    FULFILLED("FULFILLED"),
    CONSUMED("CONSUMED"),
    CANCELLED("CANCELLED"),
    EXPIRED("EXPIRED");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
