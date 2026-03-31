package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.LocationNode;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface LocationNodeRepository extends JpaRepository<LocationNode, Long>, JpaSpecificationExecutor<LocationNode> {
    Optional<LocationNode> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    List<LocationNode> findByParentIdAndEntityStatusNot(Long parentId, EntityStatus entityStatus);
    List<LocationNode> findByLocationTypeAndEntityStatusNot(LocationType locationType, EntityStatus entityStatus);
    Page<LocationNode> findByNameContainingIgnoreCaseAndEntityStatusNot(String name, EntityStatus entityStatus, Pageable pageable);
}
