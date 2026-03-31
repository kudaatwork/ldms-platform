package projectlx.co.zw.notificationsmanagementservice.business.auditable.api;

import projectlx.co.zw.notificationsmanagementservice.model.NotificationLog;
import java.util.Locale;

public interface NotificationLogServiceAuditable {
    NotificationLog create(NotificationLog notificationLog);
    NotificationLog update(NotificationLog notificationLog);
    NotificationLog delete(NotificationLog notificationLog);
}
