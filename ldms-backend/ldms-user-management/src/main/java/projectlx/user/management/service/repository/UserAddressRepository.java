package projectlx.user.management.service.repository;

import projectlx.user.management.service.model.Address;
import projectlx.user.management.service.model.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<Address, Long>, JpaSpecificationExecutor<Address> {
    Optional<Address> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    List<Address> findByEntityStatusNot(EntityStatus entityStatus);
    long countByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<Address> findByLocationAddressIdAndEntityStatusNot(Long locationAddressId, EntityStatus entityStatus);
}
