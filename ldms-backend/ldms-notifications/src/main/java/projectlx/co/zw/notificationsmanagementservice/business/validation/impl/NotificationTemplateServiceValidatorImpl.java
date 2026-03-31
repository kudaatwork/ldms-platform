package projectlx.co.zw.notificationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.notificationsmanagementservice.business.validation.api.NotificationTemplateServiceValidator;
import projectlx.co.zw.notificationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.notificationsmanagementservice.utils.requests.CreateTemplateRequest;
import projectlx.co.zw.notificationsmanagementservice.utils.requests.TemplateMultipleFiltersRequest;
import projectlx.co.zw.notificationsmanagementservice.utils.requests.UpdateTemplateRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrLessThanOne;

@RequiredArgsConstructor
public class NotificationTemplateServiceValidatorImpl implements NotificationTemplateServiceValidator {

    private final MessageService messageService;
    private static final Logger logger = LoggerFactory.getLogger(NotificationTemplateServiceValidatorImpl.class);

    @Override
    public ValidatorDto isCreateTemplateRequestValid(CreateTemplateRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateTemplateRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_NOTIFICATION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getTemplateKey() == null || request.getTemplateKey().isEmpty()) {
            logger.info("Validation failed: Template key is missing");
            errors.add(messageService.getMessage(I18Code.NOTIFICATION_TEMPLATE_KEY_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getDescription() == null || request.getDescription().isEmpty()) {
            logger.info("Validation failed: Description is missing");
            errors.add(messageService.getMessage(I18Code.DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            logger.info("Validation failed: Channels list is empty or null");
            errors.add(messageService.getMessage(I18Code.CHANNEL_LIST_IS_EMPTY_OR_NULL.getCode(), new String[]{"null"}, locale));
        }

        // Validate that appropriate content is provided for each channel
        if (request.getChannels() != null) {
            for (var channel : request.getChannels()) {
                switch (channel) {
                    case EMAIL:
                        if (request.getEmailSubject() == null || request.getEmailSubject().isEmpty() ||
                                request.getEmailBodyHtml() == null || request.getEmailBodyHtml().isEmpty()) {
                            logger.info("Validation failed: Email content is missing");
                            errors.add(messageService.getMessage(I18Code.EMAIL_CONTENT_IS_MISSING.getCode(), new String[]{}, locale));
                        }
                        break;
                    case SMS:
                        if (request.getSmsBody() == null || request.getSmsBody().isEmpty()) {
                            logger.info("Validation failed: SMS content is missing");
                            errors.add(messageService.getMessage(I18Code.SMS_CONTENT_IS_MISSING.getCode(), new String[]{}, locale));
                        }
                        break;
                    case IN_APP:
                        if (request.getInAppTitle() == null || request.getInAppTitle().isEmpty() ||
                                request.getInAppBody() == null || request.getInAppBody().isEmpty()) {
                            logger.info("Validation failed: In-app content is missing");
                            errors.add(messageService.getMessage(I18Code.IN_APP_CONTENT_IS_MISSING.getCode(), new String[]{}, locale));
                        }
                        break;
                    case WHATSAPP:
                        if (request.getWhatsappTemplateName() == null || request.getWhatsappTemplateName().isEmpty()) {
                            logger.info("Validation failed: WhatsApp template name is missing");
                            errors.add(messageService.getMessage(I18Code.WHATSAPP_TEMPLATE_NAME_IS_MISSING.getCode(), new String[]{}, locale));
                        }
                        break;
                    case SLACK:
                    case TEAMS:
                        if ((request.getInAppBody() == null || request.getInAppBody().isEmpty()) &&
                                (request.getSmsBody() == null || request.getSmsBody().isEmpty()) &&
                                (request.getDescription() == null || request.getDescription().isEmpty())) {
                            logger.info("Validation failed: Webhook channel content is missing");
                            errors.add(messageService.getMessage(I18Code.IN_APP_CONTENT_IS_MISSING.getCode(), new String[]{}, locale));
                        }
                        break;
                }
            }
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (isNullOrLessThanOne(id)) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isUpdateTemplateRequestValid(UpdateTemplateRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: UpdateTemplateRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_NOTIFICATION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrLessThanOne(request.getId())) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isTemplateMultipleFiltersRequestValid(TemplateMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: TemplateMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_NOTIFICATION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }
}