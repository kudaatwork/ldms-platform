package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface AdministrativeLevelRepository extends JpaRepository<AdministrativeLevel, Long>,
        JpaSpecificationExecutor<AdministrativeLevel> {

    List<AdministrativeLevel> findAllByCountry_IdAndEntityStatusNot(Long countryId, EntityStatus entityStatus);

    Optional<AdministrativeLevel> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<AdministrativeLevel> findByCountry_IdAndNameAndEntityStatusNot(Long countryId, String name,
                                                                            EntityStatus entityStatus);

    Optional<AdministrativeLevel> findFirstByCountry_IdAndNameAndEntityStatus(Long countryId, String name,
                                                                              EntityStatus entityStatus);
}
