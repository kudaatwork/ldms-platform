package projectlx.co.zw.notifications.business.validation.api;

import projectlx.co.zw.notifications.utils.requests.CreateTemplateRequest;
import projectlx.co.zw.notifications.utils.requests.TemplateMultipleFiltersRequest;
import projectlx.co.zw.notifications.utils.requests.UpdateTemplateRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

/**
 * Validator interface for NotificationTemplateService
 */
public interface NotificationTemplateServiceValidator {
    /**
     * Validates if the create template request is valid
     * @param request The create template request to validate
     * @param locale The locale for internationalization
     * @return ValidatorDto containing validation result and any error messages
     */
    ValidatorDto isCreateTemplateRequestValid(CreateTemplateRequest request, Locale locale);

    /**
     * Validates if the ID is valid
     * @param id The ID to validate
     * @param locale The locale for internationalization
     * @return ValidatorDto containing validation result and any error messages
     */
    ValidatorDto isIdValid(Long id, Locale locale);

    /**
     * Validates if the update template request is valid
     * @param request The update template request to validate
     * @param locale The locale for internationalization
     * @return ValidatorDto containing validation result and any error messages
     */
    ValidatorDto isUpdateTemplateRequestValid(UpdateTemplateRequest request, Locale locale);

    /**
     * Validates if the template multiple filters request is valid
     * @param request The template multiple filters request to validate
     * @param locale The locale for internationalization
     * @return ValidatorDto containing validation result and any error messages
     */
    ValidatorDto isTemplateMultipleFiltersRequestValid(TemplateMultipleFiltersRequest request, Locale locale);
}