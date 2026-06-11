package projectlx.inventory.management.batch.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.model.SalesReservation;

@RequiredArgsConstructor
@Slf4j
public class ExpiredReservationProcessor implements ItemProcessor<SalesReservation, SalesReservation> {

    @Override
    public SalesReservation process(SalesReservation reservation) throws Exception {
        log.info("Processing expired reservation: {}", reservation.getReservationNumber());
        reservation.setReservationStatus(ReservationStatus.EXPIRED);
        return reservation;
    }
}