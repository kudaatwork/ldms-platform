package projectlx.co.zw.notificationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.notificationsmanagementservice.business.validation.api.NotificationServiceValidator;
import projectlx.co.zw.notificationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.notificationsmanagementservice.utils.requests.NotificationRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class NotificationServiceValidatorImpl implements NotificationServiceValidator {

    private final MessageService messageService;
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceValidatorImpl.class);

    @Override
    public ValidatorDto isNotificationRequestValid(NotificationRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: NotificationRequest is null");
            errors.add(messageService.getMessage(I18Code.NOTIFICATION_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getTemplateKey() == null || request.getTemplateKey().isEmpty()) {
            logger.info("Validation failed: Template key is missing");
            errors.add(messageService.getMessage(I18Code.NOTIFICATION_TEMPLATE_KEY_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getRecipient() == null) {
            logger.info("Validation failed: Recipient is missing");
            errors.add(messageService.getMessage(I18Code.NOTIFICATION_RECIPIENT_MISSING.getCode(), new String[]{}, locale));
        } else {
            // Check if at least one contact method is provided
            boolean hasContactMethod = request.getRecipient().getEmail() != null && !request.getRecipient().getEmail().isEmpty()
                    || request.getRecipient().getPhoneNumber() != null && !request.getRecipient().getPhoneNumber().isEmpty()
                    || request.getRecipient().getFcmToken() != null && !request.getRecipient().getFcmToken().isEmpty();

            if (!hasContactMethod) {
                logger.info("Validation failed: No contact method provided for recipient");
                errors.add(messageService.getMessage(I18Code.NOTIFICATION_RECIPIENT_CONTACT_MISSING.getCode(), new String[]{}, locale));
            }
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
