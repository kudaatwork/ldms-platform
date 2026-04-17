package projectlx.co.zw.notifications.business.auditable.api;

import java.util.Locale;
import projectlx.co.zw.notifications.model.NotificationTemplate;

public interface NotificationTemplateServiceAuditable {
    NotificationTemplate create(NotificationTemplate template, Locale locale, String username);

    NotificationTemplate update(NotificationTemplate template, Locale locale, String username);

    NotificationTemplate delete(NotificationTemplate template, Locale locale, String username);
}
