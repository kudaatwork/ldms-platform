package projectlx.fleet.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.fleet.management.business.logic.support.FleetRequiredComplianceSupport;
import projectlx.fleet.management.business.validator.api.FleetAssetServiceValidator;
import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.utils.enums.ComplianceType;
import projectlx.fleet.management.utils.enums.FleetAssetStatus;
import projectlx.fleet.management.utils.enums.FleetAssetType;
import projectlx.fleet.management.utils.enums.FleetContractScope;
import projectlx.fleet.management.utils.enums.FleetOwnershipType;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.requests.CompleteFleetAssetRegistrationRequest;
import projectlx.fleet.management.utils.requests.CreateFleetAssetRequest;
import projectlx.fleet.management.utils.requests.EditFleetAssetRequest;
import projectlx.fleet.management.utils.requests.FleetAssetRegistrationDocumentItem;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RequiredArgsConstructor
public class FleetAssetServiceValidatorImpl implements FleetAssetServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(FleetAssetServiceValidatorImpl.class);
    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateFleetAssetRequestValid(CreateFleetAssetRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: create fleet asset request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        validateCommonFields(request.getAssetType(), request.getOwnershipType(), request.getRegistration(),
                request.getMakeModel(), request.getStatus(), request.getContractedTransporterOrganizationId(),
                request.getContractScope(), request.getJobReference(),
                request.getContractStartDate(), request.getContractEndDate(),
                errors, locale);
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isEditFleetAssetRequestValid(EditFleetAssetRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: edit fleet asset request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getId() == null || request.getId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"id"}, locale));
        }
        validateCommonFields(request.getAssetType(), request.getOwnershipType(), request.getRegistration(),
                request.getMakeModel(), request.getStatus(), request.getContractedTransporterOrganizationId(),
                request.getContractScope(), request.getJobReference(),
                request.getContractStartDate(), request.getContractEndDate(),
                errors, locale);
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isCompleteRegistrationRequestValid(CompleteFleetAssetRegistrationRequest request,
                                                           FleetAsset asset,
                                                           Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: complete registration request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (asset == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_ASSET_NOT_FOUND.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        List<FleetAssetRegistrationDocumentItem> docs = request.getDocuments();
        if (docs == null || docs.isEmpty()) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ASSET_MISSING_REQUIRED_DOCUMENTS.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        Set<ComplianceType> provided = new HashSet<>();
        for (FleetAssetRegistrationDocumentItem doc : docs) {
            if (doc.getComplianceType() == null || doc.getComplianceType().isBlank()) {
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"complianceType"}, locale));
                continue;
            }
            ComplianceType type;
            try {
                type = ComplianceType.valueOf(doc.getComplianceType().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"complianceType"}, locale));
                continue;
            }
            if (!provided.add(type)) {
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_ASSET_DUPLICATE_COMPLIANCE_TYPE.getCode(),
                        new String[]{type.name()}, locale));
            }
            if (doc.getFileUploadId() == null || doc.getFileUploadId() < 1) {
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"fileUploadId"}, locale));
            }
        }

        Set<ComplianceType> required = FleetRequiredComplianceSupport.requiredForAsset(asset);
        if (!provided.containsAll(required)) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ASSET_MISSING_REQUIRED_DOCUMENTS.getCode(), new String[]{}, locale));
        }

        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    private void validateCommonFields(String assetType, String ownershipType, String registration, String makeModel,
                                      String status, Long contractedTransporterOrganizationId,
                                      String contractScope, String jobReference,
                                      String contractStartDate, String contractEndDate,
                                      List<String> errors, Locale locale) {
        if (assetType == null || assetType.isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"assetType"}, locale));
        } else {
            try {
                FleetAssetType.valueOf(assetType.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"assetType"}, locale));
            }
        }
        if (ownershipType == null || ownershipType.isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"ownershipType"}, locale));
        } else {
            try {
                FleetOwnershipType parsed = FleetOwnershipType.valueOf(ownershipType.trim().toUpperCase());
                if (parsed == FleetOwnershipType.CONTRACTED
                        && (contractedTransporterOrganizationId == null || contractedTransporterOrganizationId < 1)) {
                    errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(),
                            new String[]{"contractedTransporterOrganizationId"}, locale));
                }
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"ownershipType"}, locale));
            }
        }
        if (registration == null || registration.isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"registration"}, locale));
        }
        if (makeModel == null || makeModel.isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"makeModel"}, locale));
        }
        if (status != null && !status.isBlank()) {
            try {
                FleetAssetStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"status"}, locale));
            }
        }
        // Contract scope validation
        FleetContractScope resolvedScope = FleetContractScope.LONG_TERM;
        if (contractScope != null && !contractScope.isBlank()) {
            try {
                resolvedScope = FleetContractScope.valueOf(contractScope.trim().toUpperCase());
                if (resolvedScope == FleetContractScope.JOB && (jobReference == null || jobReference.isBlank())) {
                    errors.add(messageService.getMessage(
                            I18Code.MESSAGE_ASSET_JOB_REFERENCE_REQUIRED.getCode(), new String[]{}, locale));
                }
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_ASSET_CONTRACT_SCOPE_INVALID.getCode(), new String[]{}, locale));
            }
        }

        FleetOwnershipType resolvedOwnership = null;
        if (ownershipType != null && !ownershipType.isBlank()) {
            try {
                resolvedOwnership = FleetOwnershipType.valueOf(ownershipType.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // handled above
            }
        }

        if (resolvedOwnership == FleetOwnershipType.CONTRACTED && resolvedScope == FleetContractScope.LONG_TERM) {
            if (contractStartDate == null || contractStartDate.isBlank()) {
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_ASSET_CONTRACT_START_REQUIRED.getCode(), new String[]{}, locale));
            } else {
                try {
                    LocalDate start = LocalDate.parse(contractStartDate.trim());
                    if (contractEndDate != null && !contractEndDate.isBlank()) {
                        LocalDate end = LocalDate.parse(contractEndDate.trim());
                        if (end.isBefore(start)) {
                            errors.add(messageService.getMessage(
                                    I18Code.MESSAGE_ASSET_CONTRACT_END_BEFORE_START.getCode(), new String[]{}, locale));
                        }
                    }
                } catch (DateTimeParseException ex) {
                    errors.add(messageService.getMessage(
                            I18Code.MESSAGE_ASSET_CONTRACT_START_REQUIRED.getCode(), new String[]{}, locale));
                }
            }
        }
    }
}
