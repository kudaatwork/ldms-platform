package projectlx.inventory.management.tasks;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import projectlx.inventory.management.business.logic.api.SalesReservationService;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.model.SalesReservation;
import projectlx.inventory.management.repository.SalesReservationRepository;
import projectlx.inventory.management.utils.requests.EditSalesReservationRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryScheduler {

    private final SalesReservationRepository salesReservationRepository;
    private final SalesReservationService salesReservationService;

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void expireReservations() {
        LocalDateTime now = LocalDateTime.now();

        List<SalesReservation> expiredReservations = salesReservationRepository
                .findByReservationStatusAndReservedUntilBefore(ReservationStatus.ACTIVE, now);

        for (SalesReservation reservation : expiredReservations) {
            EditSalesReservationRequest expireRequest = new EditSalesReservationRequest();
            expireRequest.setSalesReservationId(reservation.getId());
            expireRequest.setReservationStatus(ReservationStatus.EXPIRED);
            expireRequest.setUpdatedByUserId(1L); // System user

            try {
                salesReservationService.update(expireRequest, "SYSTEM", Locale.ENGLISH);
            } catch (Exception e) {
                log.error("Failed to expire reservation {}: {}", reservation.getId(), e.getMessage());
            }
        }
    }
}