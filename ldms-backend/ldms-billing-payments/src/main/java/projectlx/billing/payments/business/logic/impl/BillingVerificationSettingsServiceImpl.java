package projectlx.billing.payments.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import projectlx.billing.payments.business.auditable.api.OrganizationBillingSettingServiceAuditable;
import projectlx.billing.payments.business.logic.api.BillingVerificationSettingsService;
import projectlx.billing.payments.business.logic.support.BillingVerificationStageResolver;
import projectlx.billing.payments.business.logic.support.OrganizationNameResolver;
import projectlx.billing.payments.business.logic.support.CallerOrganizationResolver;
import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.repository.OrganizationBillingSettingRepository;
import projectlx.billing.payments.utils.dtos.BillingVerificationPolicyDto;
import projectlx.billing.payments.utils.requests.UpdateBillingVerificationPolicyRequest;
import projectlx.billing.payments.utils.responses.BillingVerificationSettingsResponse;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class BillingVerificationSettingsServiceImpl implements BillingVerificationSettingsService {

    private final OrganizationBillingSettingRepository organizationBillingSettingRepository;
    private final OrganizationBillingSettingServiceAuditable organizationBillingSettingServiceAuditable;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final BillingVerificationStageResolver billingVerificationStageResolver;
    private final OrganizationNameResolver organizationNameResolver;

    @Override
    @Transactional(readOnly = true)
    public BillingVerificationSettingsResponse getVerificationPolicy(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400, "Organisation context is required.");
        }
        BillingVerificationSettingsResponse response = success(200, "Billing verification policy retrieved.");
        response.setBillingVerificationPolicyDto(buildPolicyDto(organizationId));
        return response;
    }

    @Override
    public BillingVerificationSettingsResponse updateVerificationPolicy(
            UpdateBillingVerificationPolicyRequest request, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400, "Organisation context is required.");
        }
        if (request == null || request.getDefaultRequiredVerificationStages() == null) {
            return error(400, "Required verification stages must be provided.", List.of("defaultRequiredVerificationStages is required"));
        }

        int stages = billingVerificationStageResolver.clampStages(request.getDefaultRequiredVerificationStages());
        LocalDateTime now = LocalDateTime.now();
        OrganizationBillingSetting setting = organizationBillingSettingRepository
                .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                .orElseGet(() -> {
                    OrganizationBillingSetting created = new OrganizationBillingSetting();
                    created.setOrganizationId(organizationId);
                    created.setOrganizationName(organizationNameResolver.resolve(organizationId));
                    created.setEntityStatus(EntityStatus.ACTIVE);
                    created.setCreatedAt(now);
                    created.setCreatedBy(username);
                    return created;
                });
        setting.setRequiredPaymentVerificationStages(stages);
        setting.setModifiedAt(now);
        setting.setModifiedBy(username);
        if (setting.getId() == null) {
            organizationBillingSettingServiceAuditable.create(setting, locale, username);
        } else {
            organizationBillingSettingServiceAuditable.update(setting, locale, username);
        }

        BillingVerificationSettingsResponse response = success(200, "Billing verification policy updated.");
        response.setBillingVerificationPolicyDto(buildPolicyDto(organizationId));
        return response;
    }

    private BillingVerificationPolicyDto buildPolicyDto(Long organizationId) {
        BillingVerificationPolicyDto dto = new BillingVerificationPolicyDto();
        dto.setDefaultRequiredVerificationStages(billingVerificationStageResolver.resolveRequiredStages(organizationId));
        dto.setMinAllowedStages(BillingVerificationStageResolver.MIN_STAGES);
        dto.setMaxAllowedStages(BillingVerificationStageResolver.MAX_STAGES);
        return dto;
    }

    private BillingVerificationSettingsResponse success(int statusCode, String message) {
        BillingVerificationSettingsResponse response = new BillingVerificationSettingsResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private BillingVerificationSettingsResponse error(int statusCode, String message) {
        return error(statusCode, message, List.of());
    }

    private BillingVerificationSettingsResponse error(int statusCode, String message, List<String> errorMessages) {
        BillingVerificationSettingsResponse response = new BillingVerificationSettingsResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errorMessages);
        return response;
    }
}
