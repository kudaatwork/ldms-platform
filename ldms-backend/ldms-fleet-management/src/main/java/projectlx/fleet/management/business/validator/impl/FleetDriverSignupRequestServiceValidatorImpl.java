package projectlx.fleet.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.fleet.management.business.validator.api.FleetDriverSignupRequestServiceValidator;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.requests.CreateFleetDriverSignupRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class FleetDriverSignupRequestServiceValidatorImpl implements FleetDriverSignupRequestServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(FleetDriverSignupRequestServiceValidatorImpl.class);
    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateSignupRequestValid(CreateFleetDriverSignupRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: driver signup request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isBlank(request.getFirstName())) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"firstName"}, locale));
        }
        if (isBlank(request.getLastName())) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"lastName"}, locale));
        }
        if (isBlank(request.getEmail())) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"email"}, locale));
        }
        if (isBlank(request.getPhoneNumber())) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"phoneNumber"}, locale));
        }
        if (isBlank(request.getLicenseNumber())) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"licenseNumber"}, locale));
        }
        if (isBlank(request.getLicenseClass())) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"licenseClass"}, locale));
        }
        if (isBlank(request.getNationalIdNumber())) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"nationalIdNumber"}, locale));
        }
        if (request.getStagingSessionId() == null || request.getStagingSessionId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"stagingSessionId"}, locale));
        }
        if (!hasUploadId(request.getNationalIdFrontUploadId())
                || !hasUploadId(request.getNationalIdBackUploadId())
                || !hasUploadId(request.getLicenseFrontUploadId())
                || !hasUploadId(request.getLicenseBackUploadId())) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_SIGNUP_DOCUMENTS_REQUIRED.getCode(), new String[]{}, locale));
        }
        // Company code required only when joining a specific transport company (not freelance).
        if (isBlank(request.getCompanyCode())) {
            // Freelance signup — company code intentionally omitted.
        } else if (request.getCompanyCode().trim().length() < 4) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                    new String[]{"companyCode (min 4 characters)"}, locale));
        }

        return errors.isEmpty()
                ? new ValidatorDto(true, null, new ArrayList<>())
                : new ValidatorDto(false, null, errors);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean hasUploadId(Long uploadId) {
        return uploadId != null && uploadId > 0;
    }
}
