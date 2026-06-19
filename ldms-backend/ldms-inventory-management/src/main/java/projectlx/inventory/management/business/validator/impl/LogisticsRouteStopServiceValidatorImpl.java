package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.inventory.management.business.validator.api.LogisticsRouteStopServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.ReplaceRouteStopsRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class LogisticsRouteStopServiceValidatorImpl implements LogisticsRouteStopServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(LogisticsRouteStopServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isReplaceRouteStopsRequestValid(ReplaceRouteStopsRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: ReplaceRouteStopsRequest is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ROUTE_STOP_INVALID_REQUEST.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getContextType() == null) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ROUTE_STOP_CONTEXT_TYPE_REQUIRED.getCode(), locale));
        }

        if (request.getContextId() == null || request.getContextId() <= 0) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ROUTE_STOP_CONTEXT_ID_REQUIRED.getCode(), locale));
        }

        if (request.getOrganizationId() == null || request.getOrganizationId() <= 0) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ROUTE_STOP_ORGANIZATION_ID_REQUIRED.getCode(), locale));
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
