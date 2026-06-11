package projectlx.inventory.management.model;

/**
 * Priority levels for purchase requisitions.
 */
public enum PriorityLevel {
    LOW("Low Priority"),
    NORMAL("Normal Priority"),
    HIGH("High Priority"),
    URGENT("Urgent - Immediate attention required");

    private final String description;

    PriorityLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
