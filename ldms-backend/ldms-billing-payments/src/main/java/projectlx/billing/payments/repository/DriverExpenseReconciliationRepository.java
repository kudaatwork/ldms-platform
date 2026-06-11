package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.DriverExpenseReconciliation;
import projectlx.billing.payments.utils.enums.DriverExpenseReconciliationStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface DriverExpenseReconciliationRepository extends JpaRepository<DriverExpenseReconciliation, Long> {

    List<DriverExpenseReconciliation> findByOrganizationIdAndEntityStatusNotOrderByExpenseDateDesc(
            Long organizationId, EntityStatus entityStatus);

    Optional<DriverExpenseReconciliation> findByIdAndOrganizationIdAndEntityStatusNot(
            Long id, Long organizationId, EntityStatus entityStatus);

    List<DriverExpenseReconciliation> findByOrganizationIdAndStatusAndEntityStatusNot(
            Long organizationId, DriverExpenseReconciliationStatus status, EntityStatus entityStatus);
}
