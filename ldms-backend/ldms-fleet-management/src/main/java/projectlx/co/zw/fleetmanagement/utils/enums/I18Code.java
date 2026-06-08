package projectlx.co.zw.fleetmanagement.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum I18Code {

    MESSAGE_REQUEST_NULL("fleet.request.null"),
    MESSAGE_FIELD_REQUIRED("fleet.field.required"),
    MESSAGE_ORGANIZATION_UNRESOLVED("fleet.organization.unresolved"),
    MESSAGE_ASSET_NOT_FOUND("fleet.asset.not.found"),
    MESSAGE_DRIVER_NOT_FOUND("fleet.driver.not.found"),
    MESSAGE_COMPLIANCE_NOT_FOUND("fleet.compliance.not.found"),
    MESSAGE_ASSET_CREATE_SUCCESS("fleet.asset.create.success"),
    MESSAGE_ASSET_UPDATE_SUCCESS("fleet.asset.update.success"),
    MESSAGE_ASSET_DELETE_SUCCESS("fleet.asset.delete.success"),
    MESSAGE_ASSET_LIST_SUCCESS("fleet.asset.list.success"),
    MESSAGE_ASSET_CREATE_INVALID("fleet.asset.create.invalid"),
    MESSAGE_ASSET_UPDATE_INVALID("fleet.asset.update.invalid"),
    MESSAGE_DRIVER_CREATE_SUCCESS("fleet.driver.create.success"),
    MESSAGE_DRIVER_UPDATE_SUCCESS("fleet.driver.update.success"),
    MESSAGE_DRIVER_DELETE_SUCCESS("fleet.driver.delete.success"),
    MESSAGE_DRIVER_LIST_SUCCESS("fleet.driver.list.success"),
    MESSAGE_DRIVER_CREATE_INVALID("fleet.driver.create.invalid"),
    MESSAGE_DRIVER_UPDATE_INVALID("fleet.driver.update.invalid"),
    MESSAGE_COMPLIANCE_CREATE_SUCCESS("fleet.compliance.create.success"),
    MESSAGE_COMPLIANCE_UPDATE_SUCCESS("fleet.compliance.update.success"),
    MESSAGE_COMPLIANCE_LIST_SUCCESS("fleet.compliance.list.success"),
    MESSAGE_COMPLIANCE_EXPIRING_SUCCESS("fleet.compliance.expiring.success"),
    MESSAGE_COMPLIANCE_CREATE_INVALID("fleet.compliance.create.invalid"),
    MESSAGE_COMPLIANCE_UPDATE_INVALID("fleet.compliance.update.invalid"),
    MESSAGE_COMPLIANCE_SUBJECT_NOT_FOUND("fleet.compliance.subject.not.found"),
    MESSAGE_FILE_UPLOAD_INVALID("fleet.file.upload.invalid");

    private final String code;
}
