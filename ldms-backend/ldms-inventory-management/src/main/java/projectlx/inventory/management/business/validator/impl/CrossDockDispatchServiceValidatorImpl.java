package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.inventory.management.business.validator.api.CrossDockDispatchServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.DispatchIngestRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class CrossDockDispatchServiceValidatorImpl implements CrossDockDispatchServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(CrossDockDispatchServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isDispatchIngestRequestValid(DispatchIngestRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: DispatchIngestRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_DISPATCH_INGEST_INVALID_REQUEST.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getApiKey() == null || request.getApiKey().isBlank()) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_DISPATCH_INGEST_API_KEY_REQUIRED.getCode(), locale));
        }

        if (request.getExternalDispatchId() == null || request.getExternalDispatchId().isBlank()) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_DISPATCH_INGEST_EXTERNAL_ID_REQUIRED.getCode(), locale));
        }

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_QUANTITY_MUST_BE_POSITIVE.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (id == null || id <= 0) {
            logger.info("Validation failed: ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }
}
