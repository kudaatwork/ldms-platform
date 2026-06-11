package projectlx.billing.payments.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.billing.payments.model.PlatformWallet;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface PlatformWalletRepository extends JpaRepository<PlatformWallet, Long> {

    Optional<PlatformWallet> findByOrganizationIdAndEntityStatusNot(Long organizationId, EntityStatus entityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM PlatformWallet w WHERE w.organizationId = :organizationId AND w.entityStatus <> :deleted")
    Optional<PlatformWallet> findByOrganizationIdForUpdate(
            @Param("organizationId") Long organizationId,
            @Param("deleted") EntityStatus deleted);
}
