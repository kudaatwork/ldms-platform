-- ====================================================================
-- LDMS Test Organization + Contact Person User Seed
-- One fully-verified organisation per classification, all KYC-approved,
-- ready to log in on the Platform Portal.
--
-- Password for ALL contact-person users: Kaydizzy098#@!
--   BCrypt hash: $2b$10$IsHIzodSrEgE5UU.FwnvCumMLrbLnjEr4ZFQU7rkAL.jMDNkN1jKW
--
-- Run via:
--   docker exec ldms-mysql mysql -u developer -pPassword098\(\)\! \
--     < infrastructure/mysql/seed-test-organizations.sql
-- ====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ====================================================================
-- PART 1 — ORGANIZATION MANAGEMENT
-- ====================================================================
USE ldms_organization_management;

-- ── 1. SUPPLIER: Apex Supplies Ltd ───────────────────────────────────
INSERT INTO organization (
    name, email, phone_number,
    organization_classification, organization_type,
    contact_person_first_name, contact_person_last_name,
    contact_person_email, contact_person_phone_number,
    contact_person_position, contact_person_gender,
    contact_person_national_id_number, contact_person_date_of_birth,
    registration_number, tax_number,
    website_url, organization_description,
    number_of_employees, regions_served, business_hours,
    is_verified, created_via_signup, kyc_status,
    duplex_mode, standalone_mode,
    inventory_management_enabled, cross_docking_enabled,
    inventory_data_source, counterparty_engagement_mode,
    fuel_consumption_enabled,
    entity_status, created_at, created_by
) VALUES (
    'Apex Supplies Ltd', 'admin@apexsupplies.co.zw', '+263711110001',
    'SUPPLIER', 'PRIVATE',
    'James', 'Mawere',
    'james.mawere@apexsupplies.co.zw', '+263711110001',
    'Managing Director', 'MALE',
    '63-1234567A10', '1980-03-15',
    'REG/SUP/2023/001', 'TAX/SUP/2023/001',
    'https://apexsupplies.co.zw',
    'Apex Supplies Ltd is a leading supplier of industrial gas and related products throughout Zimbabwe.',
    45, 'Harare, Bulawayo, Mutare', 'Mon–Fri 08:00–17:00',
    TRUE, FALSE, 'APPROVED',
    FALSE, FALSE, TRUE, FALSE,
    'INTERNAL', 'PLATFORM_ORG', FALSE,
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @sup_org_id = LAST_INSERT_ID();

INSERT INTO organization_branch (
    organization_id, branch_name, branch_code,
    is_head_office, is_active, branch_level, is_depot,
    email, phone_number, region, business_hours,
    entity_status, created_at, created_by
) VALUES (
    @sup_org_id, 'Apex Supplies Ltd – Head Office', CONCAT('HO-', @sup_org_id),
    TRUE, TRUE, 'BRANCH', FALSE,
    'admin@apexsupplies.co.zw', '+263711110001', 'Harare', 'Mon–Fri 08:00–17:00',
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @sup_branch_id = LAST_INSERT_ID();

-- ── 2. CUSTOMER: Harare Distributors (Pvt) Ltd ───────────────────────
INSERT INTO organization (
    name, email, phone_number,
    organization_classification, organization_type,
    contact_person_first_name, contact_person_last_name,
    contact_person_email, contact_person_phone_number,
    contact_person_position, contact_person_gender,
    contact_person_national_id_number, contact_person_date_of_birth,
    registration_number, tax_number,
    website_url, organization_description,
    number_of_employees, regions_served, business_hours,
    is_verified, created_via_signup, kyc_status,
    duplex_mode, standalone_mode,
    inventory_management_enabled, cross_docking_enabled,
    inventory_data_source, counterparty_engagement_mode,
    fuel_consumption_enabled,
    entity_status, created_at, created_by
) VALUES (
    'Harare Distributors (Pvt) Ltd', 'admin@haredist.co.zw', '+263722220002',
    'CUSTOMER', 'PRIVATE',
    'Grace', 'Ndoro',
    'grace.ndoro@haredist.co.zw', '+263722220002',
    'Chief Executive Officer', 'FEMALE',
    '63-2345678B20', '1985-07-22',
    'REG/CUST/2023/001', 'TAX/CUST/2023/001',
    'https://haredist.co.zw',
    'Harare Distributors is a major consumer goods distributor serving businesses across Zimbabwe.',
    120, 'Harare, Chinhoyi, Marondera', 'Mon–Sat 07:00–18:00',
    TRUE, FALSE, 'APPROVED',
    FALSE, FALSE, TRUE, FALSE,
    'INTERNAL', 'PLATFORM_ORG', FALSE,
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @cust_org_id = LAST_INSERT_ID();

INSERT INTO organization_branch (
    organization_id, branch_name, branch_code,
    is_head_office, is_active, branch_level, is_depot,
    email, phone_number, region, business_hours,
    entity_status, created_at, created_by
) VALUES (
    @cust_org_id, 'Harare Distributors – Head Office', CONCAT('HO-', @cust_org_id),
    TRUE, TRUE, 'BRANCH', FALSE,
    'admin@haredist.co.zw', '+263722220002', 'Harare', 'Mon–Sat 07:00–18:00',
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @cust_branch_id = LAST_INSERT_ID();

-- ── 3. TRANSPORT_COMPANY: SwiftHaul Logistics (Pvt) Ltd ──────────────
INSERT INTO organization (
    name, email, phone_number,
    organization_classification, organization_type,
    contact_person_first_name, contact_person_last_name,
    contact_person_email, contact_person_phone_number,
    contact_person_position, contact_person_gender,
    contact_person_national_id_number, contact_person_date_of_birth,
    registration_number, tax_number,
    website_url, organization_description,
    number_of_employees, regions_served, business_hours,
    is_verified, created_via_signup, kyc_status,
    duplex_mode, standalone_mode,
    inventory_management_enabled, cross_docking_enabled,
    inventory_data_source, counterparty_engagement_mode,
    fuel_consumption_enabled,
    entity_status, created_at, created_by
) VALUES (
    'SwiftHaul Logistics (Pvt) Ltd', 'admin@swifthaul.co.zw', '+263733330003',
    'TRANSPORT_COMPANY', 'PRIVATE',
    'Brian', 'Dzoro',
    'brian.dzoro@swifthaul.co.zw', '+263733330003',
    'Operations Director', 'MALE',
    '63-3456789C30', '1978-11-10',
    'REG/TRANS/2023/001', 'TAX/TRANS/2023/001',
    'https://swifthaul.co.zw',
    'SwiftHaul Logistics provides reliable road freight and logistics solutions across Southern Africa.',
    200, 'Harare, Bulawayo, Beitbridge, Mutare', 'Mon–Sun 00:00–24:00',
    TRUE, FALSE, 'APPROVED',
    FALSE, FALSE, FALSE, FALSE,
    'INTERNAL', 'PLATFORM_ORG', TRUE,
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @trans_org_id = LAST_INSERT_ID();

INSERT INTO organization_branch (
    organization_id, branch_name, branch_code,
    is_head_office, is_active, branch_level, is_depot,
    email, phone_number, region, business_hours,
    entity_status, created_at, created_by
) VALUES (
    @trans_org_id, 'SwiftHaul Logistics – Head Office', CONCAT('HO-', @trans_org_id),
    TRUE, TRUE, 'BRANCH', FALSE,
    'admin@swifthaul.co.zw', '+263733330003', 'Harare', 'Mon–Sun 00:00–24:00',
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @trans_branch_id = LAST_INSERT_ID();

-- ── 4. CLEARING_AGENT: Clearpath Agents (Pvt) Ltd ────────────────────
INSERT INTO organization (
    name, email, phone_number,
    organization_classification, organization_type,
    contact_person_first_name, contact_person_last_name,
    contact_person_email, contact_person_phone_number,
    contact_person_position, contact_person_gender,
    contact_person_national_id_number, contact_person_date_of_birth,
    registration_number, tax_number,
    website_url, organization_description,
    number_of_employees, regions_served, business_hours,
    is_verified, created_via_signup, kyc_status,
    duplex_mode, standalone_mode,
    inventory_management_enabled, cross_docking_enabled,
    inventory_data_source, counterparty_engagement_mode,
    fuel_consumption_enabled,
    entity_status, created_at, created_by
) VALUES (
    'Clearpath Agents (Pvt) Ltd', 'admin@clearpath.co.zw', '+263744440004',
    'CLEARING_AGENT', 'PRIVATE',
    'Chipo', 'Mutasa',
    'chipo.mutasa@clearpath.co.zw', '+263744440004',
    'Managing Director', 'FEMALE',
    '63-4567890D40', '1982-05-18',
    'REG/CLEAR/2023/001', 'TAX/CLEAR/2023/001',
    'https://clearpath.co.zw',
    'Clearpath Agents specializes in customs clearance and regulatory compliance for cross-border cargo.',
    55, 'Harare, Beitbridge, Chirundu, Forbes Border Post', 'Mon–Fri 07:30–17:30',
    TRUE, FALSE, 'APPROVED',
    FALSE, FALSE, FALSE, FALSE,
    'INTERNAL', 'PLATFORM_ORG', FALSE,
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @clear_org_id = LAST_INSERT_ID();

INSERT INTO organization_branch (
    organization_id, branch_name, branch_code,
    is_head_office, is_active, branch_level, is_depot,
    email, phone_number, region, business_hours,
    entity_status, created_at, created_by
) VALUES (
    @clear_org_id, 'Clearpath Agents – Head Office', CONCAT('HO-', @clear_org_id),
    TRUE, TRUE, 'BRANCH', FALSE,
    'admin@clearpath.co.zw', '+263744440004', 'Harare', 'Mon–Fri 07:30–17:30',
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @clear_branch_id = LAST_INSERT_ID();

-- ── 5. SERVICE_STATION: Highway Fuel & Services ───────────────────────
INSERT INTO organization (
    name, email, phone_number,
    organization_classification, organization_type,
    contact_person_first_name, contact_person_last_name,
    contact_person_email, contact_person_phone_number,
    contact_person_position, contact_person_gender,
    contact_person_national_id_number, contact_person_date_of_birth,
    registration_number, tax_number,
    website_url, organization_description,
    number_of_employees, regions_served, business_hours,
    is_verified, created_via_signup, kyc_status,
    duplex_mode, standalone_mode,
    inventory_management_enabled, cross_docking_enabled,
    inventory_data_source, counterparty_engagement_mode,
    fuel_consumption_enabled,
    entity_status, created_at, created_by
) VALUES (
    'Highway Fuel & Services', 'admin@highwayfuel.co.zw', '+263755550005',
    'SERVICE_STATION', 'PRIVATE',
    'Takudzwa', 'Zinyama',
    'takudzwa.zinyama@highwayfuel.co.zw', '+263755550005',
    'Station Manager', 'MALE',
    '63-5678901E50', '1990-02-28',
    'REG/SVC/2023/001', 'TAX/SVC/2023/001',
    'https://highwayfuel.co.zw',
    'Highway Fuel & Services operates full-service fuel stations along major transport corridors.',
    80, 'Harare, Gweru, Bulawayo, Masvingo', 'Mon–Sun 06:00–22:00',
    TRUE, FALSE, 'APPROVED',
    FALSE, FALSE, TRUE, FALSE,
    'INTERNAL', 'PLATFORM_ORG', TRUE,
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @svc_org_id = LAST_INSERT_ID();

INSERT INTO organization_branch (
    organization_id, branch_name, branch_code,
    is_head_office, is_active, branch_level, is_depot,
    email, phone_number, region, business_hours,
    entity_status, created_at, created_by
) VALUES (
    @svc_org_id, 'Highway Fuel – Head Office', CONCAT('HO-', @svc_org_id),
    TRUE, TRUE, 'BRANCH', FALSE,
    'admin@highwayfuel.co.zw', '+263755550005', 'Harare', 'Mon–Sun 06:00–22:00',
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @svc_branch_id = LAST_INSERT_ID();

-- ── 6. ROADSIDE_SUPPORT_SERVICE: RoadAid Zimbabwe (Pvt) Ltd ──────────
INSERT INTO organization (
    name, email, phone_number,
    organization_classification, organization_type,
    contact_person_first_name, contact_person_last_name,
    contact_person_email, contact_person_phone_number,
    contact_person_position, contact_person_gender,
    contact_person_national_id_number, contact_person_date_of_birth,
    registration_number, tax_number,
    website_url, organization_description,
    number_of_employees, regions_served, business_hours,
    is_verified, created_via_signup, kyc_status,
    duplex_mode, standalone_mode,
    inventory_management_enabled, cross_docking_enabled,
    inventory_data_source, counterparty_engagement_mode,
    fuel_consumption_enabled,
    entity_status, created_at, created_by
) VALUES (
    'RoadAid Zimbabwe (Pvt) Ltd', 'admin@roadaid.co.zw', '+263766660006',
    'ROADSIDE_SUPPORT_SERVICE', 'PRIVATE',
    'Michael', 'Goremusandu',
    'michael.goremusandu@roadaid.co.zw', '+263766660006',
    'Chief Operations Officer', 'MALE',
    '63-6789012F60', '1975-09-05',
    'REG/ROAD/2023/001', 'TAX/ROAD/2023/001',
    'https://roadaid.co.zw',
    'RoadAid Zimbabwe provides 24/7 roadside breakdown assistance and emergency support across all major routes.',
    150, 'Harare, Bulawayo, Masvingo, Gweru, Mutare', 'Mon–Sun 00:00–24:00',
    TRUE, FALSE, 'APPROVED',
    FALSE, FALSE, FALSE, FALSE,
    'INTERNAL', 'PLATFORM_ORG', FALSE,
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @road_org_id = LAST_INSERT_ID();

INSERT INTO organization_branch (
    organization_id, branch_name, branch_code,
    is_head_office, is_active, branch_level, is_depot,
    email, phone_number, region, business_hours,
    entity_status, created_at, created_by
) VALUES (
    @road_org_id, 'RoadAid Zimbabwe – Head Office', CONCAT('HO-', @road_org_id),
    TRUE, TRUE, 'BRANCH', FALSE,
    'admin@roadaid.co.zw', '+263766660006', 'Harare', 'Mon–Sun 00:00–24:00',
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @road_branch_id = LAST_INSERT_ID();

-- ── 7. GOVERNMENT_AGENCY: Zimbabwe Revenue Authority ─────────────────
INSERT INTO organization (
    name, email, phone_number,
    organization_classification, organization_type,
    contact_person_first_name, contact_person_last_name,
    contact_person_email, contact_person_phone_number,
    contact_person_position, contact_person_gender,
    contact_person_national_id_number, contact_person_date_of_birth,
    registration_number, tax_number,
    website_url, organization_description,
    number_of_employees, regions_served, business_hours,
    is_verified, created_via_signup, kyc_status,
    duplex_mode, standalone_mode,
    inventory_management_enabled, cross_docking_enabled,
    inventory_data_source, counterparty_engagement_mode,
    fuel_consumption_enabled,
    entity_status, created_at, created_by
) VALUES (
    'Zimbabwe Revenue Authority', 'ldms@zimra.co.zw', '+263777770007',
    'GOVERNMENT_AGENCY', 'GOVERNMENT',
    'Patricia', 'Musariri',
    'patricia.musariri@zimra.co.zw', '+263777770007',
    'Commissioner General', 'FEMALE',
    '63-7890123G70', '1970-12-01',
    'GOV/ZIMRA/001', 'GOV/TAX/ZIMRA/001',
    'https://zimra.co.zw',
    'Zimbabwe Revenue Authority is the principal government body responsible for revenue collection and customs regulation.',
    3000, 'All Provinces', 'Mon–Fri 08:00–16:30',
    TRUE, FALSE, 'APPROVED',
    FALSE, FALSE, FALSE, FALSE,
    'INTERNAL', 'PLATFORM_ORG', FALSE,
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @gov_org_id = LAST_INSERT_ID();

INSERT INTO organization_branch (
    organization_id, branch_name, branch_code,
    is_head_office, is_active, branch_level, is_depot,
    email, phone_number, region, business_hours,
    entity_status, created_at, created_by
) VALUES (
    @gov_org_id, 'ZIMRA – Head Office', CONCAT('HO-', @gov_org_id),
    TRUE, TRUE, 'BRANCH', FALSE,
    'ldms@zimra.co.zw', '+263777770007', 'Harare', 'Mon–Fri 08:00–16:30',
    'ACTIVE', NOW(6), 'SYSTEM'
);
SET @gov_branch_id = LAST_INSERT_ID();

-- ====================================================================
-- PART 2 — USER MANAGEMENT
-- ====================================================================
USE ldms_user_management;

-- Classification default Administrator group IDs
SET @grp_supplier  = (SELECT id FROM user_group WHERE organization_id IS NULL AND LOWER(name) = 'administrator' AND organization_classification = 'SUPPLIER'                AND entity_status <> 'DELETED' LIMIT 1);
SET @grp_customer  = (SELECT id FROM user_group WHERE organization_id IS NULL AND LOWER(name) = 'administrator' AND organization_classification = 'CUSTOMER'                AND entity_status <> 'DELETED' LIMIT 1);
SET @grp_transport = (SELECT id FROM user_group WHERE organization_id IS NULL AND LOWER(name) = 'administrator' AND organization_classification = 'TRANSPORT_COMPANY'       AND entity_status <> 'DELETED' LIMIT 1);
SET @grp_clearing  = (SELECT id FROM user_group WHERE organization_id IS NULL AND LOWER(name) = 'administrator' AND organization_classification = 'CLEARING_AGENT'          AND entity_status <> 'DELETED' LIMIT 1);
SET @grp_svc       = (SELECT id FROM user_group WHERE organization_id IS NULL AND LOWER(name) = 'administrator' AND organization_classification = 'SERVICE_STATION'         AND entity_status <> 'DELETED' LIMIT 1);
SET @grp_road      = (SELECT id FROM user_group WHERE organization_id IS NULL AND LOWER(name) = 'administrator' AND organization_classification = 'ROADSIDE_SUPPORT_SERVICE' AND entity_status <> 'DELETED' LIMIT 1);
SET @grp_gov       = (SELECT id FROM user_group WHERE organization_id IS NULL AND LOWER(name) = 'administrator' AND organization_classification = 'GOVERNMENT_AGENCY'       AND entity_status <> 'DELETED' LIMIT 1);

-- User type: ORGANIZATION_CONTACT (id=5)
SET @org_contact_type = (SELECT id FROM user_type WHERE user_type_name = 'ORGANIZATION_CONTACT' AND entity_status = 'ACTIVE' LIMIT 1);

-- BCrypt hash of 'Kaydizzy098#@!'
SET @pwd = '$2b$10$IsHIzodSrEgE5UU.FwnvCumMLrbLnjEr4ZFQU7rkAL.jMDNkN1jKW';

-- ── SUPPLIER user — James Mawere ──────────────────────────────────────
INSERT INTO `user` (
    organization_id, branch_id,
    organization_kyc_approver, operational_issue_handler,
    procurement_approver, shipment_fleet_allocator, billing_approver,
    username, email, first_name, last_name,
    gender, phone_number, national_id_number,
    email_verified, phone_verified, must_change_credentials,
    entity_status, created_at, user_group_id, user_type_id
) VALUES (
    @sup_org_id, @sup_branch_id,
    0, 0, 0, 0, 0,
    'james.mawere', 'james.mawere@apexsupplies.co.zw', 'James', 'Mawere',
    'MALE', '+263711110001', '63-1234567A10',
    1, 0, 0,
    'ACTIVE', NOW(6), @grp_supplier, @org_contact_type
);
SET @sup_user_id = LAST_INSERT_ID();

INSERT INTO user_account (phone_number, account_number, is_account_locked, created_at, entity_status, user_id)
VALUES ('+263711110001', CONCAT('ACC-', LPAD(@sup_user_id, 6, '0')), 0, NOW(6), 'ACTIVE', @sup_user_id);

INSERT INTO user_password (password, expiry_date, is_password_expired, created_at, entity_status, user_id)
VALUES (@pwd, DATE_ADD(NOW(6), INTERVAL 1 YEAR), 0, NOW(6), 'ACTIVE', @sup_user_id);

INSERT INTO user_security (is_two_factor_enabled, created_at, entity_status, user_id)
VALUES (0, NOW(6), 'ACTIVE', @sup_user_id);

INSERT INTO user_preferences (preferred_language, timezone, created_at, entity_status, user_id)
VALUES ('en', 'Africa/Harare', NOW(6), 'ACTIVE', @sup_user_id);

UPDATE ldms_organization_management.organization
SET contact_person_user_id = @sup_user_id, modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE id = @sup_org_id;

-- ── CUSTOMER user — Grace Ndoro ───────────────────────────────────────
INSERT INTO `user` (
    organization_id, branch_id,
    organization_kyc_approver, operational_issue_handler,
    procurement_approver, shipment_fleet_allocator, billing_approver,
    username, email, first_name, last_name,
    gender, phone_number, national_id_number,
    email_verified, phone_verified, must_change_credentials,
    entity_status, created_at, user_group_id, user_type_id
) VALUES (
    @cust_org_id, @cust_branch_id,
    0, 0, 0, 0, 0,
    'grace.ndoro', 'grace.ndoro@haredist.co.zw', 'Grace', 'Ndoro',
    'FEMALE', '+263722220002', '63-2345678B20',
    1, 0, 0,
    'ACTIVE', NOW(6), @grp_customer, @org_contact_type
);
SET @cust_user_id = LAST_INSERT_ID();

INSERT INTO user_account (phone_number, account_number, is_account_locked, created_at, entity_status, user_id)
VALUES ('+263722220002', CONCAT('ACC-', LPAD(@cust_user_id, 6, '0')), 0, NOW(6), 'ACTIVE', @cust_user_id);

INSERT INTO user_password (password, expiry_date, is_password_expired, created_at, entity_status, user_id)
VALUES (@pwd, DATE_ADD(NOW(6), INTERVAL 1 YEAR), 0, NOW(6), 'ACTIVE', @cust_user_id);

INSERT INTO user_security (is_two_factor_enabled, created_at, entity_status, user_id)
VALUES (0, NOW(6), 'ACTIVE', @cust_user_id);

INSERT INTO user_preferences (preferred_language, timezone, created_at, entity_status, user_id)
VALUES ('en', 'Africa/Harare', NOW(6), 'ACTIVE', @cust_user_id);

UPDATE ldms_organization_management.organization
SET contact_person_user_id = @cust_user_id, modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE id = @cust_org_id;

-- ── TRANSPORT_COMPANY user — Brian Dzoro ─────────────────────────────
INSERT INTO `user` (
    organization_id, branch_id,
    organization_kyc_approver, operational_issue_handler,
    procurement_approver, shipment_fleet_allocator, billing_approver,
    username, email, first_name, last_name,
    gender, phone_number, national_id_number,
    email_verified, phone_verified, must_change_credentials,
    entity_status, created_at, user_group_id, user_type_id
) VALUES (
    @trans_org_id, @trans_branch_id,
    0, 0, 0, 0, 0,
    'brian.dzoro', 'brian.dzoro@swifthaul.co.zw', 'Brian', 'Dzoro',
    'MALE', '+263733330003', '63-3456789C30',
    1, 0, 0,
    'ACTIVE', NOW(6), @grp_transport, @org_contact_type
);
SET @trans_user_id = LAST_INSERT_ID();

INSERT INTO user_account (phone_number, account_number, is_account_locked, created_at, entity_status, user_id)
VALUES ('+263733330003', CONCAT('ACC-', LPAD(@trans_user_id, 6, '0')), 0, NOW(6), 'ACTIVE', @trans_user_id);

INSERT INTO user_password (password, expiry_date, is_password_expired, created_at, entity_status, user_id)
VALUES (@pwd, DATE_ADD(NOW(6), INTERVAL 1 YEAR), 0, NOW(6), 'ACTIVE', @trans_user_id);

INSERT INTO user_security (is_two_factor_enabled, created_at, entity_status, user_id)
VALUES (0, NOW(6), 'ACTIVE', @trans_user_id);

INSERT INTO user_preferences (preferred_language, timezone, created_at, entity_status, user_id)
VALUES ('en', 'Africa/Harare', NOW(6), 'ACTIVE', @trans_user_id);

UPDATE ldms_organization_management.organization
SET contact_person_user_id = @trans_user_id, modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE id = @trans_org_id;

-- ── CLEARING_AGENT user — Chipo Mutasa ────────────────────────────────
INSERT INTO `user` (
    organization_id, branch_id,
    organization_kyc_approver, operational_issue_handler,
    procurement_approver, shipment_fleet_allocator, billing_approver,
    username, email, first_name, last_name,
    gender, phone_number, national_id_number,
    email_verified, phone_verified, must_change_credentials,
    entity_status, created_at, user_group_id, user_type_id
) VALUES (
    @clear_org_id, @clear_branch_id,
    0, 0, 0, 0, 0,
    'chipo.mutasa', 'chipo.mutasa@clearpath.co.zw', 'Chipo', 'Mutasa',
    'FEMALE', '+263744440004', '63-4567890D40',
    1, 0, 0,
    'ACTIVE', NOW(6), @grp_clearing, @org_contact_type
);
SET @clear_user_id = LAST_INSERT_ID();

INSERT INTO user_account (phone_number, account_number, is_account_locked, created_at, entity_status, user_id)
VALUES ('+263744440004', CONCAT('ACC-', LPAD(@clear_user_id, 6, '0')), 0, NOW(6), 'ACTIVE', @clear_user_id);

INSERT INTO user_password (password, expiry_date, is_password_expired, created_at, entity_status, user_id)
VALUES (@pwd, DATE_ADD(NOW(6), INTERVAL 1 YEAR), 0, NOW(6), 'ACTIVE', @clear_user_id);

INSERT INTO user_security (is_two_factor_enabled, created_at, entity_status, user_id)
VALUES (0, NOW(6), 'ACTIVE', @clear_user_id);

INSERT INTO user_preferences (preferred_language, timezone, created_at, entity_status, user_id)
VALUES ('en', 'Africa/Harare', NOW(6), 'ACTIVE', @clear_user_id);

UPDATE ldms_organization_management.organization
SET contact_person_user_id = @clear_user_id, modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE id = @clear_org_id;

-- ── SERVICE_STATION user — Takudzwa Zinyama ───────────────────────────
INSERT INTO `user` (
    organization_id, branch_id,
    organization_kyc_approver, operational_issue_handler,
    procurement_approver, shipment_fleet_allocator, billing_approver,
    username, email, first_name, last_name,
    gender, phone_number, national_id_number,
    email_verified, phone_verified, must_change_credentials,
    entity_status, created_at, user_group_id, user_type_id
) VALUES (
    @svc_org_id, @svc_branch_id,
    0, 0, 0, 0, 0,
    'takudzwa.zinyama', 'takudzwa.zinyama@highwayfuel.co.zw', 'Takudzwa', 'Zinyama',
    'MALE', '+263755550005', '63-5678901E50',
    1, 0, 0,
    'ACTIVE', NOW(6), @grp_svc, @org_contact_type
);
SET @svc_user_id = LAST_INSERT_ID();

INSERT INTO user_account (phone_number, account_number, is_account_locked, created_at, entity_status, user_id)
VALUES ('+263755550005', CONCAT('ACC-', LPAD(@svc_user_id, 6, '0')), 0, NOW(6), 'ACTIVE', @svc_user_id);

INSERT INTO user_password (password, expiry_date, is_password_expired, created_at, entity_status, user_id)
VALUES (@pwd, DATE_ADD(NOW(6), INTERVAL 1 YEAR), 0, NOW(6), 'ACTIVE', @svc_user_id);

INSERT INTO user_security (is_two_factor_enabled, created_at, entity_status, user_id)
VALUES (0, NOW(6), 'ACTIVE', @svc_user_id);

INSERT INTO user_preferences (preferred_language, timezone, created_at, entity_status, user_id)
VALUES ('en', 'Africa/Harare', NOW(6), 'ACTIVE', @svc_user_id);

UPDATE ldms_organization_management.organization
SET contact_person_user_id = @svc_user_id, modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE id = @svc_org_id;

-- ── ROADSIDE_SUPPORT_SERVICE user — Michael Goremusandu ───────────────
INSERT INTO `user` (
    organization_id, branch_id,
    organization_kyc_approver, operational_issue_handler,
    procurement_approver, shipment_fleet_allocator, billing_approver,
    username, email, first_name, last_name,
    gender, phone_number, national_id_number,
    email_verified, phone_verified, must_change_credentials,
    entity_status, created_at, user_group_id, user_type_id
) VALUES (
    @road_org_id, @road_branch_id,
    0, 0, 0, 0, 0,
    'michael.goremusandu', 'michael.goremusandu@roadaid.co.zw', 'Michael', 'Goremusandu',
    'MALE', '+263766660006', '63-6789012F60',
    1, 0, 0,
    'ACTIVE', NOW(6), @grp_road, @org_contact_type
);
SET @road_user_id = LAST_INSERT_ID();

INSERT INTO user_account (phone_number, account_number, is_account_locked, created_at, entity_status, user_id)
VALUES ('+263766660006', CONCAT('ACC-', LPAD(@road_user_id, 6, '0')), 0, NOW(6), 'ACTIVE', @road_user_id);

INSERT INTO user_password (password, expiry_date, is_password_expired, created_at, entity_status, user_id)
VALUES (@pwd, DATE_ADD(NOW(6), INTERVAL 1 YEAR), 0, NOW(6), 'ACTIVE', @road_user_id);

INSERT INTO user_security (is_two_factor_enabled, created_at, entity_status, user_id)
VALUES (0, NOW(6), 'ACTIVE', @road_user_id);

INSERT INTO user_preferences (preferred_language, timezone, created_at, entity_status, user_id)
VALUES ('en', 'Africa/Harare', NOW(6), 'ACTIVE', @road_user_id);

UPDATE ldms_organization_management.organization
SET contact_person_user_id = @road_user_id, modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE id = @road_org_id;

-- ── GOVERNMENT_AGENCY user — Patricia Musariri ────────────────────────
INSERT INTO `user` (
    organization_id, branch_id,
    organization_kyc_approver, operational_issue_handler,
    procurement_approver, shipment_fleet_allocator, billing_approver,
    username, email, first_name, last_name,
    gender, phone_number, national_id_number,
    email_verified, phone_verified, must_change_credentials,
    entity_status, created_at, user_group_id, user_type_id
) VALUES (
    @gov_org_id, @gov_branch_id,
    0, 0, 0, 0, 0,
    'patricia.musariri', 'patricia.musariri@zimra.co.zw', 'Patricia', 'Musariri',
    'FEMALE', '+263777770007', '63-7890123G70',
    1, 0, 0,
    'ACTIVE', NOW(6), @grp_gov, @org_contact_type
);
SET @gov_user_id = LAST_INSERT_ID();

INSERT INTO user_account (phone_number, account_number, is_account_locked, created_at, entity_status, user_id)
VALUES ('+263777770007', CONCAT('ACC-', LPAD(@gov_user_id, 6, '0')), 0, NOW(6), 'ACTIVE', @gov_user_id);

INSERT INTO user_password (password, expiry_date, is_password_expired, created_at, entity_status, user_id)
VALUES (@pwd, DATE_ADD(NOW(6), INTERVAL 1 YEAR), 0, NOW(6), 'ACTIVE', @gov_user_id);

INSERT INTO user_security (is_two_factor_enabled, created_at, entity_status, user_id)
VALUES (0, NOW(6), 'ACTIVE', @gov_user_id);

INSERT INTO user_preferences (preferred_language, timezone, created_at, entity_status, user_id)
VALUES ('en', 'Africa/Harare', NOW(6), 'ACTIVE', @gov_user_id);

UPDATE ldms_organization_management.organization
SET contact_person_user_id = @gov_user_id, modified_at = NOW(6), modified_by = 'SYSTEM'
WHERE id = @gov_org_id;

SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================================
-- VERIFICATION SUMMARY
-- ====================================================================
SELECT 'Organizations created:' AS info;
SELECT id, name, organization_classification, kyc_status, is_verified, contact_person_user_id
FROM ldms_organization_management.organization
WHERE id IN (@sup_org_id, @cust_org_id, @trans_org_id, @clear_org_id, @svc_org_id, @road_org_id, @gov_org_id)
ORDER BY id;

SELECT 'Users created:' AS info;
USE ldms_user_management;
SELECT u.id, u.username, u.email, u.organization_id, u.entity_status
FROM `user` u
WHERE u.id IN (@sup_user_id, @cust_user_id, @trans_user_id, @clear_user_id, @svc_user_id, @road_user_id, @gov_user_id)
ORDER BY u.id;
