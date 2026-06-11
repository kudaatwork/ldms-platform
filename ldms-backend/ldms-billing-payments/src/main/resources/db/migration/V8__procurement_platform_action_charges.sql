-- Procurement workflow platform usage charges (deducted from prepaid wallet when active).

INSERT INTO platform_action_charge (action_code, display_name, description, charge_cents, category, active, entity_status, created_at, created_by)
VALUES
    ('PROCUREMENT_PR_APPROVE', 'Purchase requisition approval', 'Each internal approval stage on a purchase requisition', 5, 'ORDERS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('PROCUREMENT_QUOTE_SUBMIT', 'Supplier quote submitted', 'Supplier submits a quote against a published requisition', 6, 'ORDERS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('PROCUREMENT_PO_CUSTOMER_APPROVE', 'PO customer approval', 'Each customer approval stage on a purchase order', 8, 'ORDERS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('PROCUREMENT_PO_SUPPLIER_APPROVE', 'PO supplier approval', 'Each supplier approval stage on a purchase order', 8, 'ORDERS', 1, 'ACTIVE', NOW(6), 'SYSTEM'),
    ('PROCUREMENT_SO_APPROVE', 'Sales order approval', 'Each supplier approval stage on a sales order', 6, 'ORDERS', 1, 'ACTIVE', NOW(6), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    charge_cents = VALUES(charge_cents),
    category = VALUES(category),
    active = VALUES(active),
    modified_at = NOW(6),
    modified_by = 'SYSTEM';
