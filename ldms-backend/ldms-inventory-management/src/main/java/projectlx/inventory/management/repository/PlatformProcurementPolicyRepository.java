package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.inventory.management.model.PlatformProcurementPolicy;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface PlatformProcurementPolicyRepository extends JpaRepository<PlatformProcurementPolicy, Long> {

    Optional<PlatformProcurementPolicy> findFirstByEntityStatusNotOrderByIdAsc(EntityStatus entityStatus);
}
