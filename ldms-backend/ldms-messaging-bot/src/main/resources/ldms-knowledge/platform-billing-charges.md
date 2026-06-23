# Platform billing — prepaid wallet and action charges

LDMS uses a **prepaid platform wallet** for organisations on the platform portal. **Milestone fees** apply at high-value corridor moments; routine admin is **included** in subscription plans.

Full strategy: `docs/PROJECT-LX-PLATFORM-PRICING-GUIDE.md` (repository root).

## How charges work

- Organisations **top up** their wallet (bank transfer or approved deposit) or subscribe to a **stakeholder plan** (Supplier Pro, Fleet Manager Premium, Clearing Agent Portal).
- **Milestone actions** (trip booking, dispatch, GRV, clearing match) deduct from the wallet on prepaid mode.
- **Document uploads, approvals, and status updates** are free — never blocked for wallet balance.
- **Premium GPS** and **fuel telemetry** bill per trip-day / vehicle-day (not per ping).
- Drivers and end-customers are never charged platform fees.

## Milestone fees (pay-as-you-go)

| Action | Typical fee |
|--------|-------------|
| Trip booking (`TRIP_CREATE`) | $10 |
| Trip completed (`TRIP_COMPLETE`) | $7.50 |
| Shipment dispatch | $8 |
| GRV / proof of delivery | $5 |
| Invoice after delivery | $5 |
| Clearing agent match | $25 |
| Road fund transfer | $3 (+ % planned) |

## Premium add-ons

| Add-on | Rate |
|--------|------|
| Premium GPS day | $1.50 / trip-day |
| Fuel telemetry day | $1.75 / vehicle-day |
| SMS (after plan quota) | ~$0.10 each |

## Subscription personas

| Plan | Audience | From |
|------|----------|------|
| Supplier Pro | Suppliers | $349/mo |
| Fleet Manager Premium | Transport companies | $649/mo |
| Clearing Agent Portal | Clearing agents | $549/mo |

## Where to configure (admins)

| Task | Location |
|------|----------|
| Set per-action prices | LX Admin → Settings → Platform billing → Action charges |
| Subscription packages | LX Admin → Settings → Platform billing → Packages |
| Revenue report | LX Admin → Analytics → Revenue report |

## Where organisations see usage

| Task | Location |
|------|----------|
| Wallet balance & top-up | Platform portal → Settings → Platform billing |
| Usage & deductions | Platform portal → Settings → Platform billing → Usage |
