package projectlx.fuel.expenses.utils.enums;

public enum FuelReadingType {

    /** Point-in-time snapshot of the current fuel level (% and/or litres). */
    LEVEL_SNAPSHOT,

    /** Amount consumed since the previous reading (distance-based calculation). */
    CONSUMPTION_DELTA,

    /** Fuel dispensed at a filling station. */
    DISPENSE,

    /** Approved top-up applied to the active fuel session. */
    TOP_UP
}
