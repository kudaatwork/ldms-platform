package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.SubscriptionPackage;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPackageRepository extends JpaRepository<SubscriptionPackage, Long> {

    List<SubscriptionPackage> findByEntityStatusNotAndActiveTrueOrderBySortOrderAsc(EntityStatus entityStatus);

    List<SubscriptionPackage> findByEntityStatusNotOrderBySortOrderAsc(EntityStatus entityStatus);

    Optional<SubscriptionPackage> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<SubscriptionPackage> findByCodeAndEntityStatusNot(String code, EntityStatus entityStatus);
}
