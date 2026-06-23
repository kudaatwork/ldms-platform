package projectlx.co.zw.shared_library.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

@Slf4j
public class PlatformWalletUsageSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
            handleChargeResponse(response, actionCode);
        } catch (InsufficientPlatformWalletBalanceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw mapChargeFailure(ex, actionCode, organizationId);
        }
    }

    /**
     * Records a usage charge when billing is reachable; skips silently when the action is not configured yet.
     */
    public void chargeBestEffort(Long organizationId, String actionCode, String referenceType, Long referenceId) {
        try {
            chargeRequired(organizationId, actionCode, referenceType, referenceId);
        } catch (InsufficientPlatformWalletBalanceException ex) {
            if (isTechnicalVerificationFailure(ex.getMessage())) {
                log.warn(
                        "Skipped optional wallet charge for org {} action {}: {}",
                        organizationId,
                        actionCode,
                        ex.getMessage());
                return;
            }
            throw ex;
        }
    }

    private void handleChargeResponse(PlatformWalletChargeResponse response, String actionCode) {
        if (response == null) {
            throw new IllegalStateException("Empty billing response for action " + actionCode);
        }
        Integer statusCode = response.getStatusCode();
        if (statusCode != null && statusCode == 402) {
            throw new InsufficientPlatformWalletBalanceException(
                    StringUtils.hasText(response.getMessage())
                            ? response.getMessage()
                            : "Insufficient prepaid wallet balance. Top up your wallet to continue.");
        }
        if (statusCode != null && statusCode >= 400) {
            throw new IllegalStateException(
                    StringUtils.hasText(response.getMessage())
                            ? response.getMessage()
                            : "Platform wallet charge failed with status " + statusCode);
        }
        if (Boolean.FALSE.equals(response.getSuccess()) || Boolean.FALSE.equals(response.getIsSuccess())) {
            throw new IllegalStateException(
                    StringUtils.hasText(response.getMessage())
                            ? response.getMessage()
                            : "Platform wallet charge was not accepted for action " + actionCode);
        }
    }

    private InsufficientPlatformWalletBalanceException mapChargeFailure(
            Exception ex,
            String actionCode,
            Long organizationId) {
        if (isInsufficientBalanceMessage(ex.getMessage())) {
            return new InsufficientPlatformWalletBalanceException(
                    "Insufficient prepaid wallet balance. Top up your wallet to continue.");
        }

        Integer httpStatus = reflectHttpStatus(ex);
        String bodyMessage = reflectResponseMessage(ex);
        if (httpStatus != null && httpStatus == 402) {
            return new InsufficientPlatformWalletBalanceException(
                    StringUtils.hasText(bodyMessage)
                            ? bodyMessage
                            : "Insufficient prepaid wallet balance. Top up your wallet to continue.");
        }
        if (httpStatus != null && httpStatus == 404) {
            log.warn("Platform action {} is not configured for org {}: {}", actionCode, organizationId, bodyMessage);
            return new InsufficientPlatformWalletBalanceException(
                    "Billing action " + actionCode + " is not configured. Contact platform support.");
        }
        if (httpStatus != null && httpStatus >= 500) {
            log.error(
                    "Billing service error {} for org {} action {}: {}",
                    httpStatus,
                    organizationId,
                    actionCode,
                    firstNonBlank(bodyMessage, ex.getMessage()));
            return new InsufficientPlatformWalletBalanceException(
                    "Billing service is temporarily unavailable. Please try again in a moment.");
        }
        if (httpStatus != null && httpStatus == 400) {
            log.warn(
                    "Platform wallet usage charge rejected for org {} action {}: {}",
                    organizationId,
                    actionCode,
                    firstNonBlank(bodyMessage, ex.getMessage()));
            return new InsufficientPlatformWalletBalanceException(
                    StringUtils.hasText(bodyMessage)
                            ? bodyMessage
                            : "Could not record platform wallet usage for this action.");
        }

        log.warn(
                "Platform wallet usage charge failed for org {} action {}: {}",
                organizationId,
                actionCode,
                firstNonBlank(bodyMessage, ex.getMessage()));
        return new InsufficientPlatformWalletBalanceException(
                StringUtils.hasText(bodyMessage)
                        ? bodyMessage
                        : "Could not verify platform wallet balance. Please try again or contact support.");
    }

    private static boolean isTechnicalVerificationFailure(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("temporarily unavailable")
                || normalized.contains("not configured")
                || normalized.contains("could not verify");
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

    private static Integer reflectHttpStatus(Throwable ex) {
        try {
            Object value = ex.getClass().getMethod("status").invoke(ex);
            if (value instanceof Integer status) {
                return status;
            }
        } catch (ReflectiveOperationException ignored) {
            // not a Feign HTTP exception
        }
        return null;
    }

    private static String reflectResponseMessage(Throwable ex) {
        try {
            Object body = ex.getClass().getMethod("contentUTF8").invoke(ex);
            if (body instanceof String json && StringUtils.hasText(json)) {
                return parseEnvelopeMessage(json);
            }
        } catch (ReflectiveOperationException ignored) {
            // not a Feign HTTP exception
        }
        return null;
    }

    private static String parseEnvelopeMessage(String json) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root == null) {
                return null;
            }
            JsonNode message = root.get("message");
            if (message != null && message.isTextual() && StringUtils.hasText(message.asText())) {
                return message.asText().trim();
            }
            JsonNode errorMessages = root.get("errorMessages");
            if (errorMessages != null && errorMessages.isArray() && !errorMessages.isEmpty()) {
                return errorMessages.get(0).asText();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public static boolean isTrackingAction(String actionCode) {
        return actionCode != null && TRACKING_ACTIONS.contains(actionCode.trim().toUpperCase());
    }
}
