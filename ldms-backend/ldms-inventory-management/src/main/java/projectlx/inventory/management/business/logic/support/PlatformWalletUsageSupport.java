package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.inventory.management.clients.BillingPaymentsServiceClient;
import projectlx.inventory.management.clients.dto.RecordPlatformUsageChargeRequest;

import java.util.Locale;

/**
 * Best-effort platform wallet usage charges for procurement workflow events.
 * Failures are logged and never roll back the calling business transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformWalletUsageSupport {

    public static final String ACTION_PROCUREMENT_PR_APPROVE = "PROCUREMENT_PR_APPROVE";
    public static final String ACTION_PROCUREMENT_QUOTE_SUBMIT = "PROCUREMENT_QUOTE_SUBMIT";
    public static final String ACTION_PROCUREMENT_PO_CUSTOMER_APPROVE = "PROCUREMENT_PO_CUSTOMER_APPROVE";
    public static final String ACTION_PROCUREMENT_PO_SUPPLIER_APPROVE = "PROCUREMENT_PO_SUPPLIER_APPROVE";
    public static final String ACTION_PROCUREMENT_SO_APPROVE = "PROCUREMENT_SO_APPROVE";

    private final BillingPaymentsServiceClient billingPaymentsServiceClient;

    public void chargeBestEffort(
            Long organizationId,
            String actionCode,
            String referenceType,
            Long referenceId) {
        if (organizationId == null || organizationId <= 0L || !StringUtils.hasText(actionCode)) {
            return;
        }
        RecordPlatformUsageChargeRequest request = new RecordPlatformUsageChargeRequest();
        request.setOrganizationId(organizationId);
        request.setActionCode(actionCode.trim().toUpperCase());
        request.setReferenceType(referenceType);
        request.setReferenceId(referenceId);
        request.setServiceName("ldms-inventory-management");
        try {
            billingPaymentsServiceClient.recordUsageCharge(request, Locale.getDefault());
        } catch (Exception ex) {
            log.warn(
                    "Platform wallet usage charge skipped for org {} action {} ref {}/{}: {}",
                    organizationId,
                    actionCode,
                    referenceType,
                    referenceId,
                    ex.getMessage());
        }
    }
}
