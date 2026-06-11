package projectlx.shipment.management.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum I18Code {

    MESSAGE_REQUEST_NULL("shipment.request.null"),
    MESSAGE_FIELD_REQUIRED("shipment.field.required"),
    MESSAGE_ORGANIZATION_UNRESOLVED("shipment.organization.unresolved"),
    MESSAGE_SHIPMENT_NOT_FOUND("shipment.not.found"),
    MESSAGE_SHIPMENT_ALREADY_EXISTS_FOR_TRANSFER("shipment.already.exists.for.transfer"),
    MESSAGE_SHIPMENT_CREATE_SUCCESS("shipment.create.success"),
    MESSAGE_SHIPMENT_LIST_SUCCESS("shipment.list.success"),
    MESSAGE_SHIPMENT_FIND_SUCCESS("shipment.find.success"),
    MESSAGE_SHIPMENT_ALLOCATE_SUCCESS("shipment.allocate.success"),
    MESSAGE_SHIPMENT_STATUS_UPDATE_SUCCESS("shipment.status.update.success"),
    MESSAGE_SHIPMENT_ALLOCATE_INVALID("shipment.allocate.invalid"),
    MESSAGE_SHIPMENT_ALREADY_ALLOCATED("shipment.already.allocated"),
    MESSAGE_SHIPMENT_INVALID_STATUS_TRANSITION("shipment.invalid.status.transition"),
    MESSAGE_SHIPMENT_CANCELLED("shipment.cancelled"),
    MESSAGE_SHIPMENT_CANCEL_SUCCESS("shipment.cancel.success"),
    MESSAGE_FLEET_DRIVER_REQUIRED("shipment.fleet.driver.required"),
    MESSAGE_FLEET_ASSET_REQUIRED("shipment.fleet.asset.required");

    private final String code;
}
