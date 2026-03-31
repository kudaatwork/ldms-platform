package projectlx.co.zw.notifications.business.logic.impl;

import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.notifications.business.auditable.api.NotificationLogServiceAuditable;
import projectlx.co.zw.notifications.business.logic.api.NotificationService;
import projectlx.co.zw.notifications.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notifications.business.validation.api.NotificationServiceValidator;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.repository.NotificationTemplateRepository;
import projectlx.co.zw.notifications.repository.UserNotificationPreferenceRepository;
import projectlx.co.zw.notifications.utils.exception.TemplateNotFoundException;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final Map<Channel, NotificationProviderService> providerMap;
    private final NotificationServiceValidator validator;
    private final NotificationLogServiceAuditable notificationLogServiceAuditable;

    // Spring automatically injects all beans of type NotificationProvider into a list.
    // We then convert this list into a map for easy lookup (Strategy Pattern).
    public NotificationServiceImpl(
            NotificationTemplateRepository templateRepository,
            UserNotificationPreferenceRepository preferenceRepository,
            List<NotificationProviderService> providers,
            NotificationServiceValidator validator,
            NotificationLogServiceAuditable notificationLogServiceAuditable
    ) {
        this.templateRepository = templateRepository;
        this.preferenceRepository = preferenceRepository;
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(NotificationProviderService::getChannel, Function.identity()));
        this.validator = validator;
        this.notificationLogServiceAuditable = notificationLogServiceAuditable;
        log.info("Initialized with {} notification providers.", providerMap.size());
    }

    @Override
    public void processNotification(NotificationRequest request) {

        log.info("[NOTIFICATION] Processing eventId={} templateKey={}", request.getEventId(), request.getTemplateKey());

        // Validate the notification request
        ValidatorDto validationResult = validator.isNotificationRequestValid(request, Locale.getDefault());

        if (!validationResult.getSuccess()) {
            log.error("[NOTIFICATION] Validation failed eventId={} templateKey={} errors={}", request.getEventId(), request.getTemplateKey(), validationResult.getErrorMessages());
            return;
        }

        // 1. Fetch the template
        NotificationTemplate template = templateRepository.findByTemplateKeyAndEntityStatusNot(request.getTemplateKey(),
                        EntityStatus.DELETED)
                .orElseThrow(() -> new TemplateNotFoundException("Active template not found for key: " + request.getTemplateKey()));

        // 2. Loop through the channels defined in the template
        template.getChannels().forEach(channel -> {
            // 3. Check if user has disabled this notification
            boolean isEnabled = isNotificationEnabledForUser(request.getRecipient().getUserId(), request.getTemplateKey(),
                    channel);

            if (!isEnabled) {

                log.info("[NOTIFICATION] Skipped (disabled by user) userId={} templateKey={} channel={}",
                        request.getRecipient().getUserId(), request.getTemplateKey(), channel);
                return; // Skip to the next channel
            }

            // 4. Find the correct provider for the channel
            NotificationProviderService provider = providerMap.get(channel);

            if (provider != null) {

                try {
                    log.info("[NOTIFICATION] Dispatching channel={} eventId={} templateKey={}", channel, request.getEventId(), request.getTemplateKey());
                    provider.send(request, template);
                } catch (Exception e) {
                    log.error("[NOTIFICATION] Failed dispatch channel={} eventId={} templateKey={}", channel, request.getEventId(), request.getTemplateKey(), e);
                    // Log the failure to the NotificationLog table
                    NotificationLog logEntry = createLogEntry(request, channel, "FAILED", e.getMessage());
                    NotificationLog savedLogEntry = notificationLogServiceAuditable.create(logEntry);
                }

            } else {
                log.warn("[NOTIFICATION] No provider found channel={} templateKey={}. Skipping.", channel, request.getTemplateKey());
            }
        });
    }

    private boolean isNotificationEnabledForUser(String userId, String templateKey, Channel channel) {
        // Find a specific preference setting. If it exists, respect it.
        // If it doesn't exist, we default to 'true' (the user hasn't opted out).
        return preferenceRepository.findByUserIdAndTemplateKeyAndChannelAndEntityStatusNot(userId, templateKey, channel,
                        EntityStatus.DELETED)
                .map(preference -> preference.isEnabled())
                .orElse(true);
    }

    private NotificationLog createLogEntry(NotificationRequest request, Channel channel, String status, String errorMessage) {

        NotificationLog logEntry = new NotificationLog();
        logEntry.setRecipientId(request.getRecipient().getUserId());
        logEntry.setTemplateKey(request.getTemplateKey());
        logEntry.setChannel(channel);
        logEntry.setStatus(status);
        logEntry.setPayload(request.getData());
        logEntry.setErrorMessage(errorMessage);
        return logEntry;
    }
}
