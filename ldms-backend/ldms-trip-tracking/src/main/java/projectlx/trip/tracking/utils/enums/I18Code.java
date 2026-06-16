package projectlx.trip.tracking.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum I18Code {

    MESSAGE_REQUEST_NULL("trip.request.null"),
    MESSAGE_FIELD_REQUIRED("trip.field.required"),
    MESSAGE_ORGANIZATION_UNRESOLVED("trip.organization.unresolved"),

    // Trip messages
    MESSAGE_TRIP_NOT_FOUND("trip.not.found"),
    MESSAGE_TRIP_START_SUCCESS("trip.start.success"),
    MESSAGE_TRIP_START_INVALID("trip.start.invalid"),
    MESSAGE_TRIP_EVENT_RECORDED_SUCCESS("trip.event.recorded.success"),
    MESSAGE_TRIP_EVENT_RECORD_INVALID("trip.event.record.invalid"),
    MESSAGE_TRIP_LOCATION_RECORDED_SUCCESS("trip.location.recorded.success"),
    MESSAGE_TRIP_LOCATION_RECORD_INVALID("trip.location.record.invalid"),
    MESSAGE_TRIP_ARRIVAL_TRIGGERED_SUCCESS("trip.arrival.triggered.success"),
    MESSAGE_TRIP_ARRIVAL_TRIGGER_INVALID("trip.arrival.trigger.invalid"),
    MESSAGE_TRIP_DELIVERY_VERIFIED_SUCCESS("trip.delivery.verified.success"),
    MESSAGE_TRIP_DELIVERY_VERIFY_INVALID("trip.delivery.verify.invalid"),
    MESSAGE_TRIP_FIND_SUCCESS("trip.find.success"),
    MESSAGE_TRIP_FIND_ALL_SUCCESS("trip.find.all.success"),
    MESSAGE_TRIP_TRACK_SUCCESS("trip.track.success"),
    MESSAGE_TRIP_LIVE_SNAPSHOT_SUCCESS("trip.live.snapshot.success"),
    MESSAGE_TRIP_DEMO_SIMULATION_STARTED("trip.demo.simulation.started"),

    // Status transition errors
    MESSAGE_TRIP_NOT_IN_TRANSIT("trip.not.in.transit"),
    MESSAGE_TRIP_NOT_ARRIVED("trip.not.arrived"),
    MESSAGE_TRIP_NOT_OTP_PENDING("trip.not.otp.pending"),
    MESSAGE_TRIP_SHIPMENT_NOT_ALLOCATED("trip.shipment.not.allocated"),
    MESSAGE_TRIP_ALREADY_ACTIVE("trip.already.active"),

    // OTP messages
    MESSAGE_OTP_INVALID_OR_EXPIRED("trip.otp.invalid.or.expired"),
    MESSAGE_OTP_ALREADY_VERIFIED("trip.otp.already.verified"),
    MESSAGE_OTP_NOT_FOUND("trip.otp.not.found");

    private final String code;
}
