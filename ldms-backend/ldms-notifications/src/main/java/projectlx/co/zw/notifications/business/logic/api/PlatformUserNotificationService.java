package projectlx.co.zw.notifications.business.logic.api;

import projectlx.co.zw.notifications.utils.responses.PlatformUserNotificationResponse;
import projectlx.co.zw.shared_library.utils.requests.PlatformBellNotificationRequest;

import java.util.Locale;

public interface PlatformUserNotificationService {

    PlatformUserNotificationResponse ingest(PlatformBellNotificationRequest request, Locale locale);

    PlatformUserNotificationResponse listInbox(Long userId, Locale locale, String username);

    PlatformUserNotificationResponse dismiss(Long userId, Long notificationId, Locale locale, String username);

    PlatformUserNotificationResponse dismissAll(Long userId, Locale locale, String username);
}
