package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.inventory.management.model.OrganizationProcurementSetting;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface OrganizationProcurementSettingRepository extends JpaRepository<OrganizationProcurementSetting, Long> {

    Optional<OrganizationProcurementSetting> findByOrganizationIdAndEntityStatusNot(
            Long organizationId, EntityStatus entityStatus);
}
