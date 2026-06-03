package projectlx.co.zw.organizationmanagement.utils.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum I18Code {

    KYC_INVALID_TRANSITION("org.kyc.invalidTransition"),
    KYC_NOT_ASSIGNED_APPROVER("org.kyc.notAssignedApprover"),
    KYC_SAME_APPROVER("org.kyc.sameApprover"),
    ORG_NOT_FOUND("org.notFound"),
    ORG_DELETED("org.deleted"),
    ORG_EMAIL_EXISTS("org.emailExists"),
    ORG_VALIDATION_FAILED("org.validationFailed"),
    ORG_FORBIDDEN_CUSTOMERS("org.forbidden.customers"),
    ORG_FORBIDDEN_TRANSPORTER_LINK("org.forbidden.transporterLink"),
    ORG_FORBIDDEN_CUSTOMER_LINK("org.forbidden.customerLink"),
    ORG_FORBIDDEN_CLEARING_AGENT_LINK("org.forbidden.clearingAgentLink"),
    ORG_FORBIDDEN_SUPPLIER_LINK("org.forbidden.supplierLink"),
    ORG_UPDATE_FORBIDDEN_STATUS("org.forbidden.updateStatus"),
    ORG_BRANCH_INVALID("org.branch.invalid"),
    ORG_CUSTOMER_REGISTER_INVALID("org.customer.registerInvalid"),
    ORG_DOCUMENT_UPLOAD_FAILED("org.document.uploadFailed"),
    INDUSTRY_NOT_FOUND("org.industry.notFound"),
    INDUSTRY_NAME_EXISTS("org.industry.nameExists"),
    INDUSTRY_VALIDATION_FAILED("org.industry.validationFailed"),
    INDUSTRY_IN_USE("org.industry.inUse"),
    INDUSTRY_CREATED("org.industry.created"),
    INDUSTRY_UPDATED("org.industry.updated"),
    INDUSTRY_DELETED("org.industry.deleted"),
    INDUSTRY_RETRIEVED("org.industry.retrieved"),
    BRANCH_NOT_FOUND("org.branch.notFound"),
    BRANCH_CREATED("org.branch.created"),
    BRANCH_UPDATED("org.branch.updated"),
    BRANCH_DELETED("org.branch.deleted"),
    BRANCH_RETRIEVED("org.branch.retrieved"),
    BRANCH_VALIDATION_FAILED("org.branch.validationFailed"),
    AGENT_NOT_FOUND("org.agent.notFound"),
    AGENT_CREATED("org.agent.created"),
    AGENT_UPDATED("org.agent.updated"),
    AGENT_DELETED("org.agent.deleted"),
    AGENT_RETRIEVED("org.agent.retrieved"),
    AGENT_VALIDATION_FAILED("org.agent.validationFailed"),
    EXPORT_FAILED("org.export.failed"),
    IMPORT_SUCCESS("org.import.success"),
    IMPORT_FAILED("org.import.failed");

    private final String code;

    public String getCode() {
        return code;
    }
}
