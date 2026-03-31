package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long>, JpaSpecificationExecutor<Country> {
    Optional<Country> findByNameAndEntityStatusNot(String countryName, EntityStatus entityStatus);
    Optional<Country> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<Country> findByIsoAlpha2CodeAndEntityStatusNot(String shortName, EntityStatus entityStatus);
}
