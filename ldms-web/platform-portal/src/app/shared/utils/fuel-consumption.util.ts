/** Platform wallet action codes tied to fuel consumption billing. */
export const FUEL_CONSUMPTION_BILLING_ACTION_CODES = ['FUEL_FUND_REQUEST'] as const;

export function isFuelConsumptionBillingAction(actionCode?: string | null): boolean {
  if (!actionCode) {
    return false;
  }
  return FUEL_CONSUMPTION_BILLING_ACTION_CODES.includes(
    actionCode.toUpperCase() as (typeof FUEL_CONSUMPTION_BILLING_ACTION_CODES)[number],
  );
}

export function isFuelConsumptionFeatureEnabled(
  fuelConsumptionEnabled?: boolean | null,
): boolean {
  return fuelConsumptionEnabled === true;
}

export function isFuelConsumptionPackageAvailable(
  fuelConsumptionAvailable?: boolean | null,
): boolean {
  return fuelConsumptionAvailable !== false;
}
