package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface ProvinceRepository extends JpaRepository<Province, Long>, JpaSpecificationExecutor<Province> {
    Optional<Province> findByNameAndEntityStatusNot(String name, EntityStatus entityStatus);
    Optional<Province> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<Province> findByNameAndCountryAndEntityStatusNot(String longName, Country country, EntityStatus entityStatus);
}
