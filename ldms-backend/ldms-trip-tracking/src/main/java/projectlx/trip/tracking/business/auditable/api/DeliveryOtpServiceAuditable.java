package projectlx.trip.tracking.business.auditable.api;

import projectlx.trip.tracking.model.DeliveryOtp;

import java.util.Locale;

public interface DeliveryOtpServiceAuditable {

    DeliveryOtp create(DeliveryOtp deliveryOtp, Locale locale, String username);

    DeliveryOtp update(DeliveryOtp deliveryOtp, Locale locale, String username);
}
