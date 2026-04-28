package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long>, JpaSpecificationExecutor<District> {
    Optional<District> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<District> findByNameAndEntityStatusNot(String name, EntityStatus entityStatus);
    Optional<District> findByName(String name);
    List<District> findAllByEntityStatusNot(EntityStatus entityStatus);
    Optional<District> findByNameAndProvinceAndEntityStatusNot(String longName, Province province, EntityStatus entityStatus);
}
