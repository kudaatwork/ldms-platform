-- ========================================================================
-- Flyway Migration V5: Purchase Requisition Tables (CORRECTED)
-- ========================================================================
-- Purpose: Purchase requisition workflow matching PurchaseRequisition.java
-- Reference: PurchaseRequisition.java, PurchaseRequisitionLine.java,
--            PurchaseRequisitionAmendment.java
-- ========================================================================

-- ========================================================================
-- Table: purchase_requisition
-- Entity: PurchaseRequisition.java
-- ========================================================================
CREATE TABLE purchase_requisition (
                                      id BIGINT NOT NULL AUTO_INCREMENT,

    -- Identification
                                      requisition_number VARCHAR(50) NOT NULL UNIQUE,
                                      external_reference VARCHAR(100),
                                      version INT NOT NULL DEFAULT 1,

    -- Requester information
                                      organization_id BIGINT NOT NULL COMMENT 'FK to Org Service',
                                      department_id BIGINT NOT NULL COMMENT 'FK to Org Service',
                                      requested_by_user_id BIGINT NOT NULL COMMENT 'FK to User Management Service',
                                      cost_center VARCHAR(50),
                                      project_code VARCHAR(50),

    -- Purpose & justification
                                      purpose TEXT NOT NULL,
                                      justification TEXT,
                                      priority VARCHAR(20) NOT NULL COMMENT 'ENUM: PriorityLevel (URGENT, HIGH, NORMAL, LOW)',

    -- Dates
                                      requisition_date DATE NOT NULL,
                                      required_by_date DATE,
                                      expiry_date DATE,

    -- Status & workflow
                                      status VARCHAR(30) NOT NULL COMMENT 'ENUM: PurchaseRequisitionStatus',

    -- Submission tracking
                                      submitted_at DATETIME(6),
                                      submitted_by_user_id BIGINT,

    -- Approval tracking
                                      approved_by_user_id BIGINT,
                                      approved_at DATETIME(6),
                                      approval_notes TEXT,

    -- Rejection tracking
                                      rejected_by_user_id BIGINT,
                                      rejected_at DATETIME(6),
                                      rejection_reason TEXT,

    -- Cancellation tracking
                                      cancelled_by_user_id BIGINT,
                                      cancelled_at DATETIME(6),
                                      cancellation_reason TEXT,

    -- Fulfillment strategy
                                      default_fulfillment_method VARCHAR(30) COMMENT 'ENUM: FulfillmentMethod',
                                      target_warehouse_id BIGINT COMMENT 'FK to warehouse_location',
                                      preferred_supplier_id BIGINT COMMENT 'FK to Org Service',

    -- Financial estimates
                                      estimated_total DECIMAL(19, 4) DEFAULT 0.0000,
                                      currency VARCHAR(3) DEFAULT 'USD',
                                      budget_available BOOLEAN DEFAULT FALSE,
                                      budget_code VARCHAR(50),

    -- Audit fields
                                      created_by_user_id BIGINT NOT NULL,
                                      updated_by_user_id BIGINT,
                                      created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                      updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                      entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Notes
                                      notes TEXT,

                                      PRIMARY KEY (id),
                                      CONSTRAINT fk_pr_target_warehouse FOREIGN KEY (target_warehouse_id)
                                          REFERENCES warehouse_location(id) ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Purchase requisitions (internal procurement requests)';

CREATE UNIQUE INDEX ux_pr_requisition_number ON purchase_requisition(requisition_number);
CREATE INDEX idx_pr_department_status ON purchase_requisition(department_id, status);
CREATE INDEX idx_pr_requester_status ON purchase_requisition(requested_by_user_id, status);
CREATE INDEX idx_pr_required_date ON purchase_requisition(required_by_date);
CREATE INDEX idx_pr_created_at ON purchase_requisition(created_at);
CREATE INDEX idx_pr_organization ON purchase_requisition(organization_id);
CREATE INDEX idx_pr_status ON purchase_requisition(status);

-- ========================================================================
-- Table: purchase_requisition_line
-- Entity: PurchaseRequisitionLine.java
-- ========================================================================
CREATE TABLE purchase_requisition_line (
                                           id BIGINT NOT NULL AUTO_INCREMENT,
                                           purchase_requisition_id BIGINT NOT NULL,
                                           line_number INT NOT NULL,
                                           product_id BIGINT NOT NULL,
                                           product_description VARCHAR(500),
                                           unit_of_measure VARCHAR(20) NOT NULL COMMENT 'Stored as string, not FK',

    -- Quantity tracking (CRITICAL for fulfillment)
                                           requested_quantity DECIMAL(19, 4) NOT NULL,
                                           approved_quantity DECIMAL(19, 4),
                                           ordered_quantity DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
                                           fulfilled_from_stock_quantity DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
                                           fulfilled_from_transfer_quantity DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
                                           remaining_quantity DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,

    -- Pricing estimates
                                           estimated_unit_price DECIMAL(19, 4),
                                           estimated_total_price DECIMAL(19, 4),

    -- Fulfillment strategy
                                           fulfillment_method VARCHAR(30) COMMENT 'ENUM: FulfillmentMethod',
                                           fulfillment_notes TEXT,

    -- Specifications & requirements
                                           specifications TEXT,
                                           preferred_brand VARCHAR(100),
                                           is_substitute_acceptable BOOLEAN DEFAULT TRUE,

    -- Approval adjustments
                                           quantity_adjustment_reason TEXT,

    -- Audit fields
                                           created_by_user_id BIGINT,
                                           updated_by_user_id BIGINT,
                                           created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                           updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                           entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                           PRIMARY KEY (id),
                                           CONSTRAINT fk_pr_line_pr FOREIGN KEY (purchase_requisition_id)
                                               REFERENCES purchase_requisition(id) ON DELETE CASCADE,
                                           CONSTRAINT fk_pr_line_product FOREIGN KEY (product_id)
                                               REFERENCES product(id) ON DELETE RESTRICT

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='PR line items with granular fulfillment tracking';

CREATE INDEX idx_pr_line_pr_id ON purchase_requisition_line(purchase_requisition_id);
CREATE INDEX idx_pr_line_product ON purchase_requisition_line(product_id);
CREATE INDEX idx_pr_line_fulfillment ON purchase_requisition_line(fulfillment_method);
CREATE INDEX idx_pr_line_created_at ON purchase_requisition_line(created_at);

-- ========================================================================
-- Table: purchase_requisition_amendment
-- Entity: PurchaseRequisitionAmendment.java
-- Purpose: Audit trail for changes to approved PRs
-- ========================================================================
CREATE TABLE purchase_requisition_amendment (
                                                id BIGINT NOT NULL AUTO_INCREMENT,
                                                purchase_requisition_id BIGINT NOT NULL,
                                                amendment_number INT NOT NULL,
                                                amendment_type VARCHAR(50) NOT NULL,
                                                description TEXT NOT NULL,
                                                previous_value TEXT,
                                                new_value TEXT,
                                                reason TEXT,
                                                line_id BIGINT,

    -- Audit fields
                                                created_by_user_id BIGINT NOT NULL,
                                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                                entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                                PRIMARY KEY (id),
                                                CONSTRAINT fk_pr_amendment_pr FOREIGN KEY (purchase_requisition_id)
                                                    REFERENCES purchase_requisition(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit trail for changes to approved PRs (versioning)';

CREATE INDEX idx_pr_amendment_pr_id ON purchase_requisition_amendment(purchase_requisition_id);
CREATE INDEX idx_pr_amendment_created ON purchase_requisition_amendment(created_at);

-- ========================================================================
-- Add PR references to purchase_order table (CONDITIONAL)
-- ========================================================================
SET @dbname = DATABASE();
SET @tablename = 'purchase_order';
SET @columnname = 'purchase_requisition_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT ''Column already exists.'' AS result;',
  'ALTER TABLE purchase_order ADD COLUMN purchase_requisition_id BIGINT COMMENT ''Source PR if auto-created from requisition'';'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Create index conditionally
SET @indexname = 'idx_po_pr_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (INDEX_NAME = @indexname)
  ) > 0,
  'SELECT ''Index already exists.'' AS result;',
  'CREATE INDEX idx_po_pr_id ON purchase_order(purchase_requisition_id);'
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- ========================================================================
-- Add PR line references to purchase_order_line table (CONDITIONAL)
-- ========================================================================
SET @tablename = 'purchase_order_line';
SET @columnname = 'purchase_requisition_line_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT ''Column already exists.'' AS result;',
  'ALTER TABLE purchase_order_line ADD COLUMN purchase_requisition_line_id BIGINT COMMENT ''Source PR line if created from requisition'';'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Create index conditionally
SET @indexname = 'idx_po_line_pr_line_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (INDEX_NAME = @indexname)
  ) > 0,
  'SELECT ''Index already exists.'' AS result;',
  'CREATE INDEX idx_po_line_pr_line_id ON purchase_order_line(purchase_requisition_line_id);'
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- ========================================================================
-- END OF V5 MIGRATION
-- ========================================================================