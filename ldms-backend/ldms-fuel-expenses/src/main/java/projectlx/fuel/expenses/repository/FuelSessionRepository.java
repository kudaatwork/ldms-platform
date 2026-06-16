package projectlx.fuel.expenses.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.fuel.expenses.model.FuelSession;
import projectlx.fuel.expenses.utils.enums.FuelSessionStatus;

import java.util.Optional;

public interface FuelSessionRepository extends JpaRepository<FuelSession, Long>,
        JpaSpecificationExecutor<FuelSession> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FuelSession> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FuelSession> findByTripIdAndStatusAndEntityStatusNot(Long tripId, FuelSessionStatus status,
            EntityStatus entityStatus);

    Optional<FuelSession> findByTripIdAndEntityStatusNot(Long tripId, EntityStatus entityStatus);

    boolean existsByTripIdAndEntityStatusNot(Long tripId, EntityStatus entityStatus);
}
