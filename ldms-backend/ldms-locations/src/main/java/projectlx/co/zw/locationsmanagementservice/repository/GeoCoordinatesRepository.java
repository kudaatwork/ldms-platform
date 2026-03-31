package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface GeoCoordinatesRepository extends JpaRepository<GeoCoordinates, Long>, JpaSpecificationExecutor<GeoCoordinates> {
    Optional<GeoCoordinates> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
}