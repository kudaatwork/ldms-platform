-- Supplier-registered customers: organisation is active and verified without platform KYC review.
-- Does not change self-signup applicants (created_via_signup = TRUE).

UPDATE organization o
    INNER JOIN organization_customer_supplier ocs ON ocs.customer_id = o.id
SET
    o.entity_status = 'ACTIVE',
    o.is_verified = TRUE,
    o.kyc_status = 'APPROVED',
    o.assigned_stage1_approver_user_id = NULL,
    o.assigned_stage1_approver_username = NULL,
    o.assigned_stage2_approver_user_id = NULL,
    o.assigned_stage2_approver_username = NULL,
    o.assigned_stage3_approver_user_id = NULL,
    o.assigned_stage3_approver_username = NULL,
    o.assigned_stage4_approver_user_id = NULL,
    o.assigned_stage4_approver_username = NULL,
    o.assigned_stage5_approver_user_id = NULL,
    o.assigned_stage5_approver_username = NULL,
    o.modified_at = CURRENT_TIMESTAMP(6),
    o.modified_by = 'SYSTEM'
WHERE o.organization_classification = 'CUSTOMER'
  AND o.entity_status <> 'DELETED'
  AND (o.created_via_signup IS NULL OR o.created_via_signup = FALSE);
