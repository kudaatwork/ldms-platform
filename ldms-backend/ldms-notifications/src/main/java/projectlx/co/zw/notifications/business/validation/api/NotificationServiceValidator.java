package projectlx.co.zw.notifications.business.validation.api;

import projectlx.co.zw.notifications.utils.requests.NotificationRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

/**
 * Validator interface for NotificationService
 */
public interface NotificationServiceValidator {
    /**
     * Validates if the notification request is valid for processing
     * @param request The notification request to validate
     * @param locale The locale for internationalization
     * @return ValidatorDto containing validation result and any error messages
     */
    ValidatorDto isNotificationRequestValid(NotificationRequest request, Locale locale);
}