package projectlx.co.zw.notifications.business.logic.impl;

import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogRecorder;
import projectlx.co.zw.notifications.business.logic.api.NotificationService;
import projectlx.co.zw.notifications.business.logic.api.NotificationProviderService;
import projectlx.co.zw.notifications.business.validation.api.NotificationServiceValidator;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.support.TemplateChannelDeliverySupport;
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
    private final NotificationLogRecorder notificationLogRecorder;

    public NotificationServiceImpl(
            NotificationTemplateRepository templateRepository,
            UserNotificationPreferenceRepository preferenceRepository,
            List<NotificationProviderService> providers,
            NotificationServiceValidator validator,
            NotificationLogRecorder notificationLogRecorder) {
        this.templateRepository = templateRepository;
        this.preferenceRepository = preferenceRepository;
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(NotificationProviderService::getChannel, Function.identity()));
        this.validator = validator;
        this.notificationLogRecorder = notificationLogRecorder;
        log.info("Initialized with {} notification providers.", providerMap.size());
    }

    @Override
    public void processNotification(NotificationRequest request) {

        log.info("[NOTIFICATION] Processing eventId={} templateKey={}", request.getEventId(), request.getTemplateKey());

        ValidatorDto validationResult = validator.isNotificationRequestValid(request, Locale.getDefault());

        if (!validationResult.getSuccess()) {
            log.error("[NOTIFICATION] Validation failed eventId={} templateKey={} errors={}",
                    request.getEventId(), request.getTemplateKey(), validationResult.getErrorMessages());
            return;
        }

        NotificationTemplate template = templateRepository.findByTemplateKeyAndEntityStatusNot(request.getTemplateKey(),
                        EntityStatus.DELETED)
                .orElseThrow(() -> new TemplateNotFoundException("Active template not found for key: " + request.getTemplateKey()));

        notificationLogRecorder.recordQueued(request, template);

        template.getChannels().forEach(channel -> {
            if (!TemplateChannelDeliverySupport.isChannelDeliveryEnabled(template, channel)) {
                log.info("[NOTIFICATION] Skipped (channel delivery disabled on template) templateKey={} channel={}",
                        request.getTemplateKey(), channel);
                notificationLogRecorder.markSkipped(request, channel, "channel delivery disabled on template");
                return;
            }

            boolean isEnabled = isNotificationEnabledForUser(request.getRecipient().getUserId(), request.getTemplateKey(),
                    channel);

            if (!isEnabled) {
                log.info("[NOTIFICATION] Skipped (disabled by user) userId={} templateKey={} channel={}",
                        request.getRecipient().getUserId(), request.getTemplateKey(), channel);
                notificationLogRecorder.markSkipped(request, channel, "disabled by user preference");
                return;
            }

            NotificationProviderService provider = providerMap.get(channel);

            if (provider != null) {
                try {
                    log.info("[NOTIFICATION] Dispatching channel={} eventId={} templateKey={}", channel, request.getEventId(),
                            request.getTemplateKey());
                    provider.send(request, template);
                } catch (Exception e) {
                    log.error("[NOTIFICATION] Failed dispatch channel={} eventId={} templateKey={}", channel,
                            request.getEventId(), request.getTemplateKey(), e);
                    notificationLogRecorder.markFailed(request, channel, e.getMessage());
                }
            } else {
                log.warn("[NOTIFICATION] No provider found channel={} templateKey={}. Skipping.", channel,
                        request.getTemplateKey());
                notificationLogRecorder.markSkipped(request, channel, "no provider registered");
            }
        });
    }

    private boolean isNotificationEnabledForUser(String userId, String templateKey, Channel channel) {
        return preferenceRepository.findByUserIdAndTemplateKeyAndChannelAndEntityStatusNot(userId, templateKey, channel,
                        EntityStatus.DELETED)
                .map(preference -> preference.isEnabled())
                .orElse(true);
    }
}
