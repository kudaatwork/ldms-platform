package projectlx.billing.payments.business.logic.support;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.repository.OrganizationBillingSettingRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@RequiredArgsConstructor
public class BillingVerificationStageResolver {

    public static final int MIN_STAGES = 1;
    public static final int MAX_STAGES = 3;
    public static final int DEFAULT_STAGES = 1;

    private final OrganizationBillingSettingRepository organizationBillingSettingRepository;

    public int resolveRequiredStages(Long supplierOrganizationId) {
        if (supplierOrganizationId == null || supplierOrganizationId <= 0) {
            return DEFAULT_STAGES;
        }
        return organizationBillingSettingRepository
                .findByOrganizationIdAndEntityStatusNot(supplierOrganizationId, EntityStatus.DELETED)
                .map(OrganizationBillingSetting::getRequiredPaymentVerificationStages)
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
