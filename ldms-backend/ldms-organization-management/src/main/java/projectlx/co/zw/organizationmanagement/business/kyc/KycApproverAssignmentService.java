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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Assigns KYC reviewers (1–5 stages) to signup organisations using a least-load algorithm.
 * Approver candidates are admin-portal users ({@code organizationKycApprover=true}, no organisation).
 * Each stage must be a different individual when enough candidates exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycApproverAssignmentService {

    private final UserManagementServiceClient userManagementServiceClient;
    private final OrganizationRepository organizationRepository;
    private final KycApprovalStageResolver kycApprovalStageResolver;

    /**
     * Picks and persists KYC reviewers on the organisation based on the required stage count.
     */
    public void assignApprovers(Organization organization) {
        if (organization == null || !Boolean.TRUE.equals(organization.getCreatedViaSignup())) {
            return;
        }
        int requiredStages = kycApprovalStageResolver.resolveRequiredStages(organization);
        List<ApproverCandidate> candidates = loadApproverCandidates();
        if (candidates.isEmpty()) {
            log.warn("No organisation KYC approver candidates available; organisation id={} left unassigned",
                    organization.getId());
            KycStageSupport.clearAllAssignedApprovers(organization);
            return;
        }

        Set<Long> alreadyAssigned = new HashSet<>();
        for (int stage = KycStageSupport.MIN_STAGE; stage <= KycStageSupport.MAX_STAGE; stage++) {
            if (stage > requiredStages) {
                KycStageSupport.clearAssignedApprover(organization, stage);
                continue;
            }
            Map<Long, Long> loads = loadStageLoads(candidates, stage);
            Optional<ApproverCandidate> picked = pickLeastLoaded(candidates, loads, alreadyAssigned);
            if (picked.isEmpty()) {
                KycStageSupport.clearAssignedApprover(organization, stage);
                log.warn(
                        "Insufficient distinct KYC approver candidates; stage {} left unassigned for organisation id={}",
                        stage,
                        organization.getId());
                continue;
            }
            ApproverCandidate approver = picked.get();
            KycStageSupport.setAssignedApprover(organization, stage, approver.userId(), approver.username());
            alreadyAssigned.add(approver.userId());
            log.info("Assigned KYC stage {} approver for organisation id={}: {} (load {})",
                    stage,
                    organization.getId(),
                    approver.username(),
                    loads.getOrDefault(approver.userId(), 0L));
        }
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

    private Map<Long, Long> loadStageLoads(List<ApproverCandidate> candidates, int stage) {
        Map<Long, Long> loads = new HashMap<>();
        Set<KycStatus> statuses = KycStageSupport.workloadStatuses(stage);
        for (ApproverCandidate candidate : candidates) {
            long count = countAssignedLoad(candidate.userId(), stage, statuses);
            loads.put(candidate.userId(), count);
        }
        return loads;
    }

    private long countAssignedLoad(Long userId, int stage, Set<KycStatus> statuses) {
        return switch (stage) {
            case 1 -> organizationRepository
                    .countByAssignedStage1ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
                            userId, statuses, EntityStatus.DELETED);
            case 2 -> organizationRepository
                    .countByAssignedStage2ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
                            userId, statuses, EntityStatus.DELETED);
            case 3 -> organizationRepository
                    .countByAssignedStage3ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
                            userId, statuses, EntityStatus.DELETED);
            case 4 -> organizationRepository
                    .countByAssignedStage4ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
                            userId, statuses, EntityStatus.DELETED);
            case 5 -> organizationRepository
                    .countByAssignedStage5ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
                            userId, statuses, EntityStatus.DELETED);
            default -> 0L;
        };
    }

    private Optional<ApproverCandidate> pickLeastLoaded(
            List<ApproverCandidate> candidates,
            Map<Long, Long> loads,
            Set<Long> excludeUserIds) {
        return candidates.stream()
                .filter(c -> !excludeUserIds.contains(c.userId()))
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
