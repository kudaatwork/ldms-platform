package projectlx.co.zw.notifications.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum NotificationLogRoles {

    SEARCH_NOTIFICATION_LOGS("SEARCH_NOTIFICATION_LOGS", "Search notification delivery log with filters"),
    EXPORT_NOTIFICATION_LOGS("EXPORT_NOTIFICATION_LOGS", "Export notification delivery log");

    private final String roleName;
    private final String description;
}
