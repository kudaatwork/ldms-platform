/** Fuel level thresholds for live tracking and shell alerts (percent). */
export const FUEL_WARN_LEVEL_PCT = 35;
export const FUEL_CRITICAL_LEVEL_PCT = 15;

export type FuelAlertTone = 'ok' | 'warn' | 'critical';

export function fuelAlertTone(fuelLevelPct: number | null | undefined): FuelAlertTone {
  const pct = fuelLevelPct ?? 100;
  if (pct <= FUEL_CRITICAL_LEVEL_PCT) {
    return 'critical';
  }
  if (pct <= FUEL_WARN_LEVEL_PCT) {
    return 'warn';
  }
  return 'ok';
}
