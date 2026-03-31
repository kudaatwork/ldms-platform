package projectlx.co.zw.notifications.business.validation.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;
import java.util.Map;

/**
 * Validator interface for TemplateProcessorService
 */
public interface TemplateProcessorServiceValidator {
    /**
     * Validates if the template content and data are valid for processing
     * @param templateContent The template content to validate
     * @param data The data to validate
     * @param locale The locale for internationalization
     * @return ValidatorDto containing validation result and any error messages
     */
    ValidatorDto isValidForProcessing(String templateContent, Map<String, Object> data, Locale locale);
}