-- Product categories
SELECT id, name, description, entity_status
FROM product_category
WHERE name IN ('Gas Products', 'Gas Accessories');

-- Product subcategories
SELECT id, category_id, name, description, entity_status
FROM product_sub_category
WHERE name IN ('Compressed Gas Cylinders', 'Regulators and Hoses');

-- Products
SELECT id, product_code, name, supplier_id, category_id, subcategory_id, price, unit_of_measure
FROM product
WHERE product_code IN ('GAS-O2-50L', 'GAS-C2H2-40L', 'GAS-CO2-45KG', 'GAS-LPG-14KG', 'GAS-REG-SET');

-- Warehouse location
SELECT id, name, description, supplier_id, location_id
FROM warehouse_location
WHERE name = 'Main Warehouse';

-- Inventory items
SELECT id, product_id, warehouse_location_id, current_stock, reserved_quantity, unit_cost, last_purchase_cost
FROM inventory_item
WHERE product_id IN ({{productId1}}, {{productId2}}, {{productId3}}, {{productId4}}, {{productId5}});

-- Purchase requisition (PR)
SELECT id, requisition_number, organization_id, status, requested_by_user_id, project_code
FROM purchase_requisition
WHERE project_code = 'PRJ-GAS-TEST';

-- Purchase requisition lines
SELECT id, purchase_requisition_id, product_id, requested_quantity, approved_quantity, fulfillment_method
FROM purchase_requisition_line
WHERE purchase_requisition_id = {{purchaseRequisitionId}};

-- Purchase order (direct or from PR)
SELECT id, purchase_order_number, external_id, supplier_id, status, total_amount
FROM purchase_order
WHERE external_id = 'EXT-PO-GAS-001'
   OR purchase_requisition_id = {{purchaseRequisitionId}};

-- Purchase order lines
SELECT id, purchase_order_id, product_id, quantity, unit_price, received_quantity
FROM purchase_order_line
WHERE purchase_order_id = {{purchaseOrderId}};

-- Goods received voucher (GRV)
SELECT id, grv_number, purchase_order_id, warehouse_location_id, received_by_user_id, status
FROM goods_received_voucher
WHERE purchase_order_id = {{purchaseOrderId}};

-- Stock adjustments
SELECT id, inventory_item_id, quantity_delta, unit_cost, adjusted_by_user_id, reason
FROM stock_adjustments
WHERE adjusted_by_user_id = 22
ORDER BY id DESC;

-- Inventory transfers
SELECT id, transfer_number, product_id, from_location_id, to_location_id, status
FROM inventory_transfer
WHERE transfer_number = 'TR-GAS-0001';

-- Sales orders
SELECT id, sales_order_number, customer_id, status, payment_term, created_by_user_id
FROM sales_order
WHERE created_by_user_id = 22
ORDER BY id DESC;

-- Sales order lines
SELECT id, sales_order_id, product_id, quantity, unit_price, fulfilled_quantity
FROM sales_order_line
WHERE sales_order_id = {{salesOrderId}};

-- Sales reservations
SELECT id, reservation_number, product_id, warehouse_location_id, quantity_reserved, reservation_status
FROM sales_reservations
WHERE created_by_user_id = 22
ORDER BY id DESC;

-- Purchase returns
SELECT id, return_number, purchase_order_id, warehouse_location_id, returned_by_user_id
FROM purchase_return
WHERE returned_by_user_id = 22
ORDER BY id DESC;

-- Stock transaction history
SELECT id, inventory_item_id, transaction_type, quantity_change, unit_cost, reference_document_type
FROM stock_transaction_history
WHERE inventory_item_id = {{inventoryItemId1}}
ORDER BY id DESC
LIMIT 20;

-- Product documents (requires multipart upload to create)
SELECT id, product_id, name, description, expires_at
FROM product_document
WHERE name = 'Cylinder Test Certificate';
