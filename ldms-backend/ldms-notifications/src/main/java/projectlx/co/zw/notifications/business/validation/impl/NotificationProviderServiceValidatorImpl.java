package projectlx.co.zw.notifications.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.notifications.business.validation.api.NotificationProviderServiceValidator;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.enums.I18Code;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class NotificationProviderServiceValidatorImpl implements NotificationProviderServiceValidator {

    private final MessageService messageService;
    private static final Logger logger = LoggerFactory.getLogger(NotificationProviderServiceValidatorImpl.class);
    private final Channel channel;

    @Override
    public ValidatorDto isValidForSending(NotificationRequest request, NotificationTemplate template, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: NotificationRequest is null");
            errors.add(messageService.getMessage(I18Code.NOTIFICATION_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (template == null) {
            logger.info("Validation failed: NotificationTemplate is null");
            errors.add(messageService.getMessage(I18Code.NOTIFICATION_TEMPLATE_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getRecipient() == null) {
            logger.info("Validation failed: Recipient is missing");
            errors.add(messageService.getMessage(I18Code.NOTIFICATION_RECIPIENT_MISSING.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        // Validate channel-specific recipient information
        switch (channel) {

            case EMAIL:
                if (request.getRecipient().getEmail() == null || request.getRecipient().getEmail().isEmpty()) {
                    logger.info("Validation failed: Email address is missing for EMAIL channel");
                    errors.add(messageService.getMessage(I18Code.EMAIL_CONTENT_IS_MISSING.getCode(), new String[]{}, locale));
                }
                break;

            case SMS:
                if (request.getRecipient().getPhoneNumber() == null || request.getRecipient().getPhoneNumber().isEmpty()) {
                    logger.info("Validation failed: Phone number is missing for SMS channel");
                    errors.add(messageService.getMessage(I18Code.WHATSAPP_TEMPLATE_NAME_IS_MISSING.getCode(), new String[]{}, locale));
                }
                break;

            case WHATSAPP:
                if (request.getRecipient().getPhoneNumber() == null || request.getRecipient().getPhoneNumber().isEmpty()) {
                    logger.info("Validation failed: Phone number is missing for WHATSAPP channel");
                    errors.add(messageService.getMessage(I18Code.WHATSAPP_TEMPLATE_NAME_IS_MISSING.getCode(), new String[]{}, locale));
                }
                break;

            case IN_APP:
                if (request.getRecipient().getUserId() == null || request.getRecipient().getUserId().isEmpty()) {
                    logger.info("Validation failed: User ID is missing for IN_APP channel");
                    errors.add(messageService.getMessage(I18Code.IN_APP_CONTENT_IS_MISSING.getCode(), new String[]{}, locale));
                }
                break;
            case SLACK:
                if ((request.getRecipient().getSlackWebhookUrl() == null || request.getRecipient().getSlackWebhookUrl().isEmpty()) &&
                        (request.getRecipient().getChannelWebhookUrls() == null ||
                                request.getRecipient().getChannelWebhookUrls().get("SLACK") == null ||
                                request.getRecipient().getChannelWebhookUrls().get("SLACK").isEmpty())) {
                    logger.info("Validation failed: Slack webhook URL is missing for SLACK channel");
                    errors.add(messageService.getMessage(I18Code.IN_APP_CONTENT_IS_MISSING.getCode(), new String[]{}, locale));
                }
                break;
            case TEAMS:
                if ((request.getRecipient().getTeamsWebhookUrl() == null || request.getRecipient().getTeamsWebhookUrl().isEmpty()) &&
                        (request.getRecipient().getChannelWebhookUrls() == null ||
                                request.getRecipient().getChannelWebhookUrls().get("TEAMS") == null ||
                                request.getRecipient().getChannelWebhookUrls().get("TEAMS").isEmpty())) {
                    logger.info("Validation failed: Teams webhook URL is missing for TEAMS channel");
                    errors.add(messageService.getMessage(I18Code.IN_APP_CONTENT_IS_MISSING.getCode(), new String[]{}, locale));
                }
                break;
            default:
                logger.warn("Unknown channel: {}", channel);
                errors.add(messageService.getMessage(I18Code.CHANNEL_LIST_IS_EMPTY_OR_NULL.getCode(), new String[]{channel.toString()}, locale));
        }

        if (request.getData() == null) {
            logger.info("Validation failed: Data is missing");
            errors.add(messageService.getMessage(I18Code.DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }
}
