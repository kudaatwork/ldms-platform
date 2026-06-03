package projectlx.co.zw.organizationmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.co.zw.organizationmanagement.model.PlatformKycPolicy;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface PlatformKycPolicyRepository extends JpaRepository<PlatformKycPolicy, Long> {

    Optional<PlatformKycPolicy> findFirstByEntityStatusNotOrderByIdAsc(EntityStatus deleted);
}
