package projectlx.trip.tracking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.trip.tracking.model.TripDeliveryWorkflow;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface TripDeliveryWorkflowRepository extends JpaRepository<TripDeliveryWorkflow, Long> {

    @Query("SELECT w FROM TripDeliveryWorkflow w WHERE w.trip.id = :tripId AND w.entityStatus <> :excluded")
    Optional<TripDeliveryWorkflow> findByTripIdAndEntityStatusNot(
            @Param("tripId") Long tripId,
            @Param("excluded") EntityStatus excluded);
}
