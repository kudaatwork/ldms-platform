package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long>, JpaSpecificationExecutor<District> {

    @Query("SELECT d FROM District d LEFT JOIN FETCH d.geoCoordinates WHERE d.id = :id "
            + "AND (d.entityStatus IS NULL OR d.entityStatus <> :excluded)")
    Optional<District> findByIdFetchingGeoCoordinates(@Param("id") Long id, @Param("excluded") EntityStatus excluded);
    List<District> findAllByProvince_IdAndEntityStatusNot(Long provinceId, EntityStatus entityStatus);

    Optional<District> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<District> findByNameAndEntityStatusNot(String name, EntityStatus entityStatus);
    Optional<District> findByName(String name);
    List<District> findAllByEntityStatusNot(EntityStatus entityStatus);
    Optional<District> findByNameAndProvince(String name, Province province);

    Optional<District> findByNameAndProvinceAndEntityStatusNot(String longName, Province province, EntityStatus entityStatus);
}
