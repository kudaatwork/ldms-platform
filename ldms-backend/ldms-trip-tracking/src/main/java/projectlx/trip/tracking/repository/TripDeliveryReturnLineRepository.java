package projectlx.trip.tracking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.trip.tracking.model.TripDeliveryReturnLine;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;

public interface TripDeliveryReturnLineRepository extends JpaRepository<TripDeliveryReturnLine, Long> {

    List<TripDeliveryReturnLine> findByWorkflow_IdAndEntityStatusNot(Long workflowId, EntityStatus entityStatus);
}
