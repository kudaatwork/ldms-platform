package projectlx.co.zw.notifications.business.logic.impl;

import org.springframework.util.StringUtils;
import projectlx.co.zw.notifications.business.auditable.api.NotificationLogServiceAuditable;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogRecorder;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.repository.NotificationLogRepository;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class NotificationLogRecorderImpl implements NotificationLogRecorder {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationLogServiceAuditable notificationLogServiceAuditable;

    public NotificationLogRecorderImpl(
            NotificationLogRepository notificationLogRepository,
            NotificationLogServiceAuditable notificationLogServiceAuditable) {
        this.notificationLogRepository = notificationLogRepository;
        this.notificationLogServiceAuditable = notificationLogServiceAuditable;
    }

    @Override
    public void recordQueued(NotificationRequest request, NotificationTemplate template) {
        if (request == null || template == null || template.getChannels() == null) {
            return;
        }
        for (Channel channel : template.getChannels()) {
            upsert(request, channel, "QUEUED", null, null, null, null);
        }
    }

    @Override
    public void markSkipped(NotificationRequest request, Channel channel, String reason) {
        upsert(request, channel, "SKIPPED", reason, null, null, null);
    }

    @Override
    public void markFailed(NotificationRequest request, Channel channel, String errorMessage) {
        upsert(request, channel, "FAILED", errorMessage, null, null, null);
    }

    @Override
    public NotificationLog beginDispatch(NotificationRequest request, Channel channel) {
        return upsert(request, channel, "PENDING", null, null, null, null);
    }

    @Override
    public void markSent(NotificationLog logEntry, String provider, String providerMessageId, String renderedContent) {
        if (logEntry == null) {
            return;
        }
        logEntry.setStatus("SENT");
        logEntry.setProvider(provider);
        logEntry.setProviderMessageId(providerMessageId);
        if (StringUtils.hasText(renderedContent)) {
            logEntry.setRenderedContent(renderedContent);
        }
        notificationLogServiceAuditable.update(logEntry);
    }

    @Override
    public void markFailed(NotificationLog logEntry, String errorMessage) {
        if (logEntry == null) {
            return;
        }
        logEntry.setStatus("FAILED");
        logEntry.setErrorMessage(errorMessage);
        notificationLogServiceAuditable.update(logEntry);
    }

    @Override
    public void markSkipped(NotificationLog logEntry, String reason) {
        if (logEntry == null) {
            return;
        }
        logEntry.setStatus("SKIPPED");
        logEntry.setErrorMessage(reason);
        notificationLogServiceAuditable.update(logEntry);
    }

    private NotificationLog upsert(
            NotificationRequest request,
            Channel channel,
            String status,
            String errorMessage,
            String provider,
            String providerMessageId,
            String renderedContent) {

        NotificationLog logEntry = resolveExisting(request, channel).orElseGet(NotificationLog::new);
        populateFromRequest(logEntry, request, channel);
        logEntry.setStatus(status);
        logEntry.setErrorMessage(errorMessage);
        if (provider != null) {
            logEntry.setProvider(provider);
        }
        if (providerMessageId != null) {
            logEntry.setProviderMessageId(providerMessageId);
        }
        if (renderedContent != null) {
            logEntry.setRenderedContent(renderedContent);
        }
        if (logEntry.getId() == null) {
            return notificationLogServiceAuditable.create(logEntry);
        }
        return notificationLogServiceAuditable.update(logEntry);
    }

    private java.util.Optional<NotificationLog> resolveExisting(NotificationRequest request, Channel channel) {
        if (request == null || !StringUtils.hasText(request.getEventId())) {
            return java.util.Optional.empty();
        }
        return notificationLogRepository.findFirstByEventIdAndChannelAndEntityStatusNot(
                request.getEventId(), channel, EntityStatus.DELETED);
    }

    private static void populateFromRequest(NotificationLog logEntry, NotificationRequest request, Channel channel) {
        logEntry.setEventId(request.getEventId());
        logEntry.setTemplateKey(request.getTemplateKey());
        logEntry.setChannel(channel);
        logEntry.setPayload(request.getData());
        if (request.getRecipient() != null) {
            logEntry.setRecipientId(request.getRecipient().getUserId());
            logEntry.setRecipientEmail(request.getRecipient().getEmail());
            logEntry.setRecipientPhone(request.getRecipient().getPhoneNumber());
        }
    }
}
