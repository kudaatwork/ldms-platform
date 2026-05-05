package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface ProvinceRepository extends JpaRepository<Province, Long>, JpaSpecificationExecutor<Province> {

    List<Province> findAllByCountry_IdAndEntityStatusNot(Long countryId, EntityStatus entityStatus);

    @Query("SELECT p FROM Province p LEFT JOIN FETCH p.geoCoordinates WHERE p.id = :id "
            + "AND (p.entityStatus IS NULL OR p.entityStatus <> :excluded)")
    Optional<Province> findByIdFetchingGeoCoordinates(@Param("id") Long id, @Param("excluded") EntityStatus excluded);
    Optional<Province> findByNameAndEntityStatusNot(String name, EntityStatus entityStatus);
    Optional<Province> findByName(String name);
    Optional<Province> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<Province> findByNameAndCountry(String name, Country country);

    Optional<Province> findByNameAndCountryAndEntityStatusNot(String longName, Country country, EntityStatus entityStatus);
}
