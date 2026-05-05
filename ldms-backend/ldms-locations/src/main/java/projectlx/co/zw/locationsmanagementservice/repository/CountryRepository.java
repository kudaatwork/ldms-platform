package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long>, JpaSpecificationExecutor<Country> {

    /**
     * Loads {@code geoCoordinates} in the same query so callers can copy default coordinates for province CSV import
     * without lazy-load issues outside a transaction.
     */
    @Query("SELECT c FROM Country c LEFT JOIN FETCH c.geoCoordinates WHERE c.id = :id "
            + "AND (c.entityStatus IS NULL OR c.entityStatus <> :excluded)")
    Optional<Country> findByIdFetchingGeoCoordinates(@Param("id") Long id, @Param("excluded") EntityStatus excluded);
    Optional<Country> findByNameAndEntityStatusNot(String countryName, EntityStatus entityStatus);
    Optional<Country> findByName(String countryName);
    Optional<Country> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<Country> findByIsoAlpha2CodeAndEntityStatusNot(String shortName, EntityStatus entityStatus);
    Optional<Country> findByIsoAlpha2Code(String shortName);
    Optional<Country> findByIsoAlpha3CodeAndEntityStatusNot(String shortName, EntityStatus entityStatus);
    Optional<Country> findByIsoAlpha3Code(String shortName);
}
