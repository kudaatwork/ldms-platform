package projectlx.co.zw.notifications.business.validation.api;

import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

/**
 * Validator interface for NotificationProviderService
 */
public interface NotificationProviderServiceValidator {
    /**
     * Validates if the notification request is valid for sending via a specific channel
     * @param request The notification request to validate
     * @param template The template to use for the notification
     * @param locale The locale for internationalization
     * @return ValidatorDto containing validation result and any error messages
     */
    ValidatorDto isValidForSending(NotificationRequest request, NotificationTemplate template, Locale locale);
}