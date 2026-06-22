package projectlx.fleet.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.fleet.management.business.validator.api.FleetDriverServiceValidator;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.requests.CreateFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetDriverRequest;
import projectlx.fleet.management.utils.requests.ProvisionFleetDriverPlatformAccessRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

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
        validateIdentityAndAddress(request.getFirstName(), request.getLastName(), request.getLicenseNumber(),
                request.getNationalIdNumber(), request.getPassportNumber(),
                request.getAddressLine1(), request.getAddressCity(), errors, locale);
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
        validateIdentityAndAddress(request.getFirstName(), request.getLastName(), request.getLicenseNumber(),
                request.getNationalIdNumber(), request.getPassportNumber(),
                request.getAddressLine1(), request.getAddressCity(), errors, locale);
        validateDocuments(request.getNationalIdUploadId(), request.getPassportUploadId(),
                request.getLicenseUploadId(), errors, locale);
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isProvisionPlatformAccessRequestValid(
            ProvisionFleetDriverPlatformAccessRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: provision platform access request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (!StringUtils.hasText(request.getEmail()) || !Validators.isValidEmail(request.getEmail().trim())) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_PROVISION_EMAIL_REQUIRED.getCode(), new String[]{}, locale));
        }
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    // ============================================================
    // Private helpers
    // ============================================================

    /**
     * Validates identity fields and address on both create and update:
     * <ul>
     *   <li>firstName, lastName, licenseNumber — required</li>
     *   <li>nationalIdNumber OR passportNumber — at least one required</li>
     *   <li>addressLine1, addressCity — required</li>
     * </ul>
     */
    private void validateIdentityAndAddress(String firstName, String lastName, String licenseNumber,
                                            String nationalIdNumber, String passportNumber,
                                            String addressLine1, String addressCity,
                                            List<String> errors, Locale locale) {
        boolean identityFieldsMissing = false;
        if (firstName == null || firstName.isBlank()) {
            identityFieldsMissing = true;
        }
        if (lastName == null || lastName.isBlank()) {
            identityFieldsMissing = true;
        }
        if (licenseNumber == null || licenseNumber.isBlank()) {
            identityFieldsMissing = true;
        }
        if (identityFieldsMissing) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_DRIVER_IDENTITY_REQUIRED.getCode(), new String[]{}, locale));
        }

        boolean hasIdentityDocument = (nationalIdNumber != null && !nationalIdNumber.isBlank())
                || (passportNumber != null && !passportNumber.isBlank());
        if (!hasIdentityDocument) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_DRIVER_IDENTITY_DOCUMENT_REQUIRED.getCode(), new String[]{}, locale));
        }

        boolean addressMissing = (addressLine1 == null || addressLine1.isBlank())
                || (addressCity == null || addressCity.isBlank());
        if (addressMissing) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_DRIVER_ADDRESS_REQUIRED.getCode(), new String[]{}, locale));
        }
    }

    /**
     * Validates upload id presence on both create and update:
     * <ul>
     *   <li>nationalIdUploadId OR passportUploadId — at least one required</li>
     *   <li>licenseUploadId — required</li>
     * </ul>
     */
    private void validateDocuments(Long nationalIdUploadId, Long passportUploadId,
                                   Long licenseUploadId, List<String> errors, Locale locale) {
        boolean hasIdentityUpload = (nationalIdUploadId != null && nationalIdUploadId > 0)
                || (passportUploadId != null && passportUploadId > 0);
        if (!hasIdentityUpload) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_DRIVER_IDENTITY_DOCUMENT_REQUIRED.getCode(), new String[]{}, locale));
        }

        if (licenseUploadId == null || licenseUploadId < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_DRIVER_LICENSE_DOCUMENT_REQUIRED.getCode(), new String[]{}, locale));
        }
    }
}
