package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface OrganizationBillingSettingRepository extends JpaRepository<OrganizationBillingSetting, Long> {

    Optional<OrganizationBillingSetting> findByOrganizationIdAndEntityStatusNot(Long organizationId, EntityStatus entityStatus);

    List<OrganizationBillingSetting> findByEntityStatusNot(EntityStatus entityStatus);

    long countBySubscriptionPackageIdAndEntityStatusNot(Long subscriptionPackageId, EntityStatus entityStatus);
}
