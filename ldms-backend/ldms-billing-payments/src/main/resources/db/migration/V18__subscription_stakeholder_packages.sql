-- Stakeholder-focused subscription tiers, higher Starter pricing, SMS bundles per package.
-- included_standard_credits = monthly SMS / WhatsApp notification quota (MESSAGING tier overage bills wallet).

-- ── STARTER — supplier programmes (no fuel telemetry) ─────────────────────────
UPDATE subscription_package
SET name = 'Supplier Starter',
    monthly_price_cents = 49900,
    included_heavy_credits = 20,
    included_standard_credits = 100,
    included_light_credits = 0,
    included_tracking_day_credits = 20,
    fuel_consumption_available = 0,
    featured = 0,
    sort_order = 1,
    description = 'Built for suppliers moving product through the corridor
Unlimited customer onboarding and bulk CSV product uploads
Purchase orders, dispatch, GRV, and delivery route analytics
100 SMS notifications included monthly — wallet top-up required after quota
20 trip milestone credits and 20 premium GPS days monthly
Fuel consumption telemetry not included — upgrade to Growth or Enterprise',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'STARTER'
  AND entity_status <> 'DELETED';

-- ── GROWTH — suppliers + transport companies ───────────────────────────────────
UPDATE subscription_package
SET name = 'Corridor Growth',
    monthly_price_cents = 89900,
    included_heavy_credits = 50,
    included_standard_credits = 450,
    included_light_credits = 0,
    included_tracking_day_credits = 60,
    fuel_consumption_available = 1,
    featured = 1,
    sort_order = 2,
    description = 'For suppliers and transport companies on shared corridors
Fleet registration, driver roster, and priority truck visibility for suppliers
Contracted fleet, trip execution, and driver performance for transporters
450 SMS notifications included monthly — wallet top-up required after quota
50 trip milestone credits and 60 premium GPS days monthly
Optional fuel telemetry at published per-vehicle-day rates',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'GROWTH'
  AND entity_status <> 'DELETED';

-- ── ENTERPRISE — all stakeholder types ────────────────────────────────────────
UPDATE subscription_package
SET name = 'Enterprise Platform',
    monthly_price_cents = 129900,
    included_heavy_credits = 100,
    included_standard_credits = 1200,
    included_light_credits = 0,
    included_tracking_day_credits = 120,
    fuel_consumption_available = 1,
    featured = 0,
    sort_order = 3,
    description = 'Full multi-sided marketplace — suppliers, transporters, clearing agents, and partners
All corridor modules: inventory, fleet, trips, border clearance, and analytics
1,200 SMS notifications included monthly — wallet top-up required after quota
100 trip milestone credits and 120 premium GPS days monthly
Fuel consumption telemetry, clearing-agent match fees bundled in milestone credits
Dedicated platform infrastructure and priority LX operations support',
    modified_at = NOW(6),
    modified_by = 'SYSTEM'
WHERE code = 'ENTERPRISE'
  AND entity_status <> 'DELETED';
