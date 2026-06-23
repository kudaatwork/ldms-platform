-- Starter package excludes fuel consumption; Growth and Enterprise include it as an optional org toggle.

ALTER TABLE subscription_package
    ADD COLUMN fuel_consumption_available TINYINT(1) NOT NULL DEFAULT 1 AFTER included_tracking_day_credits;

UPDATE subscription_package
SET fuel_consumption_available = 0,
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'STARTER'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET description = 'Small depot programmes (up to ~10 trucks)
Includes 100 Heavy · 150 Standard · 200 Light · 50 tracking-day credits monthly
Pay-as-you-go overage at published tier rates (Light $0.05 · Standard $0.15 · Heavy $0.45)
Dedicated platform infrastructure per organisation
GPS live tracking included — fuel consumption not included',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'STARTER'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET description = 'Mid-size corridor programmes (~10–40 trucks)
Includes 500 Heavy · 400 Standard · 400 Light · 300 tracking-day credits monthly
Optional fuel consumption & telemetry (enable in Settings)
Priority LX operations support
10% bonus on wallet top-ups
Dedicated platform infrastructure per organisation',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'GROWTH'
  AND entity_status <> 'DELETED';

UPDATE subscription_package
SET description = 'Enterprise multi-depot operations
Includes 1,500 Heavy · 1,000 Standard · 1,000 Light · 800 tracking-day credits monthly
Optional fuel consumption & telemetry (enable in Settings)
Dedicated customer success manager
Unlimited email and push notifications included
Dedicated platform infrastructure per organisation',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'ENTERPRISE'
  AND entity_status <> 'DELETED';
