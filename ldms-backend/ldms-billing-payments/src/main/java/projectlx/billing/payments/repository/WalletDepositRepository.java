package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.billing.payments.model.WalletDeposit;
import projectlx.billing.payments.utils.enums.WalletDepositStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface WalletDepositRepository extends JpaRepository<WalletDeposit, Long> {

    List<WalletDeposit> findByOrganizationIdAndEntityStatusNotOrderByCreatedAtDesc(
            Long organizationId, EntityStatus entityStatus);

    List<WalletDeposit> findByStatusAndEntityStatusNotOrderByCreatedAtDesc(
            WalletDepositStatus status, EntityStatus entityStatus);

    List<WalletDeposit> findByStatusAndEntityStatusNotOrderByModifiedAtDesc(
            WalletDepositStatus status, EntityStatus entityStatus);

    @Query("""
            SELECT d.organizationId, SUM(d.amountCents)
            FROM WalletDeposit d
            WHERE d.entityStatus <> :deleted
              AND d.status = :status
            GROUP BY d.organizationId
            """)
    List<Object[]> aggregateDepositsByOrganization(
            @Param("status") WalletDepositStatus status,
            @Param("deleted") EntityStatus deleted);

    @Query("""
            SELECT YEAR(COALESCE(d.modifiedAt, d.createdAt)),
                   MONTH(COALESCE(d.modifiedAt, d.createdAt)),
                   SUM(d.amountCents)
            FROM WalletDeposit d
            WHERE d.entityStatus <> :deleted
              AND d.status = :status
              AND COALESCE(d.modifiedAt, d.createdAt) >= :from
            GROUP BY YEAR(COALESCE(d.modifiedAt, d.createdAt)),
                     MONTH(COALESCE(d.modifiedAt, d.createdAt))
            ORDER BY YEAR(COALESCE(d.modifiedAt, d.createdAt)),
                     MONTH(COALESCE(d.modifiedAt, d.createdAt))
            """)
    List<Object[]> sumConfirmedDepositsByMonth(
            @Param("from") LocalDateTime from,
            @Param("status") WalletDepositStatus status,
            @Param("deleted") EntityStatus deleted);
}
