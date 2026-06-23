package projectlx.co.zw.organizationmanagement.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;
import java.util.Map;

public interface BillingPaymentsServiceClient {

    @GetMapping("/ldms-billing-payments/v1/system/platform-wallet/organization/{organizationId}/fuel-consumption-available")
    Map<String, Object> isFuelConsumptionAvailableForOrganization(
            @PathVariable("organizationId") Long organizationId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
