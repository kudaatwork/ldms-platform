package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.logic.api.ProcurementSettingsService;
import projectlx.inventory.management.business.logic.support.ProcurementApprovalStageResolver;
import projectlx.inventory.management.model.OrganizationProcurementSetting;
import projectlx.inventory.management.model.PlatformProcurementPolicy;
import projectlx.inventory.management.repository.OrganizationProcurementSettingRepository;
import projectlx.inventory.management.repository.PlatformProcurementPolicyRepository;
import projectlx.inventory.management.utils.dtos.ProcurementApprovalPolicyDto;
import projectlx.inventory.management.utils.requests.UpdateProcurementApprovalPolicyRequest;
import projectlx.inventory.management.utils.responses.ProcurementSettingsResponse;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class ProcurementSettingsServiceImpl implements ProcurementSettingsService {

    private final PlatformProcurementPolicyRepository platformProcurementPolicyRepository;
    private final OrganizationProcurementSettingRepository organizationProcurementSettingRepository;
    private final ProcurementApprovalStageResolver procurementApprovalStageResolver;

    @Override
    @Transactional(readOnly = true)
    public ProcurementSettingsResponse getApprovalPolicy(Locale locale) {
        ProcurementSettingsResponse response = new ProcurementSettingsResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage("Procurement approval policy retrieved.");
        response.setProcurementApprovalPolicyDto(buildPolicyDto());
        return response;
    }

    @Override
    public ProcurementSettingsResponse updateApprovalPolicy(UpdateProcurementApprovalPolicyRequest request,
                                                            Locale locale,
                                                            String username) {
        if (request.getDefaultRequiredApprovalStages() != null) {
            int stages = procurementApprovalStageResolver.clampStages(request.getDefaultRequiredApprovalStages());
            PlatformProcurementPolicy policy = platformProcurementPolicyRepository
                    .findFirstByEntityStatusNotOrderByIdAsc(EntityStatus.DELETED)
                    .orElseGet(() -> {
                        PlatformProcurementPolicy created = new PlatformProcurementPolicy();
                        created.setDefaultRequiredApprovalStages(ProcurementApprovalStageResolver.DEFAULT_STAGES);
                        created.setEntityStatus(EntityStatus.ACTIVE);
                        created.setCreatedBy(username);
                        return created;
                    });
            policy.setDefaultRequiredApprovalStages(stages);
            policy.setModifiedBy(username);
            platformProcurementPolicyRepository.save(policy);
        }

        if (request.getOrganizationId() != null) {
            OrganizationProcurementSetting setting = organizationProcurementSettingRepository
                    .findByOrganizationIdAndEntityStatusNot(request.getOrganizationId(), EntityStatus.DELETED)
                    .orElseGet(() -> {
                        OrganizationProcurementSetting created = new OrganizationProcurementSetting();
                        created.setOrganizationId(request.getOrganizationId());
                        created.setEntityStatus(EntityStatus.ACTIVE);
                        created.setCreatedBy(username);
                        return created;
                    });
            if (request.getOrganizationRequiredApprovalStages() != null) {
                setting.setRequiredApprovalStages(
                        procurementApprovalStageResolver.clampStages(request.getOrganizationRequiredApprovalStages()));
            } else {
                setting.setRequiredApprovalStages(null);
            }
            setting.setModifiedBy(username);
            organizationProcurementSettingRepository.save(setting);
        }

        ProcurementSettingsResponse response = new ProcurementSettingsResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage("Procurement approval policy updated.");
        response.setProcurementApprovalPolicyDto(buildPolicyDto());
        return response;
    }

    private ProcurementApprovalPolicyDto buildPolicyDto() {
        ProcurementApprovalPolicyDto dto = new ProcurementApprovalPolicyDto();
        dto.setDefaultRequiredApprovalStages(procurementApprovalStageResolver.loadPlatformDefault());
        dto.setMinAllowedStages(ProcurementApprovalStageResolver.MIN_STAGES);
        dto.setMaxAllowedStages(ProcurementApprovalStageResolver.MAX_STAGES);
        return dto;
    }
}
