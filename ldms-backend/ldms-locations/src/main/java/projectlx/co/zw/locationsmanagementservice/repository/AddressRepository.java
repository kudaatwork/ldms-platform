package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.Address;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long>, JpaSpecificationExecutor<Address> {
    Optional<Address> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
}