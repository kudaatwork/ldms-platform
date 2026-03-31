package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.locationsmanagementservice.model.LocalizedName;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface LocalizedNameRepository extends JpaRepository<LocalizedName, Long>, JpaSpecificationExecutor<LocalizedName> {
    Optional<LocalizedName> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
}