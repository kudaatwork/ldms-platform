package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.WalletDeposit;
import projectlx.billing.payments.utils.enums.WalletDepositStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;

public interface WalletDepositRepository extends JpaRepository<WalletDeposit, Long> {

    List<WalletDeposit> findByOrganizationIdAndEntityStatusNotOrderByCreatedAtDesc(
            Long organizationId, EntityStatus entityStatus);

    List<WalletDeposit> findByStatusAndEntityStatusNotOrderByCreatedAtDesc(
            WalletDepositStatus status, EntityStatus entityStatus);
}
