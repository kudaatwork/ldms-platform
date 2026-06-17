package projectlx.user.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.DemoRequisition;
import projectlx.user.management.model.DemoRequisitionStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DemoRequisitionRepository extends JpaRepository<DemoRequisition, Long> {

    Optional<DemoRequisition> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<DemoRequisition> findByEntityStatusNotOrderByCreatedAtDesc(EntityStatus entityStatus);

    long countByEntityStatusNotAndStatusIn(EntityStatus entityStatus, Collection<DemoRequisitionStatus> statuses);
}
