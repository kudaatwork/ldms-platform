package projectlx.trip.tracking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.trip.tracking.model.TripRoutePlan;

import java.util.List;
import java.util.Optional;

public interface TripRoutePlanRepository extends JpaRepository<TripRoutePlan, Long> {

    Optional<TripRoutePlan> findByTripIdAndEntityStatusNot(Long tripId, EntityStatus entityStatus);

    List<TripRoutePlan> findBySimulationActiveTrueAndEntityStatusNot(EntityStatus entityStatus);
}
