package projectlx.trip.tracking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import projectlx.trip.tracking.model.DeliveryOtp;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface DeliveryOtpRepository extends JpaRepository<DeliveryOtp, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryOtp> findTopByTripIdAndVerifiedAtIsNullAndEntityStatusNotOrderByIdDesc(
            Long tripId, EntityStatus entityStatus);

    Optional<DeliveryOtp> findTopByTripIdAndEntityStatusNotOrderByIdDesc(Long tripId, EntityStatus entityStatus);
}
