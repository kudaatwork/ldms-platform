package projectlx.co.zw.notificationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.notificationsmanagementservice.business.validation.api.TemplateProcessorServiceValidator;
import projectlx.co.zw.notificationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
public class TemplateProcessorServiceValidatorImpl implements TemplateProcessorServiceValidator {

    private final MessageService messageService;
    private static final Logger logger = LoggerFactory.getLogger(TemplateProcessorServiceValidatorImpl.class);

    @Override
    public ValidatorDto isValidForProcessing(String templateContent, Map<String, Object> data, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (templateContent == null || templateContent.isBlank()) {
            logger.info("Validation failed: Template content is null or blank");
            errors.add(messageService.getMessage(I18Code.TEMPLATE_CONTENT_MISSING.getCode(), new String[]{}, locale));
        }

        if (data == null) {
            logger.info("Validation failed: Template data is null");
            errors.add(messageService.getMessage(I18Code.TEMPLATE_DATA_NULL.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }
}
