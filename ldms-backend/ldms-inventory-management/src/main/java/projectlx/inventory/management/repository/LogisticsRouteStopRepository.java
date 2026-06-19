package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.inventory.management.model.LogisticsRouteStop;
import projectlx.inventory.management.model.RouteStopContextType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface LogisticsRouteStopRepository
        extends JpaRepository<LogisticsRouteStop, Long>, JpaSpecificationExecutor<LogisticsRouteStop> {

    List<LogisticsRouteStop> findByContextTypeAndContextIdAndEntityStatusNotOrderByStopSequenceAsc(
            RouteStopContextType contextType, Long contextId, EntityStatus entityStatus);

    Optional<LogisticsRouteStop> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    @Modifying
    @Query("UPDATE LogisticsRouteStop r SET r.entityStatus = :status " +
           "WHERE r.contextType = :contextType AND r.contextId = :contextId")
    int softDeleteByContext(@Param("contextType") RouteStopContextType contextType,
                            @Param("contextId") Long contextId,
                            @Param("status") EntityStatus status);
}
