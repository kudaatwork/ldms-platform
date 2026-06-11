package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import projectlx.inventory.management.model.OrganizationProcurementSetting;
import projectlx.inventory.management.model.PlatformProcurementPolicy;
import projectlx.inventory.management.repository.OrganizationProcurementSettingRepository;
import projectlx.inventory.management.repository.PlatformProcurementPolicyRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Component
@RequiredArgsConstructor
public class ProcurementApprovalStageResolver {

    public static final int MIN_STAGES = 1;
    public static final int MAX_STAGES = 3;
    public static final int DEFAULT_STAGES = 1;

    private final PlatformProcurementPolicyRepository platformProcurementPolicyRepository;
    private final OrganizationProcurementSettingRepository organizationProcurementSettingRepository;

    public int resolveRequiredStages(Long organizationId) {
        if (organizationId != null) {
            return organizationProcurementSettingRepository
                    .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                    .map(OrganizationProcurementSetting::getRequiredApprovalStages)
                    .map(this::clampStages)
                    .orElseGet(this::loadPlatformDefault);
        }
        return loadPlatformDefault();
    }

    public int loadPlatformDefault() {
        return platformProcurementPolicyRepository
                .findFirstByEntityStatusNotOrderByIdAsc(EntityStatus.DELETED)
                .map(PlatformProcurementPolicy::getDefaultRequiredApprovalStages)
                .map(this::clampStages)
                .orElse(DEFAULT_STAGES);
    }

    public int clampStages(Integer stages) {
        if (stages == null) {
            return DEFAULT_STAGES;
        }
        return Math.max(MIN_STAGES, Math.min(MAX_STAGES, stages));
    }
}
