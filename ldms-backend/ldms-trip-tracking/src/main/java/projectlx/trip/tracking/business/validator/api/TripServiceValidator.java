package projectlx.trip.tracking.business.validator.api;

import projectlx.trip.tracking.utils.requests.RecordLocationRequest;
import projectlx.trip.tracking.utils.requests.RecordTripEventRequest;
import projectlx.trip.tracking.utils.requests.StartTripRequest;
import projectlx.trip.tracking.utils.requests.TriggerArrivalRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface TripServiceValidator {

    ValidatorDto isStartTripRequestValid(StartTripRequest request, Locale locale);

    ValidatorDto isRecordTripEventRequestValid(RecordTripEventRequest request, Locale locale);

    ValidatorDto isRecordLocationRequestValid(RecordLocationRequest request, Locale locale);

    ValidatorDto isTriggerArrivalRequestValid(TriggerArrivalRequest request, Locale locale);

    ValidatorDto isVerifyDeliveryOtpRequestValid(VerifyDeliveryOtpRequest request, Locale locale);
}
