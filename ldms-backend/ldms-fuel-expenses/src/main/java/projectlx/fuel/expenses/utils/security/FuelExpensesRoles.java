package projectlx.fuel.expenses.utils.security;

public enum FuelExpensesRoles {

    VIEW_FUEL_SESSION,
    MANAGE_FUEL_SESSION,

    // Operational Fund Request
    CREATE_FUND_REQUEST,
    APPROVE_FUND_REQUEST,
    REJECT_FUND_REQUEST,
    CANCEL_FUND_REQUEST,
    VIEW_FUND_REQUEST,

    // Fuel Telemetry Log
    RECORD_FUEL_TELEMETRY,
    VIEW_FUEL_TELEMETRY
}
