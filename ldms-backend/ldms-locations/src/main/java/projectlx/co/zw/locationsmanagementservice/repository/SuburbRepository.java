package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface SuburbRepository extends JpaRepository<Suburb, Long>, JpaSpecificationExecutor<Suburb> {
    Optional<Suburb> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<Suburb> findByName(String name);
    <T> Optional<T> findByNameAndDistrictAndEntityStatusNot(String longName, District district, EntityStatus entityStatus);
}