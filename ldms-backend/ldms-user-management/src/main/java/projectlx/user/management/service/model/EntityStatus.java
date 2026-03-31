package projectlx.user.management.service.model;

public enum EntityStatus {
    ACTIVE("ACTIVE"),      // The entity is active and operational
    INACTIVE("INACTIVE"),    // Temporarily disabled but not deleted
    SUSPENDED("SUSPENDED"),   // Blocked due to some policy violation
    DELETED("DELETED");      // Soft deletion (not physically removed)

    private String status;

    EntityStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
