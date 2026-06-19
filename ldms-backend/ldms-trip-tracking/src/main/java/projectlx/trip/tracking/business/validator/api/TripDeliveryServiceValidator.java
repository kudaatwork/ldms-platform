package projectlx.trip.tracking.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.trip.tracking.utils.requests.FinishCountingRequest;
import projectlx.trip.tracking.utils.requests.RecordReturnLinesRequest;
import projectlx.trip.tracking.utils.requests.SendDeliveryOtpRequest;
import projectlx.trip.tracking.utils.requests.StartCountingRequest;

import java.util.Locale;

public interface TripDeliveryServiceValidator {

    ValidatorDto isStartCountingRequestValid(StartCountingRequest request, Locale locale);

    ValidatorDto isFinishCountingRequestValid(FinishCountingRequest request, Locale locale);

    ValidatorDto isSendDeliveryOtpRequestValid(SendDeliveryOtpRequest request, Locale locale);

    ValidatorDto isRecordReturnLinesRequestValid(RecordReturnLinesRequest request, Locale locale);
}
