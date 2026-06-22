package projectlx.billing.payments.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import projectlx.billing.payments.business.logic.api.PlatformWalletBillingService;
import projectlx.billing.payments.utils.requests.CreditOrganizationWalletRequest;

import java.util.Locale;

/**
 * Optional dev bootstrap: set {@code ldms.billing.bootstrap.ecogas-organization-id} to credit Ecogas on startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcogasWalletBootstrapRunner implements ApplicationRunner {

    private final Environment environment;
    private final PlatformWalletBillingService platformWalletBillingService;

    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.parseBoolean(environment.getProperty("ldms.billing.bootstrap.ecogas-enabled", "false"))) {
            return;
        }
        String orgIdRaw = environment.getProperty("ldms.billing.bootstrap.ecogas-organization-id");
        if (orgIdRaw == null || orgIdRaw.isBlank()) {
            log.info("Ecogas wallet bootstrap skipped — ldms.billing.bootstrap.ecogas-organization-id is not set");
            return;
        }
        long organizationId;
        try {
            organizationId = Long.parseLong(orgIdRaw.trim());
        } catch (NumberFormatException ex) {
            log.warn("Ecogas wallet bootstrap skipped — invalid organisation id {}", orgIdRaw);
            return;
        }
        long amountCents = Long.parseLong(environment.getProperty("ldms.billing.bootstrap.ecogas-credit-cents", "100000"));
        String orgName = environment.getProperty("ldms.billing.bootstrap.ecogas-organization-name", "Ecogas");

        CreditOrganizationWalletRequest request = new CreditOrganizationWalletRequest();
        request.setOrganizationId(organizationId);
        request.setOrganizationName(orgName);
        request.setAmountCents(amountCents);
        request.setCurrencyCode("USD");
        request.setNotes("Ecogas startup wallet bootstrap");
        request.setEnablePrepaidBilling(true);

        try {
            platformWalletBillingService.creditOrganizationWallet(request, Locale.getDefault(), "SYSTEM_BOOTSTRAP");
            log.info("Ecogas wallet bootstrap credited {} cents to organisation {}", amountCents, organizationId);
        } catch (Exception ex) {
            log.warn("Ecogas wallet bootstrap failed for organisation {}: {}", organizationId, ex.getMessage());
        }
    }
}
