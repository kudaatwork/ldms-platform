package projectlx.shipment.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.shipment.management.business.validator.api.ShipmentServiceValidator;
import projectlx.shipment.management.utils.enums.I18Code;
import projectlx.shipment.management.utils.requests.AllocateShipmentRequest;
import projectlx.shipment.management.utils.requests.AssignTransportCompanyRequest;
import projectlx.shipment.management.utils.requests.ShipmentMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.UpdateShipmentStatusRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class ShipmentServiceValidatorImpl implements ShipmentServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentServiceValidatorImpl.class);
    private final MessageService messageService;

    @Override
    public ValidatorDto isAllocateShipmentRequestValid(AllocateShipmentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: AllocateShipmentRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getShipmentId() == null || request.getShipmentId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"shipmentId"}, locale));
        }
        if (request.getFleetDriverId() == null || request.getFleetDriverId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FLEET_DRIVER_REQUIRED.getCode(), new String[]{}, locale));
        }
        if (request.getFleetAssetId() == null || request.getFleetAssetId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FLEET_ASSET_REQUIRED.getCode(), new String[]{}, locale));
        }

        return buildResult(errors);
    }

    @Override
    public ValidatorDto isAssignTransportCompanyRequestValid(AssignTransportCompanyRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: AssignTransportCompanyRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getShipmentId() == null || request.getShipmentId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"shipmentId"}, locale));
        }
        if (request.getTransportCompanyOrganizationId() == null || request.getTransportCompanyOrganizationId() < 1) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_TRANSPORT_COMPANY_REQUIRED.getCode(), new String[]{}, locale));
        }

        return buildResult(errors);
    }

    @Override
    public ValidatorDto isUpdateShipmentStatusRequestValid(UpdateShipmentStatusRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: UpdateShipmentStatusRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getShipmentId() == null || request.getShipmentId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"shipmentId"}, locale));
        }
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"status"}, locale));
        }

        return buildResult(errors);
    }

    @Override
    public ValidatorDto isShipmentMultipleFiltersRequestValid(ShipmentMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            return new ValidatorDto(true, null, new ArrayList<>());
        }
        if (request.getOrganizationId() != null && request.getOrganizationId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"organizationId"}, locale));
        }

        return buildResult(errors);
    }

    private ValidatorDto buildResult(List<String> errors) {
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, new ArrayList<>());
        }
        return new ValidatorDto(false, null, errors);
    }
}
