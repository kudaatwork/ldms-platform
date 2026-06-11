package projectlx.inventory.management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.model.SalesReservation;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesReservationRepository extends JpaRepository<SalesReservation, Long>, JpaSpecificationExecutor<SalesReservation> {
    List<SalesReservation> findByEntityStatusNot(EntityStatus entityStatus);
    Optional<SalesReservation> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    // Corrected method naming to use reservedUntil and support pagination
    Page<SalesReservation> findByReservationStatusAndReservedUntilBeforeAndEntityStatusNot(
            ReservationStatus reservationStatus,
            LocalDateTime now,
            EntityStatus entityStatus,
            Pageable pageable
    );

    List<SalesReservation> findByReservationStatusAndReservedUntilBefore(ReservationStatus reservationStatus, LocalDateTime now);
}
