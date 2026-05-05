-- Spring Batch (MySQL) expects BATCH_JOB_SEQ for job instance ID allocation.
-- Keep previous migrations immutable; add missing sequence table in this follow-up migration.

CREATE TABLE IF NOT EXISTS ldms_audit_trail_dev_db.BATCH_JOB_SEQ
(
    ID         BIGINT  NOT NULL,
    UNIQUE_KEY CHAR(1) NOT NULL,
    CONSTRAINT UNIQUE_KEY_UN_BATCH_JOB_SEQ UNIQUE (UNIQUE_KEY)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO ldms_audit_trail_dev_db.BATCH_JOB_SEQ (ID, UNIQUE_KEY)
SELECT *
FROM (SELECT 0 AS ID, '0' AS UNIQUE_KEY) AS tmp
WHERE NOT EXISTS (SELECT * FROM ldms_audit_trail_dev_db.BATCH_JOB_SEQ);
