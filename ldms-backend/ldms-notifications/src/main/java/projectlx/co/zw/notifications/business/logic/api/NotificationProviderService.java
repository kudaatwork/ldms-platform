package projectlx.co.zw.notifications.business.logic.api;

import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;

public interface NotificationProviderService {
    /**
     * Sends a notification using a specific provider implementation.
     * @param request The original notification request containing recipient info and data.
     * @param template The template to use for the notification.
     */
    void send(NotificationRequest request, NotificationTemplate template);

    /**
     * Returns the channel this provider is responsible for. Used by the Strategy pattern.
     * @return The Channel enum.
     */
    Channel getChannel();
}
