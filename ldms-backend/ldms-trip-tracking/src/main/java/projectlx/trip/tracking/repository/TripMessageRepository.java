package projectlx.trip.tracking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.trip.tracking.model.TripMessage;

import java.util.List;

@Repository
public interface TripMessageRepository extends JpaRepository<TripMessage, Long> {

    List<TripMessage> findByTripIdAndEntityStatusNotOrderByCreatedAtAsc(
            Long tripId, EntityStatus excluded);

    long countByTripIdAndSenderUserIdNotAndReadAtIsNullAndEntityStatusNot(
            Long tripId, Long senderUserId, EntityStatus excluded);
}
