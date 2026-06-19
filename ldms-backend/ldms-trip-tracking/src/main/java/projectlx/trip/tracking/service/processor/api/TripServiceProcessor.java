package projectlx.trip.tracking.service.processor.api;

import projectlx.trip.tracking.utils.requests.RecordLocationRequest;
import projectlx.trip.tracking.utils.requests.RecordTripEventRequest;
import projectlx.trip.tracking.utils.requests.StartTripRequest;
import projectlx.trip.tracking.utils.requests.TriggerArrivalRequest;
import projectlx.trip.tracking.utils.requests.TripFilterRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.trip.tracking.utils.responses.TripResponse;

import java.util.Locale;

public interface TripServiceProcessor {

    TripResponse startTrip(StartTripRequest request, Locale locale, String username);

    TripResponse recordEvent(RecordTripEventRequest request, Locale locale, String username);

    /**
     * Records a trip event for inter-service (system) callers.
     * Bypasses authentication context — username is fixed as "system".
     */
    TripResponse recordSystemEvent(RecordTripEventRequest request, Locale locale);

    TripResponse recordLocation(RecordLocationRequest request, Locale locale, String username);

    TripResponse triggerArrival(TriggerArrivalRequest request, Locale locale, String username);

    TripResponse verifyDeliveryOtp(VerifyDeliveryOtpRequest request, Locale locale, String username);

    TripResponse findById(Long id, Locale locale, String username);

    TripResponse findByMultipleFilters(TripFilterRequest request, Locale locale, String username);

    TripResponse track(Long id, Locale locale);

    TripResponse listMyTrips(Locale locale, String username);

    TripResponse findMyTripById(Long tripId, Locale locale, String username);

    TripResponse getMyTripMetrics(Locale locale, String username);
}
