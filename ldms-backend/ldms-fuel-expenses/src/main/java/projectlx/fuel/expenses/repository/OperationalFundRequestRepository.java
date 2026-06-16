package projectlx.fuel.expenses.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.fuel.expenses.model.OperationalFundRequest;
import projectlx.fuel.expenses.utils.enums.FundRequestStatus;

import java.util.Optional;

public interface OperationalFundRequestRepository extends JpaRepository<OperationalFundRequest, Long>,
        JpaSpecificationExecutor<OperationalFundRequest> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OperationalFundRequest> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<OperationalFundRequest> findByRequestNumberAndEntityStatusNot(String requestNumber,
            EntityStatus entityStatus);

    Page<OperationalFundRequest> findByTripIdAndEntityStatusNot(Long tripId, EntityStatus entityStatus,
            Pageable pageable);

    Page<OperationalFundRequest> findByOrganizationIdAndStatusAndEntityStatusNot(Long organizationId,
            FundRequestStatus status, EntityStatus entityStatus, Pageable pageable);

    Page<OperationalFundRequest> findByFleetDriverIdAndEntityStatusNot(Long fleetDriverId, EntityStatus entityStatus,
            Pageable pageable);
}
