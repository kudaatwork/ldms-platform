package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.inventory.management.clients.BillingPaymentsServiceClient;
import projectlx.inventory.management.clients.dto.BillingCurrencyContextResponse;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationFunctionalCurrencySupport {

    private static final String DEFAULT_CURRENCY = "USD";

    private final BillingPaymentsServiceClient billingPaymentsServiceClient;

    public String resolveFunctionalCurrency(Long organizationId) {
        if (organizationId == null || organizationId < 1) {
            return DEFAULT_CURRENCY;
        }
        try {
            BillingCurrencyContextResponse response = billingPaymentsServiceClient
                    .getOrganizationCurrencyContext(organizationId, Locale.ENGLISH);
            if (response != null && response.isSuccess() && response.getOrganizationCurrencyContextDto() != null
                    && StringUtils.hasText(response.getOrganizationCurrencyContextDto().getFunctionalCurrencyCode())) {
                return response.getOrganizationCurrencyContextDto().getFunctionalCurrencyCode().trim().toUpperCase();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve functional currency for org {} from billing: {}", organizationId, ex.getMessage());
        }
        return DEFAULT_CURRENCY;
    }
}
