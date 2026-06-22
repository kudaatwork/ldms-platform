package projectlx.inventory.management.business.logic.support;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.inventory.management.clients.BillingPaymentsServiceClient;
import projectlx.inventory.management.clients.dto.BillingPlatformWalletResponse;
import projectlx.inventory.management.clients.dto.RecordPlatformUsageChargeRequest;
import projectlx.inventory.management.exceptions.InsufficientPlatformWalletBalanceException;

import java.util.Locale;

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

    public void chargeRequired(
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
            BillingPlatformWalletResponse response = billingPaymentsServiceClient.recordUsageCharge(request, Locale.getDefault());
            if (response != null && response.getStatusCode() != null && response.getStatusCode() == 402) {
                throw new InsufficientPlatformWalletBalanceException(
                        StringUtils.hasText(response.getMessage())
                                ? response.getMessage()
                                : "Insufficient prepaid wallet balance. Top up your wallet to continue.");
            }
        } catch (FeignException ex) {
            if (ex.status() == 402) {
                throw new InsufficientPlatformWalletBalanceException(
                        "Insufficient prepaid wallet balance. Top up your wallet to continue.");
            }
            log.warn(
                    "Platform wallet usage charge failed for org {} action {} ref {}/{}: {}",
                    organizationId,
                    actionCode,
                    referenceType,
                    referenceId,
                    ex.getMessage());
            throw new InsufficientPlatformWalletBalanceException(
                    "Could not verify platform wallet balance. Please try again or contact support.");
        } catch (InsufficientPlatformWalletBalanceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn(
                    "Platform wallet usage charge skipped for org {} action {} ref {}/{}: {}",
                    organizationId,
                    actionCode,
                    referenceType,
                    referenceId,
                    ex.getMessage());
            throw new InsufficientPlatformWalletBalanceException(
                    "Could not verify platform wallet balance. Please try again or contact support.");
        }
    }

    /** @deprecated use {@link #chargeRequired(Long, String, String, Long)} */
    @Deprecated
    public void chargeBestEffort(
            Long organizationId,
            String actionCode,
            String referenceType,
            Long referenceId) {
        chargeRequired(organizationId, actionCode, referenceType, referenceId);
    }
}
