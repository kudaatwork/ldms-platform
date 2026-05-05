package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.City;
import projectlx.co.zw.locationsmanagementservice.model.Village;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface VillageRepository extends JpaRepository<Village, Long>, JpaSpecificationExecutor<Village> {

    List<Village> findAllByCity_IdAndEntityStatusNot(Long cityId, EntityStatus entityStatus);

    List<Village> findAllBySuburb_IdAndEntityStatusNot(Long suburbId, EntityStatus entityStatus);

    Optional<Village> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<Village> findAllByEntityStatusNot(EntityStatus entityStatus);

    Optional<Village> findByNameAndCityAndEntityStatusNot(String name, City city, EntityStatus entityStatus);

    Optional<Village> findByNameAndCity(String name, City city);
}
