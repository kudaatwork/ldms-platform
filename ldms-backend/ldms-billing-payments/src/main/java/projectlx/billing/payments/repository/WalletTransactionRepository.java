package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.WalletTransaction;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findTop50ByOrganizationIdAndEntityStatusNotOrderByCreatedAtDesc(
            Long organizationId, EntityStatus entityStatus);

    /** Resolves the wallet credit transaction generated when a deposit was approved. */
    Optional<WalletTransaction> findFirstByReferenceTypeAndReferenceIdAndEntityStatusNotOrderByCreatedAtDesc(
            String referenceType, Long referenceId, EntityStatus entityStatus);
}
