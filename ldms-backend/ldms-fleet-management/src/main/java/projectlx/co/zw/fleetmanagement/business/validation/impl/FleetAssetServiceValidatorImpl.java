package projectlx.co.zw.fleetmanagement.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.co.zw.fleetmanagement.business.validation.api.FleetAssetServiceValidator;
import projectlx.co.zw.fleetmanagement.utils.enums.FleetAssetStatus;
import projectlx.co.zw.fleetmanagement.utils.enums.FleetAssetType;
import projectlx.co.zw.fleetmanagement.utils.enums.FleetOwnershipType;
import projectlx.co.zw.fleetmanagement.utils.enums.I18Code;
import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetAssetRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetAssetRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
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
                errors, locale);
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    private void validateCommonFields(String assetType, String ownershipType, String registration, String makeModel,
                                      String status, Long contractedTransporterOrganizationId,
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
    }
}
