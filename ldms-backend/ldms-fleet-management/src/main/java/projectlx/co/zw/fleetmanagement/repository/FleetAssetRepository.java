package projectlx.co.zw.fleetmanagement.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import projectlx.co.zw.fleetmanagement.model.FleetAsset;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface FleetAssetRepository extends JpaRepository<FleetAsset, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FleetAsset> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<FleetAsset> findByOrganizationIdAndEntityStatusNotOrderByIdDesc(Long organizationId, EntityStatus entityStatus);

    Optional<FleetAsset> findByIdAndOrganizationIdAndEntityStatusNot(Long id, Long organizationId, EntityStatus entityStatus);
}
