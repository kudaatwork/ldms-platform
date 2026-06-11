package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.SalesReservation;

import java.util.Locale;

public interface SalesReservationServiceAuditable {
    SalesReservation create(SalesReservation salesReservation, Locale locale, String username);
    SalesReservation update(SalesReservation salesReservation, Locale locale, String username);
    SalesReservation delete(SalesReservation salesReservation, Locale locale);
}
