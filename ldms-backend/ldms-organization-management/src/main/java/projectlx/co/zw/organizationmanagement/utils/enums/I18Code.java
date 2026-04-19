package projectlx.co.zw.organizationmanagement.utils.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum I18Code {

    KYC_INVALID_TRANSITION("org.kyc.invalidTransition"),
    ORG_NOT_FOUND("org.notFound"),
    ORG_EMAIL_EXISTS("org.emailExists"),
    ORG_VALIDATION_FAILED("org.validationFailed"),
    ORG_FORBIDDEN_CUSTOMERS("org.forbidden.customers"),
    ORG_FORBIDDEN_TRANSPORTER_LINK("org.forbidden.transporterLink"),
    ORG_UPDATE_FORBIDDEN_STATUS("org.forbidden.updateStatus"),
    ORG_BRANCH_INVALID("org.branch.invalid"),
    ORG_CUSTOMER_REGISTER_INVALID("org.customer.registerInvalid");

    private final String code;

    public String getCode() {
        return code;
    }
}
