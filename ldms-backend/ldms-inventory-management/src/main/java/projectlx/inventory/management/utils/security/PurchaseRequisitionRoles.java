package projectlx.inventory.management.utils.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PurchaseRequisitionRoles {

    // === CREATE & MANAGE OWN PRs ===
    CREATE_PURCHASE_REQUISITION("CREATE_PURCHASE_REQUISITION", "Creates a new purchase requisition"),
    VIEW_PURCHASE_REQUISITION_BY_ID("VIEW_PURCHASE_REQUISITION_BY_ID", "Views purchase requisition by ID"),
    VIEW_OWN_PURCHASE_REQUISITIONS("VIEW_OWN_PURCHASE_REQUISITIONS", "Views own purchase requisitions"),
    UPDATE_PURCHASE_REQUISITION("UPDATE_PURCHASE_REQUISITION", "Updates purchase requisition details (draft only)"),
    DELETE_PURCHASE_REQUISITION("DELETE_PURCHASE_REQUISITION", "Deletes purchase requisition (draft only)"),
    SUBMIT_PURCHASE_REQUISITION("SUBMIT_PURCHASE_REQUISITION", "Submits purchase requisition for approval"),
    CANCEL_PURCHASE_REQUISITION("CANCEL_PURCHASE_REQUISITION", "Cancels a purchase requisition"),

    // === DEPARTMENT/APPROVAL ROLES ===
    VIEW_DEPARTMENT_PURCHASE_REQUISITIONS("VIEW_DEPARTMENT_PURCHASE_REQUISITIONS", "Views all PRs in the department"),
    APPROVE_PURCHASE_REQUISITION("APPROVE_PURCHASE_REQUISITION", "Approves purchase requisitions"),
    REJECT_PURCHASE_REQUISITION("REJECT_PURCHASE_REQUISITION", "Rejects purchase requisitions"),

    // === PROCUREMENT TEAM ROLES ===
    VIEW_ALL_PURCHASE_REQUISITIONS("VIEW_ALL_PURCHASE_REQUISITIONS", "Views all purchase requisitions"),
    VIEW_PURCHASE_REQUISITIONS_BY_FILTERS("VIEW_PURCHASE_REQUISITIONS_BY_FILTERS", "Views PRs by multiple filters"),
    FULFILL_PURCHASE_REQUISITION("FULFILL_PURCHASE_REQUISITION", "Records fulfillment for PR lines"),
    CREATE_PO_FROM_PURCHASE_REQUISITION("CREATE_PO_FROM_PURCHASE_REQUISITION", "Creates PO from approved PR"),
    CLOSE_PURCHASE_REQUISITION("CLOSE_PURCHASE_REQUISITION", "Administratively closes a PR"),

    // === ADMIN ROLES ===
    AMEND_PURCHASE_REQUISITION("AMEND_PURCHASE_REQUISITION", "Creates amendments to approved PRs"),
    EXPIRE_PURCHASE_REQUISITIONS("EXPIRE_PURCHASE_REQUISITIONS", "Marks expired PRs as expired"),
    EXPORT_PURCHASE_REQUISITIONS("EXPORT_PURCHASE_REQUISITIONS", "Exports purchase requisitions");

    private final String roleName;
    private final String description;
}
