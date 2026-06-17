package projectlx.trip.tracking.business.logic.support;

import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Derives demo fuel levels from corridor distance travelled (400 L tank, 35 L/100 km).
 */
public final class TripSimulationFuelSupport {

    public static final BigDecimal DEFAULT_TANK_CAP = new BigDecimal("400.00");
    public static final BigDecimal DEFAULT_RATE = new BigDecimal("35.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private TripSimulationFuelSupport() {
    }

    public static BigDecimal fuelRemainingLiters(BigDecimal distanceTravelledKm) {
        BigDecimal distance = distanceTravelledKm != null ? distanceTravelledKm : BigDecimal.ZERO;
        BigDecimal consumed = DEFAULT_RATE.multiply(distance).divide(HUNDRED, 4, RoundingMode.HALF_UP);
        return DEFAULT_TANK_CAP.subtract(consumed).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal fuelLevelPct(BigDecimal distanceTravelledKm) {
        BigDecimal remaining = fuelRemainingLiters(distanceTravelledKm);
        return remaining.divide(DEFAULT_TANK_CAP, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(1, RoundingMode.HALF_UP);
    }

    public static void applyToSnapshot(TripLiveSnapshotDto snapshot, BigDecimal distanceTravelledKm) {
        if (snapshot == null) {
            return;
        }
        snapshot.setFuelRemainingLiters(fuelRemainingLiters(distanceTravelledKm));
        snapshot.setFuelLevelPct(fuelLevelPct(distanceTravelledKm));
    }
}
