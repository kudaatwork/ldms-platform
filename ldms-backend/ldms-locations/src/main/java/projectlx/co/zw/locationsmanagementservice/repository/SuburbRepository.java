package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface SuburbRepository extends JpaRepository<Suburb, Long>, JpaSpecificationExecutor<Suburb> {

    List<Suburb> findAllByDistrict_IdAndEntityStatusNot(Long districtId, EntityStatus entityStatus);

    @Query("SELECT s FROM Suburb s LEFT JOIN FETCH s.geoCoordinates WHERE s.id = :id "
            + "AND (s.entityStatus IS NULL OR s.entityStatus <> :excluded)")
    Optional<Suburb> findByIdFetchingGeoCoordinates(@Param("id") Long id, @Param("excluded") EntityStatus excluded);
    Optional<Suburb> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<Suburb> findByNameAndDistrict(String name, District district);

    Optional<Suburb> findByNameAndDistrictAndEntityStatusNot(String name, District district, EntityStatus entityStatus);
}