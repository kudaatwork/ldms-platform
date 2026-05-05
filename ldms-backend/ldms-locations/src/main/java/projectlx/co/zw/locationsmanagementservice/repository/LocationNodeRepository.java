package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.locationsmanagementservice.model.LocationNode;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface LocationNodeRepository extends JpaRepository<LocationNode, Long>, JpaSpecificationExecutor<LocationNode> {
    @EntityGraph(attributePaths = {"parent", "district", "suburb", "aliases"})
    Optional<LocationNode> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    @EntityGraph(attributePaths = {"parent", "district", "suburb", "aliases"})
    List<LocationNode> findByParentIdAndEntityStatusNot(Long parentId, EntityStatus entityStatus);

    List<LocationNode> findByLocationTypeAndEntityStatusNot(LocationType locationType, EntityStatus entityStatus);
    Optional<LocationNode> findByIdAndLocationTypeAndEntityStatusNot(Long id, LocationType locationType, EntityStatus entityStatus);
    Optional<LocationNode> findFirstByDistrictIdAndLocationTypeAndEntityStatusNot(Long districtId, LocationType locationType, EntityStatus entityStatus);
    Optional<LocationNode> findFirstBySuburbIdAndLocationTypeAndEntityStatusNot(Long suburbId, LocationType locationType, EntityStatus entityStatus);
    Page<LocationNode> findByNameContainingIgnoreCaseAndEntityStatusNot(String name, EntityStatus entityStatus, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"parent", "district", "suburb"})
    Page<LocationNode> findAll(Specification<LocationNode> specification, Pageable pageable);

    /**
     * Full catalog for admin dropdowns (GET find-by-list). Graph avoids N+1 when mapping to DTOs.
     */
    @EntityGraph(attributePaths = {"parent", "district", "suburb", "aliases"})
    List<LocationNode> findAllByEntityStatusNot(EntityStatus entityStatus);

    List<LocationNode> findAllBySuburb_IdAndEntityStatusNot(Long suburbId, EntityStatus entityStatus);

    List<LocationNode> findAllByDistrict_IdAndSuburbIsNullAndEntityStatusNot(Long districtId, EntityStatus entityStatus);
}
