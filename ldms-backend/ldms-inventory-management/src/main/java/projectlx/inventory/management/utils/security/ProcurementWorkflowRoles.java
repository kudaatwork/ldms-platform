package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProcurementWorkflowRoles {

    APPROVE_PR_INTERNAL_STAGE("APPROVE_PR_INTERNAL_STAGE", "Approves an internal approval stage on a purchase requisition"),
    PUBLISH_PR_TO_SUPPLIER("PUBLISH_PR_TO_SUPPLIER", "Publishes an approved PR to the preferred supplier"),
    VIEW_SUPPLIER_VISIBLE_REQUISITIONS("VIEW_SUPPLIER_VISIBLE_REQUISITIONS", "Views PRs published to the supplier organisation"),
    SUBMIT_SUPPLIER_QUOTE("SUBMIT_SUPPLIER_QUOTE", "Submits a supplier quote against a published PR"),
    ACKNOWLEDGE_SUPPLIER_QUOTE("ACKNOWLEDGE_SUPPLIER_QUOTE", "Acknowledges a supplier quote, moving PR to CUSTOMER_ACKNOWLEDGED"),
    APPROVE_PO_CUSTOMER_STAGE("APPROVE_PO_CUSTOMER_STAGE", "Approves a customer-side approval stage on a purchase order"),
    APPROVE_PO_SUPPLIER_STAGE("APPROVE_PO_SUPPLIER_STAGE", "Approves a supplier-side approval stage on a purchase order"),
    APPROVE_SO_STAGE("APPROVE_SO_STAGE", "Approves an approval stage on a sales order"),
    VIEW_SUPPLIER_QUOTES("VIEW_SUPPLIER_QUOTES", "Lists quotes submitted by the supplier organisation"),
    VIEW_CUSTOMER_QUOTES("VIEW_CUSTOMER_QUOTES", "Lists quotes received by the customer organisation"),
    VIEW_QUOTE_BY_REQUISITION("VIEW_QUOTE_BY_REQUISITION", "Views the latest supplier quote for a given requisition");

    private final String role;
    private final String description;

    @Override
    public String toString() {
        return role;
    }
}
