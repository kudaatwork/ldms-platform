package projectlx.fuel.expenses.utils.enums;

public enum I18Code {

    MESSAGE_TRIP_ID_REQUIRED("message.trip.id.required"),
    MESSAGE_TRIP_ID_INVALID("message.trip.id.invalid"),
    MESSAGE_FUEL_SESSION_NOT_FOUND("message.fuel.session.not.found"),
    MESSAGE_FUEL_SESSION_ALREADY_EXISTS("message.fuel.session.already.exists"),
    MESSAGE_SUCCESS("message.success"),

    // ── Operational Fund Request ──
    MESSAGE_FUND_REQUEST_ID_REQUIRED("message.fund.request.id.required"),
    MESSAGE_FUND_REQUEST_ID_INVALID("message.fund.request.id.invalid"),
    MESSAGE_FUND_REQUEST_NOT_FOUND("message.fund.request.not.found"),
    MESSAGE_FUND_REQUEST_TRIP_ID_REQUIRED("message.fund.request.trip.id.required"),
    MESSAGE_FUND_REQUEST_DRIVER_ID_REQUIRED("message.fund.request.driver.id.required"),
    MESSAGE_FUND_REQUEST_TYPE_REQUIRED("message.fund.request.type.required"),
    MESSAGE_FUND_REQUEST_LITERS_REQUIRED("message.fund.request.liters.required"),
    MESSAGE_FUND_REQUEST_LITERS_INVALID("message.fund.request.liters.invalid"),
    MESSAGE_FUND_REQUEST_AMOUNT_REQUIRED("message.fund.request.amount.required"),
    MESSAGE_FUND_REQUEST_AMOUNT_INVALID("message.fund.request.amount.invalid"),
    MESSAGE_FUND_REQUEST_REJECTION_REASON_REQUIRED("message.fund.request.rejection.reason.required"),
    MESSAGE_FUND_REQUEST_NOT_PENDING("message.fund.request.not.pending"),
    MESSAGE_FUND_REQUEST_APPROVED_LITERS_REQUIRED("message.fund.request.approved.liters.required"),
    MESSAGE_FUND_REQUEST_APPROVED_AMOUNT_REQUIRED("message.fund.request.approved.amount.required"),

    // ── Fuel Telemetry Log ──
    MESSAGE_TELEMETRY_TRIP_ID_REQUIRED("message.telemetry.trip.id.required"),
    MESSAGE_TELEMETRY_SOURCE_REQUIRED("message.telemetry.source.required"),
    MESSAGE_TELEMETRY_READING_TYPE_REQUIRED("message.telemetry.reading.type.required"),
    MESSAGE_TELEMETRY_LOG_NOT_FOUND("message.telemetry.log.not.found");

    private final String code;

    I18Code(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
