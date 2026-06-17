package projectlx.trip.tracking.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.trip.tracking.model.TripEvent;
import projectlx.trip.tracking.utils.enums.TripEventType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;

public interface TripEventRepository extends JpaRepository<TripEvent, Long> {

    List<TripEvent> findByTrip_IdAndEntityStatusNotOrderByEventTimeDesc(Long tripId, EntityStatus entityStatus);

    List<TripEvent> findTop10ByTrip_IdAndEntityStatusNotOrderByEventTimeDesc(Long tripId, EntityStatus entityStatus);

    List<TripEvent> findByTrip_IdAndEventTypeNotAndEntityStatusNotOrderByEventTimeDesc(
            Long tripId, TripEventType excludedType, EntityStatus entityStatus, Pageable pageable);
}
