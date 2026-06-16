package projectlx.fleet.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.fleet.management.business.auditable.api.FleetAssetServiceAuditable;
import projectlx.fleet.management.business.auditable.api.FleetComplianceRecordServiceAuditable;
import projectlx.fleet.management.business.logic.api.FleetAssetService;
import projectlx.fleet.management.business.logic.support.CallerOrganizationResolver;
import projectlx.fleet.management.business.logic.support.ComplianceStatusResolver;
import projectlx.fleet.management.business.logic.support.FleetAssetRegistrationNotificationSupport;
import projectlx.fleet.management.business.logic.support.FleetFileUploadHelper;
import projectlx.fleet.management.business.logic.support.FleetMapper;
import projectlx.fleet.management.business.logic.support.FleetOwnershipValidationSupport;
import projectlx.fleet.management.business.validator.api.FleetAssetServiceValidator;
import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.model.FleetComplianceRecord;
import projectlx.fleet.management.model.FleetDriver;
import projectlx.fleet.management.repository.FleetAssetRepository;
import projectlx.fleet.management.repository.FleetDriverRepository;
import projectlx.fleet.management.utils.enums.ComplianceSubjectType;
import projectlx.fleet.management.utils.enums.ComplianceType;
import projectlx.fleet.management.utils.enums.FleetAssetStatus;
import projectlx.fleet.management.utils.enums.FleetAssetType;
import projectlx.fleet.management.utils.enums.FleetContractScope;
import projectlx.fleet.management.utils.enums.FleetOwnershipType;
import projectlx.fleet.management.utils.enums.FleetRegistrationStatus;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.requests.CompleteFleetAssetRegistrationRequest;
import projectlx.fleet.management.utils.requests.CreateFleetAssetRequest;
import projectlx.fleet.management.utils.requests.EditFleetAssetRequest;
import projectlx.fleet.management.utils.requests.FleetAssetRegistrationDocumentItem;
import projectlx.fleet.management.utils.responses.FleetAssetResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class FleetAssetServiceImpl implements FleetAssetService {

    private final FleetAssetServiceValidator fleetAssetServiceValidator;
    private final FleetAssetRepository fleetAssetRepository;
    private final FleetComplianceRecordServiceAuditable fleetComplianceRecordServiceAuditable;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final FleetAssetRegistrationNotificationSupport fleetAssetRegistrationNotificationSupport;
    private final FleetOwnershipValidationSupport fleetOwnershipValidationSupport;
    private final FleetFileUploadHelper fleetFileUploadHelper;
    private final MessageService messageService;
    private final FleetAssetServiceAuditable fleetAssetServiceAuditable;
    private final FleetDriverRepository fleetDriverRepository;
    private final int defaultExpiringSoonDays;

    @Override
    @Transactional(readOnly = true)
    public FleetAssetResponse list(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }
        List<FleetAsset> assets = fleetAssetRepository
                .findByOrganizationIdAndRegistrationStatusAndEntityStatusNotOrderByIdDesc(
                        organizationId, FleetRegistrationStatus.ACTIVE, EntityStatus.DELETED);
        FleetAssetResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_ASSET_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetAssetDtoList(assets.stream().map(FleetMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetAssetResponse create(CreateFleetAssetRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate the create request
        // ============================================================
        ValidatorDto validation = fleetAssetServiceValidator.isCreateFleetAssetRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_ASSET_CREATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Resolve caller organisation
        // ============================================================
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Validate ownership eligibility against org-management
        // ============================================================
        String ownershipError = fleetOwnershipValidationSupport.validateOwnership(
                organizationId, request.getOwnershipType(), request.getContractedTransporterOrganizationId(),
                request.getContractScope(), request.getContractStartDate(), request.getContractEndDate());
        if (ownershipError != null) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_ASSET_OWNERSHIP_VALIDATION_FAILED.getCode(),
                            new String[]{}, locale));
        }

        // ============================================================
        // STEP 4: Persist the asset as PENDING_COMPLIANCE via auditable
        // ============================================================
        FleetAsset asset = new FleetAsset();
        asset.setOrganizationId(organizationId);
        mapRequestToEntity(request, asset);
        asset.setRegistrationStatus(FleetRegistrationStatus.PENDING_COMPLIANCE);
        asset.setEntityStatus(EntityStatus.ACTIVE);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setCreatedBy(username);

        FleetAsset saved = fleetAssetServiceAuditable.create(asset, locale, username);
        log.info("Fleet asset created as PENDING_COMPLIANCE: id={} registration={} org={}",
                saved.getId(), saved.getRegistration(), organizationId);

        FleetAssetResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_ASSET_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetAssetDto(FleetMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetAssetResponse update(Long id, EditFleetAssetRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate the edit request
        // ============================================================
        if (request != null) {
            request.setId(id);
        }
        ValidatorDto validation = fleetAssetServiceValidator.isEditFleetAssetRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_ASSET_UPDATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Resolve caller organisation and load asset
        // ============================================================
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetAsset asset = fleetAssetRepository.findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (asset == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_ASSET_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        String ownershipError = fleetOwnershipValidationSupport.validateOwnership(
                organizationId, request.getOwnershipType(), request.getContractedTransporterOrganizationId(),
                request.getContractScope(), request.getContractStartDate(), request.getContractEndDate());
        if (ownershipError != null) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_ASSET_OWNERSHIP_VALIDATION_FAILED.getCode(),
                            new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Apply changes and persist via auditable
        // ============================================================
        mapEditRequestToEntity(request, asset);
        asset.setModifiedAt(LocalDateTime.now());
        asset.setModifiedBy(username);

        FleetAsset saved = fleetAssetServiceAuditable.update(asset, locale, username);
        FleetAssetResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_ASSET_UPDATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setFleetAssetDto(FleetMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetAssetResponse delete(Long id, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetAsset asset = fleetAssetRepository.findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (asset == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_ASSET_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        asset.setEntityStatus(EntityStatus.DELETED);
        asset.setModifiedAt(LocalDateTime.now());
        asset.setModifiedBy(username);
        fleetAssetServiceAuditable.delete(asset, locale);

        return successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_ASSET_DELETE_SUCCESS.getCode(), new String[]{}, locale));
    }

    /**
     * Complete fleet asset registration.
     *
     * Flow:
     * 1. Validate the complete-registration request
     * 2. Load and verify the asset is in PENDING_COMPLIANCE status
     * 3. Validate and persist each required compliance document via auditable
     * 4. Transition asset to ACTIVE registration status via auditable
     * 5. Fire post-commit registration notification
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FleetAssetResponse completeRegistration(Long id,
                                                    CompleteFleetAssetRegistrationRequest request,
                                                    Locale locale,
                                                    String username) {

        // ============================================================
        // STEP 1: Resolve caller organisation and load asset
        // ============================================================
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(),
                    new String[]{}, locale));
        }

        FleetAsset asset = fleetAssetRepository.findByIdAndOrganizationIdAndEntityStatusNot(id, organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (asset == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_ASSET_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Validate the request against asset context
        // ============================================================
        ValidatorDto validation = fleetAssetServiceValidator.isCompleteRegistrationRequestValid(request, asset, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_ASSET_COMPLETE_REGISTRATION_INVALID.getCode(),
                            new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 3: Ensure asset is pending compliance
        // ============================================================
        if (asset.getRegistrationStatus() != FleetRegistrationStatus.PENDING_COMPLIANCE) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ASSET_NOT_PENDING_COMPLIANCE.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 4: Validate file upload references and build compliance records
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        List<FleetComplianceRecord> records = new ArrayList<>();

        for (FleetAssetRegistrationDocumentItem doc : request.getDocuments()) {
            ComplianceType complianceType = ComplianceType.valueOf(doc.getComplianceType().trim().toUpperCase());

            if (doc.getFileUploadId() != null && doc.getFileUploadId() > 0) {
                boolean valid = fleetFileUploadHelper.validateFileUploadReference(
                        doc.getFileUploadId(), ComplianceSubjectType.ASSET, asset.getId(), locale);
                if (!valid) {
                    return errorResponse(400, fleetFileUploadHelper.fileUploadInvalidMessage(locale));
                }
            }

            FleetComplianceRecord record = new FleetComplianceRecord();
            record.setOrganizationId(organizationId);
            record.setSubjectType(ComplianceSubjectType.ASSET);
            record.setSubjectId(asset.getId());
            record.setComplianceType(complianceType);
            record.setFileUploadId(doc.getFileUploadId());
            LocalDateTime expiresAt = fleetFileUploadHelper.resolveExpiresAt(doc.getExpiresAt(), doc.getFileUploadId());
            record.setExpiresAt(expiresAt);
            record.setStatus(ComplianceStatusResolver.resolve(
                    expiresAt, doc.getFileUploadId(), defaultExpiringSoonDays));
            record.setEntityStatus(EntityStatus.ACTIVE);
            record.setCreatedAt(now);
            record.setCreatedBy(username);
            records.add(record);
        }

        fleetComplianceRecordServiceAuditable.createAll(records, locale, username);
        log.info("Saved {} compliance records for asset id={}", records.size(), asset.getId());

        // ============================================================
        // STEP 5: Activate the asset and trigger notification
        // ============================================================
        asset.setRegistrationStatus(FleetRegistrationStatus.ACTIVE);
        asset.setModifiedAt(now);
        asset.setModifiedBy(username);
        FleetAsset saved = fleetAssetServiceAuditable.update(asset, locale, username);

        fleetAssetRegistrationNotificationSupport.scheduleAfterCommit(saved);
        log.info("Fleet asset activated: id={} registration={}", saved.getId(), saved.getRegistration());

        FleetAssetResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_ASSET_COMPLETE_REGISTRATION_SUCCESS.getCode(),
                        new String[]{}, locale));
        response.setFleetAssetDto(FleetMapper.toDto(saved));
        return response;
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private void mapRequestToEntity(CreateFleetAssetRequest request, FleetAsset asset) {
        applyAssetFields(request.getAssetType(), request.getOwnershipType(), request.getContractedTransporterOrganizationId(),
                request.getRegistration(), request.getMakeModel(), request.getStatus(),
                request.getDriverName(), request.getFleetDriverId(),
                request.getUtilizationPct(), request.getContractScope(), request.getJobReference(),
                request.getContractStartDate(), request.getContractEndDate(), asset);
    }

    private void mapEditRequestToEntity(EditFleetAssetRequest request, FleetAsset asset) {
        applyAssetFields(request.getAssetType(), request.getOwnershipType(), request.getContractedTransporterOrganizationId(),
                request.getRegistration(), request.getMakeModel(), request.getStatus(),
                request.getDriverName(), request.getFleetDriverId(),
                request.getUtilizationPct(), request.getContractScope(), request.getJobReference(),
                request.getContractStartDate(), request.getContractEndDate(), asset);
    }

    private void applyAssetFields(String assetType, String ownershipType, Long contractedTransporterOrganizationId,
                                  String registration, String makeModel, String status, String driverName,
                                  Long fleetDriverId,
                                  BigDecimal utilizationPct, String contractScope, String jobReference,
                                  String contractStartDateRaw, String contractEndDateRaw,
                                  FleetAsset asset) {
        asset.setAssetType(FleetAssetType.valueOf(assetType.trim().toUpperCase()));
        asset.setOwnershipType(FleetOwnershipType.valueOf(ownershipType.trim().toUpperCase()));
        asset.setContractedTransporterOrganizationId(contractedTransporterOrganizationId);
        asset.setRegistration(registration.trim());
        asset.setMakeModel(makeModel.trim());
        if (status != null && !status.isBlank()) {
            asset.setStatus(FleetAssetStatus.valueOf(status.trim().toUpperCase()));
        }
        resolveDriverAssignment(asset, fleetDriverId, driverName);
        asset.setUtilizationPct(utilizationPct != null ? utilizationPct : BigDecimal.ZERO);
        if (contractScope != null && !contractScope.isBlank()) {
            asset.setContractScope(FleetContractScope.valueOf(contractScope.trim().toUpperCase()));
        } else {
            asset.setContractScope(FleetContractScope.LONG_TERM);
        }
        asset.setJobReference(jobReference);

        boolean longTermContracted = asset.getOwnershipType() == FleetOwnershipType.CONTRACTED
                && asset.getContractScope() == FleetContractScope.LONG_TERM;
        if (longTermContracted) {
            asset.setContractStartDate(parseContractDate(contractStartDateRaw));
            asset.setContractEndDate(parseContractDate(contractEndDateRaw));
        } else {
            asset.setContractStartDate(null);
            asset.setContractEndDate(null);
        }
    }

    private void resolveDriverAssignment(FleetAsset asset, Long fleetDriverId, String driverNameFallback) {
        if (fleetDriverId == null || fleetDriverId < 1) {
            if (driverNameFallback != null && !driverNameFallback.isBlank()) {
                asset.setDriverName(driverNameFallback.trim());
            } else {
                asset.setDriverName(null);
            }
            asset.setFleetDriverId(null);
            return;
        }
        FleetDriver driver = fleetDriverRepository.findByIdForReadOnly(fleetDriverId, EntityStatus.DELETED).orElse(null);
        if (driver == null) {
            asset.setFleetDriverId(null);
            if (driverNameFallback != null && !driverNameFallback.isBlank()) {
                asset.setDriverName(driverNameFallback.trim());
            }
            return;
        }
        Long assetOrg = asset.getOrganizationId();
        Long transporterOrg = asset.getContractedTransporterOrganizationId();
        Long driverOrg = driver.getOrganizationId();
        boolean allowed = driverOrg.equals(assetOrg)
                || (asset.getOwnershipType() == FleetOwnershipType.CONTRACTED
                && transporterOrg != null
                && driverOrg.equals(transporterOrg));
        if (!allowed) {
            asset.setFleetDriverId(null);
            return;
        }
        asset.setFleetDriverId(driver.getId());
        asset.setDriverName((driver.getFirstName() + " " + driver.getLastName()).trim());
    }

    private LocalDate parseContractDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private FleetAssetResponse successResponse(int statusCode, String message) {
        FleetAssetResponse response = new FleetAssetResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private FleetAssetResponse errorResponse(int statusCode, String message) {
        return errorResponse(statusCode, message, new ArrayList<>());
    }

    private FleetAssetResponse errorResponse(int statusCode, String message, List<String> errors) {
        FleetAssetResponse response = new FleetAssetResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
