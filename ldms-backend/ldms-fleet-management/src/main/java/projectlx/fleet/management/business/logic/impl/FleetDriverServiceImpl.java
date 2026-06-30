package projectlx.fleet.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.fleet.management.business.auditable.api.FleetDriverServiceAuditable;
import projectlx.fleet.management.business.logic.api.FleetDriverService;
import projectlx.fleet.management.business.logic.support.CallerOrganizationResolver;
import projectlx.fleet.management.business.logic.support.FleetDriverOnboardingSupport;
import projectlx.fleet.management.business.logic.support.FleetFileUploadHelper;
import projectlx.fleet.management.business.logic.support.FleetMapper;
import projectlx.fleet.management.business.logic.support.FleetOwnershipValidationSupport;
import projectlx.fleet.management.business.validator.api.FleetDriverServiceValidator;
import projectlx.fleet.management.clients.UserManagementServiceClient;
import projectlx.fleet.management.model.FleetDriver;
import projectlx.fleet.management.repository.FleetDriverRepository;
import projectlx.fleet.management.utils.enums.DriverEmploymentType;
import projectlx.fleet.management.utils.enums.FleetOwnershipType;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.requests.CreateFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetDriverRequest;
import projectlx.fleet.management.utils.requests.ProvisionFleetDriverPlatformAccessRequest;
import projectlx.fleet.management.utils.requests.ProvisionFleetDriverPlatformAccessRequest;
import projectlx.fleet.management.utils.responses.FleetDriverResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import org.springframework.util.StringUtils;

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
    private final UserManagementServiceClient userManagementServiceClient;
    private final FleetDriverOnboardingSupport fleetDriverOnboardingSupport;

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

        // ============================================================
        // STEP 3: Optionally provision platform user account
        // ============================================================
        if (Boolean.TRUE.equals(request.getProvisionPlatformAccess())
                && saved.getUserId() == null
                && StringUtils.hasText(request.getEmail())) {
            try {
                UserResponse userResponse = fleetDriverOnboardingSupport
                        .provisionAndNotify(saved, request.getEmail().trim().toLowerCase(), locale, username);
                if (userResponse.isSuccess() && userResponse.getUserDto() != null
                        && userResponse.getUserDto().getId() != null) {
                    saved.setUserId(userResponse.getUserDto().getId());
                    saved.setModifiedAt(java.time.LocalDateTime.now());
                    saved.setModifiedBy(username);
                    fleetDriverServiceAuditable.update(saved, locale, username);
                }
            } catch (Exception ex) {
                log.warn("Driver platform provisioning failed for driverId={}: {}", saved.getId(), ex.getMessage());
            }
        }

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
    public FleetDriverResponse provisionPlatformAccess(Long id, ProvisionFleetDriverPlatformAccessRequest request,
                                                       Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = fleetDriverServiceValidator.isProvisionPlatformAccessRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_PROVISION_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Load driver in caller organisation
        // ============================================================
        FleetDriver driver = fleetDriverRepository
                .findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (driver == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_DRIVER_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        boolean reissue = Boolean.TRUE.equals(request.getReissueCredentials());
        boolean linked = driver.getUserId() != null && driver.getUserId() > 0;
        if (linked && !reissue) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_PLATFORM_ALREADY_LINKED.getCode(), new String[]{}, locale));
        }
        if (!linked && reissue) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_PLATFORM_NOT_LINKED.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Provision user + email credentials
        // ============================================================
        String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        UserResponse userResponse;
        try {
            userResponse = fleetDriverOnboardingSupport.provisionAndNotify(driver, email, locale, username);
        } catch (Exception ex) {
            log.warn("Driver platform provisioning failed for driverId={}: {}", driver.getId(), ex.getMessage());
            return errorResponse(500, "Failed to provision driver platform access: " + ex.getMessage());
        }

        if (!userResponse.isSuccess()) {
            int status = userResponse.getStatusCode() > 0 ? userResponse.getStatusCode() : 400;
            return errorResponse(status,
                    userResponse.getMessage() != null ? userResponse.getMessage() : "Provisioning failed.",
                    userResponse.getErrorMessages() != null ? userResponse.getErrorMessages() : new ArrayList<>());
        }

        // ============================================================
        // STEP 4: Link userId when first-time provisioning
        // ============================================================
        FleetDriver saved = driver;
        if (!linked && userResponse.getUserDto() != null && userResponse.getUserDto().getId() != null) {
            driver.setUserId(userResponse.getUserDto().getId());
            driver.setModifiedAt(LocalDateTime.now());
            driver.setModifiedBy(username);
            saved = fleetDriverServiceAuditable.update(driver, locale, username);
        }

        String successMessage = messageService.getMessage(
                reissue
                        ? I18Code.MESSAGE_DRIVER_PROVISION_REISSUE_SUCCESS.getCode()
                        : I18Code.MESSAGE_DRIVER_PROVISION_SUCCESS.getCode(),
                new String[]{}, locale);

        FleetDriverResponse response = successResponse(200, successMessage);
        response.setFleetDriverDto(FleetMapper.toDto(saved));
        response.setTemporaryUsername(userResponse.getTemporaryUsername());
        response.setTemporaryPassword(userResponse.getTemporaryPassword());
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

    @Override
    @Transactional(readOnly = true)
    public FleetDriverResponse findByUserIdForSystem(Long userId, Locale locale) {
        if (userId == null || userId < 1) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }
        FleetDriver driver = fleetDriverRepository.findByUserIdAndEntityStatusNot(userId, EntityStatus.DELETED).orElse(null);
        if (driver == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        FleetDriverResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_DRIVER_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDto(FleetMapper.toDto(driver));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FleetDriverResponse findMyProfile(Locale locale, String username) {

        // ============================================================
        // STEP 1: Resolve platform userId from username
        // ============================================================
        Long userId = resolveUserIdFromUsername(username);
        if (userId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Look up driver profile linked to userId
        // ============================================================
        FleetDriver driver = fleetDriverRepository.findByUserIdAndEntityStatusNot(userId, EntityStatus.DELETED).orElse(null);
        if (driver == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        FleetDriverResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_DRIVER_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDto(FleetMapper.toDto(driver));
        return response;
    }

    @Override
    @Transactional
    public FleetDriverResponse updateMyProfile(EditFleetDriverRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Resolve the caller's own driver record
        // ============================================================
        Long userId = resolveUserIdFromUsername(username);
        if (userId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }
        FleetDriver driver = fleetDriverRepository
                .findByUserIdAndEntityStatusNot(userId, EntityStatus.DELETED).orElse(null);
        if (driver == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Validate (reuse edit validation against the caller's own id)
        // ============================================================
        if (request != null) {
            request.setId(driver.getId());
            request.setUserId(driver.getUserId()); // never let a driver re-link their account
            // employmentType is org-managed: we simply never apply it below, preserving the stored value
        }
        ValidatorDto validation = fleetDriverServiceValidator.isEditFleetDriverRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_UPDATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 3: Validate uploaded document references
        // ============================================================
        List<String> uploadErrors = validateUploadReferences(
                request.getNationalIdUploadId(),
                request.getPassportUploadId(),
                request.getLicenseUploadId(),
                driver.getId(),
                driver.getUserId(),
                locale);
        if (!uploadErrors.isEmpty()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DRIVER_UPDATE_INVALID.getCode(), new String[]{}, locale),
                    uploadErrors);
        }

        // ============================================================
        // STEP 4: Apply only self-editable fields and persist
        // ============================================================
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
        driver.setModifiedAt(LocalDateTime.now());
        driver.setModifiedBy(username);

        FleetDriver saved = fleetDriverServiceAuditable.update(driver, locale, username);
        FleetDriverResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_UPDATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDto(FleetMapper.toDto(saved));
        return response;
    }

    private Long resolveUserIdFromUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            UserResponse userResponse = userManagementServiceClient.findSessionProfileByUsername(username.trim());
            if (userResponse != null && userResponse.isSuccess() && userResponse.getUserDto() != null
                    && userResponse.getUserDto().getId() != null && userResponse.getUserDto().getId() > 0) {
                return userResponse.getUserDto().getId();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve userId for username {} via user-management: {}", username, ex.getMessage());
        }
        return null;
    }

    // ============================================================
    // Marketplace
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public FleetDriverResponse searchMarketplace(String term, String licenseClass, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }
        String termParam = StringUtils.hasText(term) ? term.trim() : null;
        String licenseClassParam = StringUtils.hasText(licenseClass) ? licenseClass.trim() : null;
        List<FleetDriver> drivers = fleetDriverRepository.searchMarketplace(
                EntityStatus.DELETED, organizationId, termParam, licenseClassParam);
        FleetDriverResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDtoList(drivers.stream().map(FleetMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetDriverResponse hireFromMarketplace(Long driverId, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetDriver source = fleetDriverRepository.findByIdForReadOnly(driverId, EntityStatus.DELETED).orElse(null);
        if (source == null || !Boolean.TRUE.equals(source.getMarketplaceVisible())) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_DRIVER_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }
        if (source.getUserId() == null) {
            return errorResponse(400, "Marketplace driver does not have a platform user account.");
        }
        boolean alreadyHired = fleetDriverRepository.existsByUserIdAndOrganizationIdAndEntityStatusNot(
                source.getUserId(), organizationId, EntityStatus.DELETED);
        if (alreadyHired) {
            return errorResponse(409, "This driver is already in your organisation.");
        }

        FleetDriver hired = new FleetDriver();
        hired.setOrganizationId(organizationId);
        hired.setUserId(source.getUserId());
        hired.setFirstName(source.getFirstName());
        hired.setLastName(source.getLastName());
        hired.setPhoneNumber(source.getPhoneNumber());
        hired.setLicenseNumber(source.getLicenseNumber());
        hired.setLicenseClass(source.getLicenseClass());
        hired.setEmploymentType(DriverEmploymentType.POOL);
        hired.setEntityStatus(EntityStatus.ACTIVE);
        hired.setCreatedAt(LocalDateTime.now());
        hired.setCreatedBy(username);

        FleetDriver saved = fleetDriverServiceAuditable.create(hired, locale, username);
        FleetDriverResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_DRIVER_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverDto(FleetMapper.toDto(saved));
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
