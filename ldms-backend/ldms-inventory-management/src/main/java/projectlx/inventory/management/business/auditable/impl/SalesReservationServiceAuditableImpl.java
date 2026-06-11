package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.SalesReservationServiceAuditable;
import projectlx.inventory.management.model.SalesReservation;
import projectlx.inventory.management.repository.SalesReservationRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SalesReservationServiceAuditableImpl implements SalesReservationServiceAuditable {

    private final SalesReservationRepository salesReservationRepository;

    @Override
    public SalesReservation create(SalesReservation salesReservation, Locale locale, String username) {
        return salesReservationRepository.save(salesReservation);
    }

    @Override
    public SalesReservation update(SalesReservation salesReservation, Locale locale, String username) {
        return salesReservationRepository.save(salesReservation);
    }

    @Override
    public SalesReservation delete(SalesReservation salesReservation, Locale locale) {
        return salesReservationRepository.save(salesReservation);
    }
}
