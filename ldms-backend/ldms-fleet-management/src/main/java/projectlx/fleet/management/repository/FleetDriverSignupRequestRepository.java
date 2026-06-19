package projectlx.fleet.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.fleet.management.model.FleetDriverSignupRequest;
import projectlx.fleet.management.utils.enums.DriverSignupRequestStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface FleetDriverSignupRequestRepository extends JpaRepository<FleetDriverSignupRequest, Long> {

    boolean existsByEmailAndStatusAndEntityStatusNot(
            String email,
            DriverSignupRequestStatus status,
            EntityStatus entityStatus);

    Optional<FleetDriverSignupRequest> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    /** COMPANY requests pending for a specific organisation (matched by companyCode = orgId string). */
    List<FleetDriverSignupRequest> findByOrganizationIdAndStatusAndEntityStatusNotOrderByIdDesc(
            Long organizationId,
            DriverSignupRequestStatus status,
            EntityStatus entityStatus);

    /** All PENDING FREELANCE requests across the platform. */
    List<FleetDriverSignupRequest> findBySignupTypeAndStatusAndEntityStatusNotOrderByIdDesc(
            String signupType,
            DriverSignupRequestStatus status,
            EntityStatus entityStatus);
}
