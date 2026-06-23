package projectlx.co.zw.notifications.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import projectlx.co.zw.notifications.clients.BillingPaymentsServiceClient;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;
import projectlx.co.zw.shared_library.billing.PlatformWalletChargeResponse;
import projectlx.co.zw.shared_library.billing.RecordPlatformUsageChargeRequest;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class NotificationBillingSupport {

    private final BillingPaymentsServiceClient billingPaymentsServiceClient;

    public NotificationBillingSupport(BillingPaymentsServiceClient billingPaymentsServiceClient) {
        this.billingPaymentsServiceClient = billingPaymentsServiceClient;
    }

    /**
     * Records a platform wallet / subscription SMS charge before dispatch.
     *
     * @return true when the message may be sent; false when billing blocked the send (quota + empty wallet).
     */
    public boolean authorizeMessagingCharge(NotificationRequest request, String actionCode) {
        Long organizationId = resolveBillingOrganizationId(request);
        if (organizationId == null || organizationId < 1L) {
            return true;
        }

        RecordPlatformUsageChargeRequest chargeRequest = new RecordPlatformUsageChargeRequest();
        chargeRequest.setOrganizationId(organizationId);
        chargeRequest.setActionCode(actionCode);
        chargeRequest.setReferenceType("NOTIFICATION");
        chargeRequest.setServiceName("ldms-notifications");
        chargeRequest.setTraceId(request.getMetadata() != null ? request.getMetadata().getTraceId() : null);

        try {
            PlatformWalletChargeResponse response = billingPaymentsServiceClient.recordUsageCharge(
                    chargeRequest, Locale.getDefault());
            if (response == null) {
                return true;
            }
            Integer status = response.getStatusCode();
            if (status != null && status == 402) {
                log.warn(
                        "Blocked outbound message org={} action={} template={}: {}",
                        organizationId,
                        actionCode,
                        request.getTemplateKey(),
                        response.getMessage());
                return false;
            }
            if (Boolean.FALSE.equals(response.getSuccess()) || Boolean.FALSE.equals(response.getIsSuccess())) {
                log.warn(
                        "Billing rejected outbound message org={} action={} template={}: {}",
                        organizationId,
                        actionCode,
                        request.getTemplateKey(),
                        response.getMessage());
                return status != null && status == 402 ? false : true;
            }
            return true;
        } catch (Exception ex) {
            if (isPaymentRequired(ex)) {
                log.warn(
                        "Blocked outbound message org={} action={} template={} billing=402",
                        organizationId,
                        actionCode,
                        request.getTemplateKey());
                return false;
            }
            log.warn(
                    "Billing check failed for org={} action={} template={}: {} — allowing send",
                    organizationId,
                    actionCode,
                    request.getTemplateKey(),
                    ex.getMessage());
            return true;
        }
    }

    public static Long resolveBillingOrganizationId(NotificationRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getMetadata() != null && request.getMetadata().getBillingOrganizationId() != null) {
            return request.getMetadata().getBillingOrganizationId();
        }
        Map<String, Object> data = request.getData();
        if (data == null || data.isEmpty()) {
            return null;
        }
        Long fromBilling = readLong(data.get("billingOrganizationId"));
        if (fromBilling != null) {
            return fromBilling;
        }
        Long fromOrg = readLong(data.get("organizationId"));
        if (fromOrg != null) {
            return fromOrg;
        }
        return readLong(data.get("supplierOrganizationId"));
    }

    private static Long readLong(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            long value = number.longValue();
            return value > 0L ? value : null;
        }
        String text = String.valueOf(raw).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            long value = Long.parseLong(text);
            return value > 0L ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean isPaymentRequired(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase(Locale.ROOT) : "";
        return message.contains("402") || message.contains("payment required") || message.contains("sms quota");
    }
}
