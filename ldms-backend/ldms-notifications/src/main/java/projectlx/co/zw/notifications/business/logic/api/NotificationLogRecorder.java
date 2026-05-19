package projectlx.co.zw.notifications.business.logic.api;

import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;

/**
 * Persists delivery lifecycle rows in {@code notification_log} (QUEUED → PENDING → SENT / FAILED / SKIPPED).
 */
public interface NotificationLogRecorder {

    /** One QUEUED row per template channel when a Rabbit message is accepted for processing. */
    void recordQueued(NotificationRequest request, NotificationTemplate template);

    void markSkipped(NotificationRequest request, Channel channel, String reason);

    void markFailed(NotificationRequest request, Channel channel, String errorMessage);

    /** Returns the log row to update through send (creates PENDING if missing). */
    NotificationLog beginDispatch(NotificationRequest request, Channel channel);

    void markSent(NotificationLog logEntry, String provider, String providerMessageId, String renderedContent);

    void markFailed(NotificationLog logEntry, String errorMessage);

    void markSkipped(NotificationLog logEntry, String reason);
}
