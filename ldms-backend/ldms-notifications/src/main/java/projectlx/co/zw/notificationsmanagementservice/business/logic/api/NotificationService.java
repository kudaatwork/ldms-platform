package projectlx.co.zw.notificationsmanagementservice.business.logic.api;

import projectlx.co.zw.notificationsmanagementservice.utils.requests.NotificationRequest;

public interface NotificationService {
    /**
     * Processes a notification request received from the message queue.
     * This is the main entry point for the business logic.
     * @param request The deserialized request from RabbitMQ.
     */
    void processNotification(NotificationRequest request);
}
