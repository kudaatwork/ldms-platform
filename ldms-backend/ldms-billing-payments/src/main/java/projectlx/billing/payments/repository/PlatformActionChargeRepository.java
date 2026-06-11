package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.PlatformActionCharge;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface PlatformActionChargeRepository extends JpaRepository<PlatformActionCharge, Long> {

    List<PlatformActionCharge> findByEntityStatusNotOrderByCategoryAscDisplayNameAsc(EntityStatus entityStatus);

    Optional<PlatformActionCharge> findByActionCodeAndEntityStatusNot(String actionCode, EntityStatus entityStatus);
}
