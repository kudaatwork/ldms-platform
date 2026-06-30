package projectlx.co.zw.notifications.service.processor.api;

import projectlx.co.zw.notifications.utils.responses.PlatformUserNotificationResponse;

import java.util.Locale;

public interface PlatformUserNotificationProcessor {

    PlatformUserNotificationResponse listInbox(Locale locale, String username);

    PlatformUserNotificationResponse dismiss(Long notificationId, Locale locale, String username);

    PlatformUserNotificationResponse dismissAll(Locale locale, String username);
}
