package projectlx.co.zw.organizationmanagement.business.kyc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import projectlx.co.zw.organizationmanagement.clients.UserManagementServiceClient;
import projectlx.co.zw.organizationmanagement.model.KycStatus;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.repository.OrganizationRepository;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Assigns stage-1 and stage-2 KYC reviewers to signup organisations using a least-load algorithm.
 * Approver candidates are admin-portal users ({@code organizationKycApprover=true}, no organisation).
 * Stage 1 and stage 2 must always be different individuals when two or more candidates exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycApproverAssignmentService {

    private static final Set<KycStatus> STAGE1_WORKLOAD_STATUSES = EnumSet.of(
            KycStatus.DRAFT,
            KycStatus.SUBMITTED,
            KycStatus.STAGE_1_REVIEW,
            KycStatus.RESUBMITTED);

    private static final Set<KycStatus> STAGE2_WORKLOAD_STATUSES = EnumSet.of(KycStatus.STAGE_2_REVIEW);

    private final UserManagementServiceClient userManagementServiceClient;
    private final OrganizationRepository organizationRepository;

    /**
     * Picks and persists stage-1 and stage-2 approvers on the organisation (different users when possible).
     */
    public void assignApprovers(Organization organization) {
        if (organization == null || !Boolean.TRUE.equals(organization.getCreatedViaSignup())) {
            return;
        }
        List<ApproverCandidate> candidates = loadApproverCandidates();
        if (candidates.isEmpty()) {
            log.warn("No organisation KYC approver candidates available; organisation id={} left unassigned",
                    organization.getId());
            clearAssignments(organization);
            return;
        }
        Map<Long, Long> stage1Loads = loadStage1Loads(candidates);
        Map<Long, Long> stage2Loads = loadStage2Loads(candidates);

        Optional<ApproverCandidate> stage1Opt = pickLeastLoaded(candidates, stage1Loads, null);
        if (stage1Opt.isEmpty()) {
            clearAssignments(organization);
            return;
        }
        ApproverCandidate stage1 = stage1Opt.get();
        organization.setAssignedStage1ApproverUserId(stage1.userId());
        organization.setAssignedStage1ApproverUsername(stage1.username());

        Optional<ApproverCandidate> stage2Opt = pickLeastLoaded(candidates, stage2Loads, stage1.userId());
        if (stage2Opt.isEmpty()) {
            organization.setAssignedStage2ApproverUserId(null);
            organization.setAssignedStage2ApproverUsername(null);
            log.warn(
                    "Only one KYC approver candidate available; stage-2 left unassigned for organisation id={} (stage1={})",
                    organization.getId(),
                    stage1.username());
        } else {
            ApproverCandidate stage2 = stage2Opt.get();
            organization.setAssignedStage2ApproverUserId(stage2.userId());
            organization.setAssignedStage2ApproverUsername(stage2.username());
            log.info("Assigned KYC approvers for organisation id={}: stage1={} (load {}), stage2={} (load {})",
                    organization.getId(),
                    stage1.username(),
                    stage1Loads.getOrDefault(stage1.userId(), 0L),
                    stage2.username(),
                    stage2Loads.getOrDefault(stage2.userId(), 0L));
        }
    }

    private void clearAssignments(Organization organization) {
        organization.setAssignedStage1ApproverUserId(null);
        organization.setAssignedStage1ApproverUsername(null);
        organization.setAssignedStage2ApproverUserId(null);
        organization.setAssignedStage2ApproverUsername(null);
    }

    private List<ApproverCandidate> loadApproverCandidates() {
        List<ApproverCandidate> result = new ArrayList<>();
        try {
            UserResponse response = userManagementServiceClient.listOrganizationKycApprovers();
            if (response == null || !response.isSuccess() || response.getUserDtoList() == null) {
                return result;
            }
            for (UserDto dto : response.getUserDtoList()) {
                if (dto.getId() == null || dto.getUsername() == null || dto.getUsername().isBlank()) {
                    continue;
                }
                if (dto.getOrganizationId() != null) {
                    continue;
                }
                String display = buildDisplayName(dto);
                result.add(new ApproverCandidate(dto.getId(), dto.getUsername().trim(), display));
            }
        } catch (Exception ex) {
            log.error("Failed to load organisation KYC approvers from user-management", ex);
        }
        return result;
    }

    private Map<Long, Long> loadStage1Loads(List<ApproverCandidate> candidates) {
        Map<Long, Long> loads = new HashMap<>();
        for (ApproverCandidate candidate : candidates) {
            long count = organizationRepository
                    .countByAssignedStage1ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
                            candidate.userId(), STAGE1_WORKLOAD_STATUSES, EntityStatus.DELETED);
            loads.put(candidate.userId(), count);
        }
        return loads;
    }

    private Map<Long, Long> loadStage2Loads(List<ApproverCandidate> candidates) {
        Map<Long, Long> loads = new HashMap<>();
        for (ApproverCandidate candidate : candidates) {
            long count = organizationRepository
                    .countByAssignedStage2ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
                            candidate.userId(), STAGE2_WORKLOAD_STATUSES, EntityStatus.DELETED);
            loads.put(candidate.userId(), count);
        }
        return loads;
    }

    private Optional<ApproverCandidate> pickLeastLoaded(
            List<ApproverCandidate> candidates,
            Map<Long, Long> loads,
            Long excludeUserId) {
        return candidates.stream()
                .filter(c -> !Objects.equals(c.userId(), excludeUserId))
                .min(Comparator
                        .comparingLong((ApproverCandidate c) -> loads.getOrDefault(c.userId(), 0L))
                        .thenComparing(ApproverCandidate::userId));
    }

    private static String buildDisplayName(UserDto dto) {
        String first = dto.getFirstName() != null ? dto.getFirstName().trim() : "";
        String last = dto.getLastName() != null ? dto.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? dto.getUsername() : full;
    }

    private record ApproverCandidate(Long userId, String username, String displayName) {
    }
}
