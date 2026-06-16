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
    MESSAGE_FLEET_ASSET_REQUIRED("shipment.fleet.asset.required"),
    MESSAGE_TRANSPORT_COMPANY_REQUIRED("shipment.transport.company.required"),
    MESSAGE_TRANSPORT_COMPANY_ASSIGN_SUCCESS("shipment.transport.company.assign.success"),
    MESSAGE_TRANSPORT_COMPANY_ASSIGN_FORBIDDEN("shipment.transport.company.assign.forbidden"),
    MESSAGE_TRANSPORT_COMPANY_INVALID("shipment.transport.company.invalid"),
    MESSAGE_SHIPMENT_PENDING_FLEET_ALLOCATION("shipment.pending.fleet.allocation"),
    MESSAGE_SHIPMENT_FLEET_ALLOCATE_FORBIDDEN("shipment.fleet.allocate.forbidden"),
    MESSAGE_FLEET_DRIVER_ORG_MISMATCH("shipment.fleet.driver.org.mismatch"),

    // Border clearance
    MESSAGE_BORDER_CLEARANCE_CASE_NOT_FOUND("border.clearance.case.not.found"),
    MESSAGE_BORDER_CLEARANCE_CASE_FIND_SUCCESS("border.clearance.case.find.success"),
    MESSAGE_BORDER_CLEARANCE_CASE_LIST_SUCCESS("border.clearance.case.list.success"),
    MESSAGE_BORDER_CLEARANCE_DOCUMENT_ADD_SUCCESS("border.clearance.document.add.success"),
    MESSAGE_BORDER_CLEARANCE_CASE_SUBMIT_SUCCESS("border.clearance.case.submit.success"),
    MESSAGE_BORDER_CLEARANCE_CASE_CLEAR_SUCCESS("border.clearance.case.clear.success"),
    MESSAGE_BORDER_CLEARANCE_CASE_REJECT_SUCCESS("border.clearance.case.reject.success"),
    MESSAGE_BORDER_CLEARANCE_CASE_CLOSED("border.clearance.case.closed"),
    MESSAGE_BORDER_CLEARANCE_INVALID_STATUS_TRANSITION("border.clearance.invalid.status.transition"),
    MESSAGE_BORDER_CLEARANCE_FILE_UPLOAD_REQUIRED("border.clearance.file.upload.required"),
    MESSAGE_BORDER_CLEARANCE_DOCUMENT_TYPE_REQUIRED("border.clearance.document.type.required"),
    MESSAGE_BORDER_CLEARANCE_DOCUMENT_TYPE_INVALID("border.clearance.document.type.invalid");

    private final String code;
}
