package projectlx.co.zw.notifications.utils.security;

public enum NotificationLogRoles {

    SEARCH_NOTIFICATION_LOGS("SEARCH_NOTIFICATION_LOGS", "Search notification delivery log with filters"),
    EXPORT_NOTIFICATION_LOGS("EXPORT_NOTIFICATION_LOGS", "Export notification delivery log");

    private final String roleName;
    private final String description;

    NotificationLogRoles(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }
}
