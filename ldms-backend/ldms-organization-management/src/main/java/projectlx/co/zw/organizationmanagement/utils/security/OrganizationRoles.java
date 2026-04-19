package projectlx.co.zw.organizationmanagement.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OrganizationRoles {

    SUBMIT_KYC("SUBMIT_KYC", "Submit organization KYC package"),
    VIEW_MY_ORGAN("VIEW_MY_ORGAN", "View own organization"),
    UPDATE_MY_ORGAN("UPDATE_MY_ORGAN", "Update own organization while editable"),
    MANAGE_BRANCHES("MANAGE_BRANCHES", "Add and list branches"),
    LIST_CUSTOMERS("LIST_CUSTOMERS", "List customers where supplier"),
    REGISTER_CUSTOMER("REGISTER_CUSTOMER", "Register a customer organization"),
    LINK_TRANSPORTER("LINK_TRANSPORTER", "Link a transport company");

    private final String roleName;
    private final String description;
}
