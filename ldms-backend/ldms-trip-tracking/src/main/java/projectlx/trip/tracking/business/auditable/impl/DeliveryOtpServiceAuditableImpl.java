package projectlx.trip.tracking.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.trip.tracking.business.auditable.api.DeliveryOtpServiceAuditable;
import projectlx.trip.tracking.model.DeliveryOtp;
import projectlx.trip.tracking.repository.DeliveryOtpRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class DeliveryOtpServiceAuditableImpl implements DeliveryOtpServiceAuditable {

    private final DeliveryOtpRepository deliveryOtpRepository;

    @Override
    public DeliveryOtp create(DeliveryOtp deliveryOtp, Locale locale, String username) {
        return deliveryOtpRepository.save(deliveryOtp);
    }

    @Override
    public DeliveryOtp update(DeliveryOtp deliveryOtp, Locale locale, String username) {
        return deliveryOtpRepository.save(deliveryOtp);
    }
}
