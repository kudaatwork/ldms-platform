package projectlx.fleet.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.fleet.management.business.auditable.api.FleetDriverServiceAuditable;
import projectlx.fleet.management.business.logic.api.FleetDriverService;
import projectlx.fleet.management.business.logic.support.CallerOrganizationResolver;
import projectlx.fleet.management.business.logic.support.FleetFileUploadHelper;
import projectlx.fleet.management.business.logic.support.FleetMapper;
import projectlx.fleet.management.business.logic.support.FleetOwnershipValidationSupport;
import projectlx.fleet.management.business.validator.api.FleetDriverServiceValidator;
import projectlx.fleet.management.model.FleetDriver;
import projectlx.fleet.management.repository.FleetDriverRepository;
import projectlx.fleet.management.utils.enums.DriverEmploymentType;
import projectlx.fleet.management.utils.enums.FleetOwnershipType;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.requests.CreateFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetDriverRequest;
import projectlx.fleet.management.utils.responses.FleetDriverResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class FleetDriverServiceImpl implements FleetDriverService {

    private final FleetDriverServiceValidator fleetDriverServiceValidator;
    private final FleetDriverRepository fleetDriverRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final MessageService messageService;
    private final FleetDriverServiceAuditable fleetDriverServiceAuditable;
    private final FleetFileUploadHelper fleetFileUploadHelper;
    private final FleetOwnershipValidationSupport fleetOwnershipValidationSupport;

    @Override
    @Transactional(readOnly = true)
    public FleetDriverResponse list(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }
        List<FleetDriver> drivers = fleetDriverRepository.findByOrganizationIdAndEntityStatusNotOrderByIdDesc(
                organizationId, EntityStatus.DELETED);
        FleetDriverResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDtoList(drivers.stream().map(FleetMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FleetDriverResponse listForTransporterPartner(Long transporterOrganizationId, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }
        if (transporterOrganizationId == null || transporterOrganizationId < 1) {
            return errorResponse(400, "Transport partner organisation id is required.");
        }
        String validationError = fleetOwnershipValidationSupport.validateOwnership(
                organizationId, FleetOwnershipType.CONTRACTED.name(), transporterOrganizationId);
        if (validationError != null) {
            return errorResponse(400, validationError);
        }
        List<FleetDriver> drivers = fleetDriverRepository.findByOrganizationIdAndEntityStatusNotOrderByIdDesc(
                transporterOrganizationId, EntityStatus.DELETED);
        String partnerLabel = "Transport partner #" + transporterOrganizationId;
        FleetDriverResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDtoList(drivers.stream()
                .map(driver -> FleetMapper.toPartnerRosterDto(driver, partnerLabel))
                .toList());
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetDriverResponse create(CreateFleetDriverRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request fields
        // ============================================================
        ValidatorDto validation = fleetDriverServiceValidator.isCreateFleetDriverRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_CREATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Persist driver entity
        // ============================================================
        FleetDriver driver = new FleetDriver();
        driver.setOrganizationId(organizationId);
        mapRequestToEntity(request, driver);
        driver.setEntityStatus(EntityStatus.ACTIVE);
        driver.setCreatedAt(LocalDateTime.now());
        driver.setCreatedBy(username);

        FleetDriver saved = fleetDriverServiceAuditable.create(driver, locale, username);

        FleetDriverResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDto(FleetMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetDriverResponse update(Long id, EditFleetDriverRequest request, Locale locale, String username) {
        if (request != null) {
            request.setId(id);
        }

        // ============================================================
        // STEP 1: Validate request fields
        // ============================================================
        ValidatorDto validation = fleetDriverServiceValidator.isEditFleetDriverRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_UPDATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Load existing driver
        // ============================================================
        FleetDriver driver = fleetDriverRepository.findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (driver == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_DRIVER_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Validate upload document references
        // ============================================================
        List<String> uploadErrors = validateUploadReferences(
                request.getNationalIdUploadId(),
                request.getPassportUploadId(),
                request.getLicenseUploadId(),
                driver.getId(),
                request.getUserId() != null ? request.getUserId() : driver.getUserId(),
                locale);
        if (!uploadErrors.isEmpty()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_UPDATE_INVALID.getCode(), new String[]{}, locale),
                    uploadErrors);
        }

        // ============================================================
        // STEP 4: Apply changes and persist
        // ============================================================
        applyEditRequestToEntity(request, driver);
        driver.setModifiedAt(LocalDateTime.now());
        driver.setModifiedBy(username);

        FleetDriver saved = fleetDriverServiceAuditable.update(driver, locale, username);
        FleetDriverResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_UPDATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDto(FleetMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetDriverResponse delete(Long id, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetDriver driver = fleetDriverRepository.findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (driver == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_DRIVER_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        driver.setEntityStatus(EntityStatus.DELETED);
        driver.setModifiedAt(LocalDateTime.now());
        driver.setModifiedBy(username);
        fleetDriverServiceAuditable.delete(driver, locale);

        return successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_DELETE_SUCCESS.getCode(), new String[]{}, locale));
    }

    @Override
    @Transactional(readOnly = true)
    public FleetDriverResponse findByIdForSystem(Long id, Locale locale) {
        if (id == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }
        FleetDriver driver = fleetDriverRepository.findByIdForReadOnly(id, EntityStatus.DELETED).orElse(null);
        if (driver == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        FleetDriverResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_DRIVER_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDto(FleetMapper.toDto(driver));
        return response;
    }

    // ============================================================
    // Mapping helpers
    // ============================================================

    private void mapRequestToEntity(CreateFleetDriverRequest request, FleetDriver driver) {
        driver.setUserId(request.getUserId());
        driver.setEmploymentType(resolveEmploymentType(request.getEmploymentType(), request.getUserId()));
        driver.setFirstName(request.getFirstName().trim());
        driver.setLastName(request.getLastName().trim());
        driver.setPhoneNumber(request.getPhoneNumber());
        driver.setLicenseNumber(request.getLicenseNumber().trim());
        driver.setLicenseClass(request.getLicenseClass());
        driver.setLicenseUploadId(request.getLicenseUploadId());
        applyIdentityFields(request.getNationalIdNumber(), request.getNationalIdExpiryDate(),
                request.getNationalIdUploadId(), request.getPassportNumber(),
                request.getPassportExpiryDate(), request.getPassportUploadId(), driver);
        applyAddressFields(request.getAddressLine1(), request.getAddressLine2(), request.getAddressCity(),
                request.getAddressProvince(), request.getAddressPostalCode(), request.getAddressCountry(), driver);
    }

    private void applyEditRequestToEntity(EditFleetDriverRequest request, FleetDriver driver) {
        driver.setUserId(request.getUserId());
        if (request.getEmploymentType() != null && !request.getEmploymentType().isBlank()) {
            driver.setEmploymentType(resolveEmploymentType(request.getEmploymentType(), request.getUserId()));
        }
        driver.setFirstName(request.getFirstName().trim());
        driver.setLastName(request.getLastName().trim());
        driver.setPhoneNumber(request.getPhoneNumber());
        driver.setLicenseNumber(request.getLicenseNumber().trim());
        driver.setLicenseClass(request.getLicenseClass());
        driver.setLicenseUploadId(request.getLicenseUploadId());
        applyIdentityFields(request.getNationalIdNumber(), request.getNationalIdExpiryDate(),
                request.getNationalIdUploadId(), request.getPassportNumber(),
                request.getPassportExpiryDate(), request.getPassportUploadId(), driver);
        applyAddressFields(request.getAddressLine1(), request.getAddressLine2(), request.getAddressCity(),
                request.getAddressProvince(), request.getAddressPostalCode(), request.getAddressCountry(), driver);
    }

    private void applyIdentityFields(String nationalIdNumber, java.time.LocalDate nationalIdExpiryDate,
                                     Long nationalIdUploadId, String passportNumber,
                                     java.time.LocalDate passportExpiryDate, Long passportUploadId,
                                     FleetDriver driver) {
        driver.setNationalIdNumber(blankToNull(nationalIdNumber));
        driver.setNationalIdExpiryDate(nationalIdExpiryDate);
        driver.setNationalIdUploadId(nationalIdUploadId);
        driver.setPassportNumber(blankToNull(passportNumber));
        driver.setPassportExpiryDate(passportExpiryDate);
        driver.setPassportUploadId(passportUploadId);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void applyAddressFields(String addressLine1, String addressLine2, String addressCity,
                                    String addressProvince, String addressPostalCode, String addressCountry,
                                    FleetDriver driver) {
        driver.setAddressLine1(addressLine1);
        driver.setAddressLine2(addressLine2);
        driver.setAddressCity(addressCity);
        driver.setAddressProvince(addressProvince);
        driver.setAddressPostalCode(addressPostalCode);
        driver.setAddressCountry(addressCountry);
    }

    // ============================================================
    // Employment type
    // ============================================================

    private DriverEmploymentType resolveEmploymentType(String raw, Long userId) {
        if (raw != null && !raw.isBlank()) {
            try {
                return DriverEmploymentType.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return userId != null && userId > 0 ? DriverEmploymentType.EMPLOYED : DriverEmploymentType.POOL;
    }

    // ============================================================
    // Upload reference validation
    // ============================================================

    /**
     * Validates all three driver document upload references.
     * At least one of nationalId/passport upload must be valid, and licence upload must be valid.
     *
     * @param nationalIdUploadId  national ID upload id (may be null)
     * @param passportUploadId    passport upload id (may be null)
     * @param licenseUploadId     driver licence upload id
     * @param fleetDriverId       persisted driver id; null on create before first save
     * @param linkedUserId        linked platform user id; null when no user linked
     * @param locale              request locale
     * @return list of error messages; empty when all valid
     */
    private List<String> validateUploadReferences(Long nationalIdUploadId, Long passportUploadId,
                                                  Long licenseUploadId, Long fleetDriverId,
                                                  Long linkedUserId, Locale locale) {
        List<String> errors = new ArrayList<>();

        boolean nationalIdValid = nationalIdUploadId == null || nationalIdUploadId < 1
                || fleetFileUploadHelper.validateDriverDocumentReference(nationalIdUploadId, fleetDriverId, linkedUserId, locale);
        boolean passportValid = passportUploadId == null || passportUploadId < 1
                || fleetFileUploadHelper.validateDriverDocumentReference(passportUploadId, fleetDriverId, linkedUserId, locale);

        if (!nationalIdValid || !passportValid) {
            errors.add(fleetFileUploadHelper.fileUploadInvalidMessage(locale));
        }

        if (licenseUploadId != null && licenseUploadId > 0) {
            boolean licenseValid = fleetFileUploadHelper.validateDriverDocumentReference(
                    licenseUploadId, fleetDriverId, linkedUserId, locale);
            if (!licenseValid) {
                errors.add(fleetFileUploadHelper.fileUploadInvalidMessage(locale));
            }
        }

        return errors;
    }

    // ============================================================
    // Response helpers
    // ============================================================

    private FleetDriverResponse successResponse(int statusCode, String message) {
        FleetDriverResponse response = new FleetDriverResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private FleetDriverResponse errorResponse(int statusCode, String message) {
        return errorResponse(statusCode, message, new ArrayList<>());
    }

    private FleetDriverResponse errorResponse(int statusCode, String message, List<String> errors) {
        FleetDriverResponse response = new FleetDriverResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
