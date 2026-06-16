-- Compliance documents such as a vehicle registration book may not have an expiry date.
ALTER TABLE fleet_compliance_record
    MODIFY COLUMN expires_at DATETIME(6) NULL;
