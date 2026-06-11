package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.OrganizationCurrencySetting;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface OrganizationCurrencySettingRepository extends JpaRepository<OrganizationCurrencySetting, Long> {

    Optional<OrganizationCurrencySetting> findByOrganizationIdAndEntityStatusNot(Long organizationId, EntityStatus entityStatus);
}
