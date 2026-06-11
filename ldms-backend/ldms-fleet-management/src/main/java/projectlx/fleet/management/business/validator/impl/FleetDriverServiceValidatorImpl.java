package projectlx.fleet.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.fleet.management.business.validator.api.FleetDriverServiceValidator;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.requests.CreateFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetDriverRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class FleetDriverServiceValidatorImpl implements FleetDriverServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(FleetDriverServiceValidatorImpl.class);
    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateFleetDriverRequestValid(CreateFleetDriverRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: create fleet driver request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        validateCommonFields(request.getFirstName(), request.getLastName(), request.getLicenseNumber(), errors, locale);
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isEditFleetDriverRequestValid(EditFleetDriverRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: edit fleet driver request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getId() == null || request.getId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"id"}, locale));
        }
        validateCommonFields(request.getFirstName(), request.getLastName(), request.getLicenseNumber(), errors, locale);
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    private void validateCommonFields(String firstName, String lastName, String licenseNumber,
                                      List<String> errors, Locale locale) {
        if (firstName == null || firstName.isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"firstName"}, locale));
        }
        if (lastName == null || lastName.isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"lastName"}, locale));
        }
        if (licenseNumber == null || licenseNumber.isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"licenseNumber"}, locale));
        }
    }
}
