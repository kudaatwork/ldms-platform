package projectlx.co.zw.organizationmanagement.business.kyc;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.PlatformKycPolicy;
import projectlx.co.zw.organizationmanagement.repository.PlatformKycPolicyRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

/**
 * Resolves how many distinct KYC approval stages apply (platform default or per-organisation override).
 */
@Service
@RequiredArgsConstructor
public class KycApprovalStageResolver {

    public static final int MIN_STAGES = 1;
    public static final int MAX_STAGES = 5;
    public static final int DEFAULT_STAGES = 2;

    private final PlatformKycPolicyRepository platformKycPolicyRepository;

    public int resolveRequiredStages(Organization organization) {
        if (organization != null && organization.getKycRequiredApprovalStages() != null) {
            return clampStages(organization.getKycRequiredApprovalStages());
        }
        return clampStages(loadPlatformDefault());
    }

    public int loadPlatformDefault() {
        return platformKycPolicyRepository
                .findFirstByEntityStatusNotOrderByIdAsc(EntityStatus.DELETED)
                .map(PlatformKycPolicy::getDefaultRequiredApprovalStages)
                .map(this::clampStages)
                .orElse(DEFAULT_STAGES);
    }

    public int clampStages(int stages) {
        if (stages < MIN_STAGES) {
            return MIN_STAGES;
        }
        return Math.min(stages, MAX_STAGES);
    }
}
