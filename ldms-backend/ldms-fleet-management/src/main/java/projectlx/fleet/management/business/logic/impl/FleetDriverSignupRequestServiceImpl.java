package projectlx.fleet.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.fleet.management.business.auditable.api.FleetDriverServiceAuditable;
import projectlx.fleet.management.business.auditable.api.FleetDriverSignupRequestServiceAuditable;
import projectlx.fleet.management.business.logic.api.FleetDriverSignupRequestService;
import projectlx.fleet.management.business.logic.support.CallerOrganizationResolver;
import projectlx.fleet.management.business.logic.support.FleetDriverOnboardingSupport;
import projectlx.fleet.management.business.validator.api.FleetDriverSignupRequestServiceValidator;
import projectlx.fleet.management.model.FleetDriver;
import projectlx.fleet.management.model.FleetDriverSignupRequest;
import projectlx.fleet.management.repository.FleetDriverRepository;
import projectlx.fleet.management.repository.FleetDriverSignupRequestRepository;
import projectlx.fleet.management.business.logic.support.FleetDriverSignupDocumentSupport;
import projectlx.fleet.management.utils.dtos.FleetDriverSignupRequestDto;
import projectlx.fleet.management.utils.enums.DriverEmploymentType;
import projectlx.fleet.management.utils.enums.DriverSignupRequestStatus;
import projectlx.fleet.management.utils.enums.DriverSignupDocumentSlot;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.requests.CreateFleetDriverSignupRequest;
import projectlx.fleet.management.utils.responses.FleetDriverSignupRequestResponse;
import projectlx.co.zw.shared_library.utils.dtos.FileUploadDto;
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
public class FleetDriverSignupRequestServiceImpl implements FleetDriverSignupRequestService {

    private final FleetDriverSignupRequestServiceValidator validator;
    private final FleetDriverSignupRequestServiceAuditable auditable;
    private final FleetDriverSignupRequestRepository repository;
    private final MessageService messageService;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final FleetDriverRepository fleetDriverRepository;
    private final FleetDriverServiceAuditable fleetDriverServiceAuditable;
    private final FleetDriverOnboardingSupport fleetDriverOnboardingSupport;
    private final FleetDriverSignupDocumentSupport fleetDriverSignupDocumentSupport;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FileUploadResponse uploadSignupDocument(Long stagingSessionId, DriverSignupDocumentSlot slot,
                                                   MultipartFile file, Locale locale) {
        if (stagingSessionId == null || stagingSessionId < 1 || slot == null || file == null || file.isEmpty()) {
            FileUploadResponse response = new FileUploadResponse();
            response.setStatusCode(400);
            response.setSuccess(false);
            response.setMessage(messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_SIGNUP_DOCUMENT_UPLOAD_FAILED.getCode(), new String[]{}, locale));
            return response;
        }
        Long uploadId = fleetDriverSignupDocumentSupport.uploadStagingDocument(
                stagingSessionId, slot, file, locale);
        if (uploadId == null || uploadId < 1) {
            FileUploadResponse response = new FileUploadResponse();
            response.setStatusCode(400);
            response.setSuccess(false);
            response.setMessage(messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_SIGNUP_DOCUMENT_UPLOAD_FAILED.getCode(), new String[]{}, locale));
            return response;
        }
        FileUploadResponse response = new FileUploadResponse();
        response.setStatusCode(201);
        response.setSuccess(true);
        response.setMessage(messageService.getMessage(
                I18Code.MESSAGE_DRIVER_SIGNUP_DOCUMENT_UPLOAD_SUCCESS.getCode(), new String[]{}, locale));
        FileUploadDto dto = new FileUploadDto();
        dto.setId(uploadId);
        response.setFileUploadDto(dto);
        return response;
    }

    /**
     * Submit driver signup request.
     *
     * Flow:
     * 1. Validate all required fields.
     * 2. Guard against duplicate pending requests for the same email.
     * 3. Persist the request as PENDING.
     * 4. Return success with persisted DTO.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetDriverSignupRequestResponse submitSignupRequest(CreateFleetDriverSignupRequest request, Locale locale) {

        // ============================================================
        // STEP 1: Validate request fields
        // ============================================================
        ValidatorDto validation = validator.isCreateSignupRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(
                            I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_SUBMIT_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages().isEmpty() ? null : validation.getErrorMessages().get(0));
        }

        Long stagingSessionId = request.getStagingSessionId();
        if (!validateSignupDocuments(request, stagingSessionId, locale)) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_SIGNUP_DOCUMENTS_INVALID.getCode(), new String[]{}, locale), null);
        }

        // ============================================================
        // STEP 2: Guard — reject duplicate pending email
        // ============================================================
        String email = request.getEmail().trim().toLowerCase();
        boolean alreadyPending = repository.existsByEmailAndStatusAndEntityStatusNot(
                email, DriverSignupRequestStatus.PENDING, EntityStatus.DELETED);
        if (alreadyPending) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_EMAIL_ALREADY_PENDING.getCode(), new String[]{}, locale), null);
        }

        // ============================================================
        // STEP 3: Persist signup request
        // ============================================================
        String companyCode = StringUtils.hasText(request.getCompanyCode())
                ? request.getCompanyCode().trim() : null;
        String signupType = StringUtils.hasText(companyCode) ? "COMPANY" : "FREELANCE";

        // For COMPANY requests, try to resolve orgId from companyCode (MVP: numeric = orgId)
        Long organizationId = null;
        if ("COMPANY".equals(signupType) && companyCode != null) {
            try {
                organizationId = Long.parseLong(companyCode);
            } catch (NumberFormatException ignored) {
                // Non-numeric company codes are kept as-is; org resolution happens at approval time
            }
        }

        LocalDateTime now = LocalDateTime.now();
        FleetDriverSignupRequest entity = new FleetDriverSignupRequest();
        entity.setFirstName(request.getFirstName().trim());
        entity.setLastName(request.getLastName().trim());
        entity.setEmail(email);
        entity.setPhoneNumber(request.getPhoneNumber().trim());
        entity.setLicenseNumber(request.getLicenseNumber().trim());
        entity.setLicenseClass(request.getLicenseClass() != null ? request.getLicenseClass().trim() : null);
        entity.setNationalIdNumber(request.getNationalIdNumber().trim());
        entity.setStagingSessionId(stagingSessionId);
        entity.setNationalIdFrontUploadId(request.getNationalIdFrontUploadId());
        entity.setNationalIdBackUploadId(request.getNationalIdBackUploadId());
        entity.setLicenseFrontUploadId(request.getLicenseFrontUploadId());
        entity.setLicenseBackUploadId(request.getLicenseBackUploadId());
        entity.setCompanyCode(companyCode);
        entity.setSignupType(signupType);
        entity.setOrganizationId(organizationId);
        entity.setStatus(DriverSignupRequestStatus.PENDING);
        entity.setEntityStatus(EntityStatus.ACTIVE);
        entity.setCreatedAt(now);
        entity.setCreatedBy("public");

        FleetDriverSignupRequest saved = auditable.create(entity, locale, "public");
        log.info("Driver signup request submitted: id={} email={} companyCode={}",
                saved.getId(), saved.getEmail(), saved.getCompanyCode());

        // ============================================================
        // STEP 4: Build response
        // ============================================================
        FleetDriverSignupRequestResponse response = successResponse(201, messageService.getMessage(
                I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_SUBMIT_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverSignupRequestDto(toDto(saved));
        return response;
    }

    // ============================================================
    // List / Approve / Reject
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public FleetDriverSignupRequestResponse listPendingForOrganization(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale), null);
        }
        List<FleetDriverSignupRequest> list = repository
                .findByOrganizationIdAndStatusAndEntityStatusNotOrderByIdDesc(
                        organizationId, DriverSignupRequestStatus.PENDING, EntityStatus.DELETED);
        FleetDriverSignupRequestResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverSignupRequestDtoList(list.stream().map(this::toDto).toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FleetDriverSignupRequestResponse listFreelanceMarketplace(Locale locale) {
        List<FleetDriverSignupRequest> list = repository
                .findBySignupTypeAndStatusAndEntityStatusNotOrderByIdDesc(
                        "FREELANCE", DriverSignupRequestStatus.PENDING, EntityStatus.DELETED);
        FleetDriverSignupRequestResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverSignupRequestDtoList(list.stream().map(this::toDto).toList());
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetDriverSignupRequestResponse approveSignupRequest(Long id, Locale locale, String username) {

        // ============================================================
        // STEP 1: Load and validate signup request
        // ============================================================
        FleetDriverSignupRequest signupRequest = repository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElse(null);
        if (signupRequest == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_NOT_FOUND.getCode(), new String[]{}, locale), null);
        }
        if (signupRequest.getStatus() != DriverSignupRequestStatus.PENDING) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_NOT_PENDING.getCode(), new String[]{}, locale), null);
        }
        if (!hasCompleteDocuments(signupRequest)) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_SIGNUP_DOCUMENTS_REQUIRED.getCode(), new String[]{}, locale), null);
        }

        // ============================================================
        // STEP 2: Resolve target organisation
        // ============================================================
        Long organizationId;
        if ("COMPANY".equals(signupRequest.getSignupType())) {
            organizationId = resolveOrganizationFromCompanyCode(signupRequest.getCompanyCode());
            if (organizationId == null) {
                organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
            }
        } else {
            organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        }
        if (organizationId == null || organizationId < 1) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale), null);
        }

        // ============================================================
        // STEP 3: Create FleetDriver from signup request
        // ============================================================
        FleetDriver driver = new FleetDriver();
        driver.setOrganizationId(organizationId);
        driver.setFirstName(signupRequest.getFirstName());
        driver.setLastName(signupRequest.getLastName());
        driver.setPhoneNumber(signupRequest.getPhoneNumber());
        driver.setLicenseNumber(signupRequest.getLicenseNumber());
        driver.setLicenseClass(signupRequest.getLicenseClass());
        driver.setNationalIdNumber(signupRequest.getNationalIdNumber());
        driver.setNationalIdUploadId(signupRequest.getNationalIdFrontUploadId());
        driver.setNationalIdBackUploadId(signupRequest.getNationalIdBackUploadId());
        driver.setLicenseUploadId(signupRequest.getLicenseFrontUploadId());
        driver.setLicenseBackUploadId(signupRequest.getLicenseBackUploadId());
        if ("FREELANCE".equals(signupRequest.getSignupType())) {
            driver.setEmploymentType(DriverEmploymentType.POOL);
            driver.setMarketplaceVisible(true);
        } else {
            driver.setEmploymentType(DriverEmploymentType.EMPLOYED);
            driver.setMarketplaceVisible(false);
        }
        driver.setEntityStatus(EntityStatus.ACTIVE);
        driver.setCreatedAt(LocalDateTime.now());
        driver.setCreatedBy(username);

        FleetDriver savedDriver = fleetDriverServiceAuditable.create(driver, locale, username);

        // ============================================================
        // STEP 4: Provision platform user + send credentials email
        // ============================================================
        try {
            UserResponse userResponse = fleetDriverOnboardingSupport
                    .provisionAndNotify(savedDriver, signupRequest.getEmail(), locale, username);
            if (userResponse.isSuccess() && userResponse.getUserDto() != null
                    && userResponse.getUserDto().getId() != null) {
                savedDriver.setUserId(userResponse.getUserDto().getId());
                savedDriver.setModifiedAt(LocalDateTime.now());
                savedDriver.setModifiedBy(username);
                fleetDriverServiceAuditable.update(savedDriver, locale, username);
            }
        } catch (Exception ex) {
            log.warn("Driver platform provisioning failed for signupRequestId={}: {}",
                    signupRequest.getId(), ex.getMessage());
        }

        // ============================================================
        // STEP 5: Mark signup request as approved
        // ============================================================
        signupRequest.setStatus(DriverSignupRequestStatus.APPROVED);
        signupRequest.setOrganizationId(organizationId);
        signupRequest.setModifiedAt(LocalDateTime.now());
        signupRequest.setModifiedBy(username);
        auditable.update(signupRequest, locale, username);

        log.info("Driver signup request id={} approved, driverId={}", id, savedDriver.getId());

        FleetDriverSignupRequestResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_APPROVE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverSignupRequestDto(toDto(signupRequest));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetDriverSignupRequestResponse rejectSignupRequest(Long id, Locale locale, String username) {
        FleetDriverSignupRequest signupRequest = repository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElse(null);
        if (signupRequest == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_NOT_FOUND.getCode(), new String[]{}, locale), null);
        }
        if (signupRequest.getStatus() != DriverSignupRequestStatus.PENDING) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_NOT_PENDING.getCode(), new String[]{}, locale), null);
        }
        signupRequest.setStatus(DriverSignupRequestStatus.REJECTED);
        signupRequest.setModifiedAt(LocalDateTime.now());
        signupRequest.setModifiedBy(username);
        auditable.update(signupRequest, locale, username);

        log.info("Driver signup request id={} rejected by {}", id, username);

        FleetDriverSignupRequestResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_DRIVER_SIGNUP_REQUEST_REJECT_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetDriverSignupRequestDto(toDto(signupRequest));
        return response;
    }

    private boolean validateSignupDocuments(CreateFleetDriverSignupRequest request, Long stagingSessionId,
                                            Locale locale) {
        if (!hasUploadIds(request.getNationalIdFrontUploadId(), request.getNationalIdBackUploadId(),
                request.getLicenseFrontUploadId(), request.getLicenseBackUploadId())) {
            return false;
        }
        return fleetDriverSignupDocumentSupport.validateStagingDocument(
                        request.getNationalIdFrontUploadId(), stagingSessionId, locale)
                && fleetDriverSignupDocumentSupport.validateStagingDocument(
                        request.getNationalIdBackUploadId(), stagingSessionId, locale)
                && fleetDriverSignupDocumentSupport.validateStagingDocument(
                        request.getLicenseFrontUploadId(), stagingSessionId, locale)
                && fleetDriverSignupDocumentSupport.validateStagingDocument(
                        request.getLicenseBackUploadId(), stagingSessionId, locale);
    }

    private static boolean hasCompleteDocuments(FleetDriverSignupRequest entity) {
        return hasUploadIds(entity.getNationalIdFrontUploadId(), entity.getNationalIdBackUploadId(),
                entity.getLicenseFrontUploadId(), entity.getLicenseBackUploadId());
    }

    private static boolean hasUploadIds(Long nationalIdFront, Long nationalIdBack,
                                        Long licenseFront, Long licenseBack) {
        return nationalIdFront != null && nationalIdFront > 0
                && nationalIdBack != null && nationalIdBack > 0
                && licenseFront != null && licenseFront > 0
                && licenseBack != null && licenseBack > 0;
    }

    // ============================================================
    // Company code → orgId resolution (MVP: numeric = orgId)
    // ============================================================

    private Long resolveOrganizationFromCompanyCode(String companyCode) {
        if (!StringUtils.hasText(companyCode)) {
            return null;
        }
        try {
            return Long.parseLong(companyCode.trim());
        } catch (NumberFormatException ex) {
            log.debug("Non-numeric company code '{}'; will use caller org", companyCode);
            return null;
        }
    }

    // ============================================================
    // Mapping helpers
    // ============================================================

    private FleetDriverSignupRequestDto toDto(FleetDriverSignupRequest entity) {
        FleetDriverSignupRequestDto dto = new FleetDriverSignupRequestDto();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setLicenseNumber(entity.getLicenseNumber());
        dto.setLicenseClass(entity.getLicenseClass());
        dto.setNationalIdNumber(entity.getNationalIdNumber());
        dto.setCompanyCode(entity.getCompanyCode());
        dto.setSignupType(entity.getSignupType());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setStagingSessionId(entity.getStagingSessionId());
        dto.setNationalIdFrontUploadId(entity.getNationalIdFrontUploadId());
        dto.setNationalIdBackUploadId(entity.getNationalIdBackUploadId());
        dto.setLicenseFrontUploadId(entity.getLicenseFrontUploadId());
        dto.setLicenseBackUploadId(entity.getLicenseBackUploadId());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    // ============================================================
    // Response helpers
    // ============================================================

    private FleetDriverSignupRequestResponse successResponse(int statusCode, String message) {
        FleetDriverSignupRequestResponse response = new FleetDriverSignupRequestResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private FleetDriverSignupRequestResponse errorResponse(int statusCode, String message, String detail) {
        FleetDriverSignupRequestResponse response = new FleetDriverSignupRequestResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        if (detail != null) {
            response.setErrorMessages(new ArrayList<>());
            response.getErrorMessages().add(detail);
        }
        return response;
    }
}
