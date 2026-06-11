package projectlx.trip.tracking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.trip.tracking.model.TripEvent;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;

public interface TripEventRepository extends JpaRepository<TripEvent, Long> {

    List<TripEvent> findByTripIdAndEntityStatusNotOrderByEventTimeDesc(Long tripId, EntityStatus entityStatus);

    List<TripEvent> findTop10ByTripIdAndEntityStatusNotOrderByEventTimeDesc(Long tripId, EntityStatus entityStatus);
}
