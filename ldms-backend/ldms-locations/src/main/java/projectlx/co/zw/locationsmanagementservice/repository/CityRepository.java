package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.City;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long>, JpaSpecificationExecutor<City> {

    List<City> findAllByDistrict_IdAndEntityStatusNot(Long districtId, EntityStatus entityStatus);

    Optional<City> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<City> findAllByEntityStatusNot(EntityStatus entityStatus);

    Optional<City> findByNameAndDistrict(String name, District district);

    Optional<City> findByNameAndDistrictAndEntityStatusNot(String name, District district, EntityStatus entityStatus);
}
