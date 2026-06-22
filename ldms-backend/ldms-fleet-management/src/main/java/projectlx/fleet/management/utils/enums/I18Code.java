package projectlx.fleet.management.utils.enums;

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
    MESSAGE_FILE_UPLOAD_INVALID("fleet.file.upload.invalid"),
    MESSAGE_ASSET_COMPLETE_REGISTRATION_SUCCESS("fleet.asset.complete.registration.success"),
    MESSAGE_ASSET_COMPLETE_REGISTRATION_INVALID("fleet.asset.complete.registration.invalid"),
    MESSAGE_ASSET_NOT_PENDING_COMPLIANCE("fleet.asset.not.pending.compliance"),
    MESSAGE_ASSET_MISSING_REQUIRED_DOCUMENTS("fleet.asset.missing.required.documents"),
    MESSAGE_ASSET_DUPLICATE_COMPLIANCE_TYPE("fleet.asset.duplicate.compliance.type"),
    MESSAGE_ASSET_OWNERSHIP_VALIDATION_FAILED("fleet.asset.ownership.validation.failed"),
    MESSAGE_ASSET_CONTRACT_SCOPE_INVALID("fleet.asset.contract.scope.invalid"),
    MESSAGE_ASSET_JOB_REFERENCE_REQUIRED("fleet.asset.job.reference.required"),
    MESSAGE_ASSET_CONTRACT_START_REQUIRED("fleet.asset.contract.start.required"),
    MESSAGE_ASSET_CONTRACT_END_BEFORE_START("fleet.asset.contract.end.before.start"),
    MESSAGE_ASSET_CONTRACT_DATES_OUT_OF_RANGE("fleet.asset.contract.dates.out.of.range"),
    MESSAGE_DRIVER_IDENTITY_REQUIRED("fleet.driver.identity.required"),
    MESSAGE_DRIVER_IDENTITY_DOCUMENT_REQUIRED("fleet.driver.identity.document.required"),
    MESSAGE_DRIVER_LICENSE_DOCUMENT_REQUIRED("fleet.driver.license.document.required"),
    MESSAGE_DRIVER_ADDRESS_REQUIRED("fleet.driver.address.required"),
    MESSAGE_DRIVER_PROVISION_SUCCESS("fleet.driver.provision.success"),
    MESSAGE_DRIVER_PROVISION_REISSUE_SUCCESS("fleet.driver.provision.reissue.success"),
    MESSAGE_DRIVER_PROVISION_INVALID("fleet.driver.provision.invalid"),
    MESSAGE_DRIVER_PROVISION_EMAIL_REQUIRED("fleet.driver.provision.email.required"),
    MESSAGE_DRIVER_PLATFORM_ALREADY_LINKED("fleet.driver.platform.already.linked"),
    MESSAGE_DRIVER_PLATFORM_NOT_LINKED("fleet.driver.platform.not.linked"),

    // Tracking device
    MESSAGE_TRACKING_DEVICE_NOT_FOUND("fleet.tracking.device.not.found"),
    MESSAGE_TRACKING_DEVICE_INSTALL_SUCCESS("fleet.tracking.device.install.success"),
    MESSAGE_TRACKING_DEVICE_UPDATE_SUCCESS("fleet.tracking.device.update.success"),
    MESSAGE_TRACKING_DEVICE_DELETE_SUCCESS("fleet.tracking.device.delete.success"),
    MESSAGE_TRACKING_DEVICE_SUSPEND_SUCCESS("fleet.tracking.device.suspend.success"),
    MESSAGE_TRACKING_DEVICE_LIST_SUCCESS("fleet.tracking.device.list.success"),
    MESSAGE_TRACKING_DEVICE_INSTALL_INVALID("fleet.tracking.device.install.invalid"),
    MESSAGE_TRACKING_DEVICE_UPDATE_INVALID("fleet.tracking.device.update.invalid"),
    MESSAGE_TRACKING_DEVICE_TYPE_INVALID("fleet.tracking.device.type.invalid"),
    MESSAGE_TRACKING_INTEGRATION_PROVIDER_INVALID("fleet.tracking.integration.provider.invalid"),
    MESSAGE_TRACKING_DEVICE_RESOLVED("fleet.tracking.device.resolved"),

    // Tracking integration credentials (integrator ingest keys)
    MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_INVALID("fleet.tracking.integration.credential.invalid"),
    MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_NOT_FOUND("fleet.tracking.integration.credential.not.found"),
    MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_CREATE_SUCCESS("fleet.tracking.integration.credential.create.success"),
    MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_LIST_SUCCESS("fleet.tracking.integration.credential.list.success"),
    MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_SUSPEND_SUCCESS("fleet.tracking.integration.credential.suspend.success"),
    MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_DELETE_SUCCESS("fleet.tracking.integration.credential.delete.success"),
    MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_ORG_MISMATCH("fleet.tracking.integration.credential.org.mismatch"),
    MESSAGE_TRACKING_INTEGRATION_CREDENTIAL_PROVIDER_NOT_ALLOWED("fleet.tracking.integration.credential.provider.not.allowed"),

    // Driver signup request
    MESSAGE_DRIVER_SIGNUP_REQUEST_SUBMIT_SUCCESS("fleet.driver.signup.request.submit.success"),
    MESSAGE_DRIVER_SIGNUP_REQUEST_SUBMIT_INVALID("fleet.driver.signup.request.submit.invalid"),
    MESSAGE_DRIVER_SIGNUP_REQUEST_EMAIL_ALREADY_PENDING("fleet.driver.signup.request.email.already.pending"),
    MESSAGE_DRIVER_SIGNUP_REQUEST_LIST_SUCCESS("fleet.driver.signup.request.list.success"),
    MESSAGE_DRIVER_SIGNUP_REQUEST_NOT_FOUND("fleet.driver.signup.request.not.found"),
    MESSAGE_DRIVER_SIGNUP_REQUEST_NOT_PENDING("fleet.driver.signup.request.not.pending"),
    MESSAGE_DRIVER_SIGNUP_REQUEST_APPROVE_SUCCESS("fleet.driver.signup.request.approve.success"),
    MESSAGE_DRIVER_SIGNUP_REQUEST_REJECT_SUCCESS("fleet.driver.signup.request.reject.success"),
    MESSAGE_DRIVER_SIGNUP_DOCUMENT_UPLOAD_SUCCESS("fleet.driver.signup.document.upload.success"),
    MESSAGE_DRIVER_SIGNUP_DOCUMENT_UPLOAD_FAILED("fleet.driver.signup.document.upload.failed"),
    MESSAGE_DRIVER_SIGNUP_DOCUMENTS_REQUIRED("fleet.driver.signup.documents.required"),
    MESSAGE_DRIVER_SIGNUP_DOCUMENTS_INVALID("fleet.driver.signup.documents.invalid");

    private final String code;
}
