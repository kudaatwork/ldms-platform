package projectlx.co.zw.notifications.business.auditable.api;

import projectlx.co.zw.notifications.model.NotificationLog;

public interface NotificationLogServiceAuditable {
    NotificationLog create(NotificationLog notificationLog);
    NotificationLog update(NotificationLog notificationLog);
    NotificationLog delete(NotificationLog notificationLog);
}
