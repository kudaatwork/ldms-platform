-- Help articles (FAQ) and user support tickets for Help & Support portal feature.

CREATE TABLE help_article (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    slug            VARCHAR(120)    NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    summary         VARCHAR(500)    NOT NULL,
    body_markdown   TEXT            NOT NULL,
    category        VARCHAR(50)     NOT NULL,
    sort_order      INT             NOT NULL DEFAULT 0,
    entity_status   VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)     NOT NULL,
    created_by      VARCHAR(150)    NOT NULL,
    modified_at     DATETIME(6)     NULL,
    modified_by     VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_help_article_slug (slug),
    KEY idx_help_article_category (category),
    KEY idx_help_article_status (entity_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE support_ticket (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    ticket_number   VARCHAR(32)     NOT NULL,
    subject         VARCHAR(200)    NOT NULL,
    description     TEXT            NOT NULL,
    category        VARCHAR(50)     NOT NULL,
    priority        VARCHAR(50)     NOT NULL DEFAULT 'NORMAL',
    status          VARCHAR(50)     NOT NULL DEFAULT 'OPEN',
    requester_username VARCHAR(150) NOT NULL,
    requester_email VARCHAR(254)    NOT NULL,
    organization_id BIGINT          NULL,
    organization_name VARCHAR(200)  NULL,
    entity_status   VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)     NOT NULL,
    created_by      VARCHAR(150)    NOT NULL,
    modified_at     DATETIME(6)     NULL,
    modified_by     VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_support_ticket_number (ticket_number),
    KEY idx_support_ticket_requester (requester_username),
    KEY idx_support_ticket_status (status),
    KEY idx_support_ticket_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO help_article (slug, title, summary, body_markdown, category, sort_order, entity_status, created_at, created_by)
VALUES
(
    'getting-started',
    'Getting started on LX Platform',
    'Sign in, complete onboarding, and navigate your workspace.',
    '## Welcome\n\nAfter KYC approval and email verification, sign in with your organisation credentials.\n\n### Workspace navigation\n\nUse the sidebar to reach **Dashboard**, operational modules for your classification, and **Account & support** for settings and help.\n\n### Need access?\n\nContact your organisation administrator or submit a support ticket from this page.',
    'GETTING_STARTED',
    10,
    'ACTIVE',
    CURRENT_TIMESTAMP(6),
    'system'
),
(
    'orders-and-shipments',
    'Orders, shipments, and tracking',
    'How purchase orders flow into trips and live status updates.',
    '## Order lifecycle\n\nPurchase orders move through preparation, dispatch, in-transit, and delivery states.\n\n### Tracking\n\nUse **Track shipments** or the dashboard operations board to filter by status.\n\n### Issues\n\nIf a shipment status looks stale for more than 30 minutes, check **Platform status** on this page or open a ticket with the shipment reference.',
    'OPERATIONS',
    20,
    'ACTIVE',
    CURRENT_TIMESTAMP(6),
    'system'
),
(
    'account-and-security',
    'Account, passwords, and security',
    'Manage your profile, reset passwords, and keep your account secure.',
    '## Profile\n\nOpen **My account** to review your details.\n\n### Password reset\n\nUse **Forgot password** on the sign-in page. Reset links expire after a short window for security.\n\n### Suspicious activity\n\nIf you notice unexpected sign-in activity, change your password immediately and raise a **Security** support ticket.',
    'ACCOUNT',
    30,
    'ACTIVE',
    CURRENT_TIMESTAMP(6),
    'system'
),
(
    'billing-and-documents',
    'Billing, invoices, and documents',
    'Find invoices, upload compliance documents, and resolve billing questions.',
    '## Documents\n\nCompliance and KYC documents are reviewed during onboarding. Upload replacements from **Documents** when requested.\n\n### Invoices\n\nView invoices under **Billing** or **Invoices** depending on your portal role.\n\n### Disputes\n\nInclude invoice or PO numbers when submitting a **Billing** ticket so finance can respond faster.',
    'BILLING',
    40,
    'ACTIVE',
    CURRENT_TIMESTAMP(6),
    'system'
),
(
    'platform-status',
    'Understanding platform status',
    'What operational, degraded, and outage mean for your work.',
    '## Status levels\n\n- **Operational** — all core services are healthy.\n- **Degraded** — some non-critical services may be slow; core workflows usually continue.\n- **Outage** — a critical service is down; expect delays until restored.\n\nCheck the **Platform status** tab on this page for a live snapshot.',
    'PLATFORM',
    50,
    'ACTIVE',
    CURRENT_TIMESTAMP(6),
    'system'
);
