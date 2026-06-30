package projectlx.co.zw.notifications.business.auditable.api;

import projectlx.co.zw.notifications.model.PlatformUserNotification;

import java.util.Locale;

public interface PlatformUserNotificationServiceAuditable {

    PlatformUserNotification create(PlatformUserNotification notification, Locale locale, String username);

    PlatformUserNotification update(PlatformUserNotification notification, Locale locale, String username);
}
