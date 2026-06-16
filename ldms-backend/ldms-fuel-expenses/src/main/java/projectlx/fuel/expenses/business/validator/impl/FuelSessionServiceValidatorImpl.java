package projectlx.fuel.expenses.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fuel.expenses.business.validator.api.FuelSessionServiceValidator;
import projectlx.fuel.expenses.utils.enums.I18Code;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class FuelSessionServiceValidatorImpl implements FuelSessionServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(FuelSessionServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isGetLiveByTripIdRequestValid(Long tripId, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (tripId == null) {
            logger.info("Validation failed: tripId is null");
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_TRIP_ID_REQUIRED.getCode(),
                    new String[]{},
                    locale));
            return new ValidatorDto(false, null, errors);
        }

        if (tripId <= 0) {
            logger.info("Validation failed: tripId {} is not positive", tripId);
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_TRIP_ID_INVALID.getCode(),
                    new String[]{},
                    locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, new ArrayList<>());
        }

        return new ValidatorDto(false, null, errors);
    }
}
