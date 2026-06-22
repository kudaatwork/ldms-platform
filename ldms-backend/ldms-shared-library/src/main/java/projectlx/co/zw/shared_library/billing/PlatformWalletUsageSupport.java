package projectlx.co.zw.shared_library.billing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

@Slf4j
public class PlatformWalletUsageSupport {

    private static final Set<String> TRACKING_ACTIONS = Set.of(
            PlatformWalletActionCodes.TRIP_TRACK,
            PlatformWalletActionCodes.GPS_PING,
            PlatformWalletActionCodes.LIVE_MAP_SESSION);

    private final PlatformWalletBillingClient billingClient;
    private final String serviceName;

    public PlatformWalletUsageSupport(PlatformWalletBillingClient billingClient, String serviceName) {
        this.billingClient = billingClient;
        this.serviceName = serviceName;
    }

    public void chargeRequired(Long organizationId, String actionCode, String referenceType, Long referenceId) {
        chargeRequired(organizationId, actionCode, referenceType, referenceId, null);
    }

    public void chargeRequired(
            Long organizationId,
            String actionCode,
            String referenceType,
            Long referenceId,
            Long tripId) {
        if (organizationId == null || organizationId <= 0L || !StringUtils.hasText(actionCode)) {
            return;
        }
        RecordPlatformUsageChargeRequest request = new RecordPlatformUsageChargeRequest();
        request.setOrganizationId(organizationId);
        request.setActionCode(actionCode.trim().toUpperCase());
        request.setReferenceType(referenceType);
        request.setReferenceId(referenceId);
        request.setTripId(tripId);
        request.setServiceName(serviceName);
        try {
            PlatformWalletChargeResponse response = billingClient.recordUsageCharge(request, Locale.getDefault());
            if (response != null && response.getStatusCode() != null && response.getStatusCode() == 402) {
                throw new InsufficientPlatformWalletBalanceException(
                        StringUtils.hasText(response.getMessage())
                                ? response.getMessage()
                                : "Insufficient prepaid wallet balance. Top up your wallet to continue.");
            }
        } catch (InsufficientPlatformWalletBalanceException ex) {
            throw ex;
        } catch (Exception ex) {
            if (isInsufficientBalanceMessage(ex.getMessage())) {
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
        }
    }

    private static boolean isInsufficientBalanceMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("402")
                || normalized.contains("insufficient")
                || normalized.contains("payment required");
    }

    public static boolean isTrackingAction(String actionCode) {
        return actionCode != null && TRACKING_ACTIONS.contains(actionCode.trim().toUpperCase());
    }
}
