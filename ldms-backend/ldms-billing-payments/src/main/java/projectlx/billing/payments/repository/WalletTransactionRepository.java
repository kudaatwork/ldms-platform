package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.WalletTransaction;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findTop50ByOrganizationIdAndEntityStatusNotOrderByCreatedAtDesc(
            Long organizationId, EntityStatus entityStatus);
}
