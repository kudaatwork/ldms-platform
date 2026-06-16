package projectlx.fuel.expenses.utils.enums;

public enum FuelSessionStatus {

    /** Session is open — the trip is in progress and fuel levels are being tracked. */
    ACTIVE,

    /** Session has been closed on trip completion or cancellation. */
    COMPLETED,

    /** Session is temporarily suspended (e.g. trip paused). */
    SUSPENDED
}
