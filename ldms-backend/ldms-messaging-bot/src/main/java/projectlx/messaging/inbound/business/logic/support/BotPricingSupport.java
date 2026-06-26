package projectlx.messaging.inbound.business.logic.support;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.shared_library.billing.PlatformWalletActionCodes;
import projectlx.messaging.inbound.clients.BillingPaymentsServiceClient;
import projectlx.messaging.inbound.utils.dtos.BotPricingDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Resolves Help &amp; Support and Lexi costs from the live platform action charge catalog
 * ({@code ldms-billing-payments} pricing catalog).
 */
@Slf4j
public class BotPricingSupport {

    private static final long CACHE_TTL_MS = 60_000L;
    private static final String DEFAULT_CURRENCY = "USD";

    private final BillingPaymentsServiceClient billingPaymentsServiceClient;

    private volatile BotPricingDto cached;
    private volatile long cachedAtMs;

    public BotPricingSupport(BillingPaymentsServiceClient billingPaymentsServiceClient) {
        this.billingPaymentsServiceClient = billingPaymentsServiceClient;
    }

    public BotPricingDto currentPricing() {
        if (cached != null && System.currentTimeMillis() - cachedAtMs < CACHE_TTL_MS) {
            return cached;
        }
        return refreshPricing();
    }

    public synchronized BotPricingDto refreshPricing() {
        BotPricingDto pricing = loadFromCatalog();
        cached = pricing;
        cachedAtMs = System.currentTimeMillis();
        return pricing;
    }

    /** System-prompt block so Lexi quotes live catalog prices, not hardcoded amounts. */
    public String promptBlock() {
        BotPricingDto pricing = currentPricing();
        return """
                Live platform pricing (from action charge catalog — quote these amounts only):
                - Assistant message (%s): %s
                - Agent message (%s): %s
                - New Lexi chat session (%s): %s
                - Open support ticket (%s): %s
                - Live support ticket message (%s): %s
                When users ask what Lexi or Help & Support costs, use get_pricing_catalog or these figures. Never invent prices."""
                .formatted(
                        PlatformWalletActionCodes.HELP_BOT_MESSAGE,
                        formatMoney(pricing.getAssistantMessageCents(), pricing.getCurrencyCode()),
                        PlatformWalletActionCodes.HELP_BOT_AGENT_MESSAGE,
                        formatMoney(pricing.getAgentMessageCents(), pricing.getCurrencyCode()),
                        PlatformWalletActionCodes.BOT_SESSION_START,
                        formatMoney(pricing.getSessionStartCents(), pricing.getCurrencyCode()),
                        PlatformWalletActionCodes.HELP_SUPPORT_TICKET_OPEN,
                        formatEventMoney(pricing.getSupportTicketOpenCents(), pricing.getCurrencyCode()),
                        PlatformWalletActionCodes.HELP_LIVE_CHAT_MESSAGE,
                        formatMoney(pricing.getLiveChatMessageCents(), pricing.getCurrencyCode()));
    }

    public static String formatMoney(long cents, String currencyCode) {
        if (cents <= 0L) {
            return "Included (no wallet charge)";
        }
        String currency = currencyCode == null || currencyCode.isBlank() ? DEFAULT_CURRENCY : currencyCode.trim();
        BigDecimal amount = BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.HALF_UP);
        return currency + " " + amount.toPlainString() + " per message";
    }

    private static String formatEventMoney(long cents, String currencyCode) {
        if (cents <= 0L) {
            return "Included (no wallet charge)";
        }
        String currency = currencyCode == null || currencyCode.isBlank() ? DEFAULT_CURRENCY : currencyCode.trim();
        BigDecimal amount = BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.HALF_UP);
        return currency + " " + amount.toPlainString() + " per ticket";
    }

    private BotPricingDto loadFromCatalog() {
        BotPricingDto fallback = fallbackPricing();
        try {
            JsonNode root = billingPaymentsServiceClient.getPublicPricingCatalog(Locale.ENGLISH);
            if (root == null || !root.path("success").asBoolean(false)) {
                log.warn("Help & Support pricing catalog unavailable — using fallback amounts");
                return fallback;
            }
            JsonNode charges = root.path("platformActionChargeDtoList");
            if (!charges.isArray() || charges.isEmpty()) {
                log.warn("Help & Support pricing catalog empty — using fallback amounts");
                return fallback;
            }
            BotPricingDto pricing = new BotPricingDto();
            pricing.setCurrencyCode(DEFAULT_CURRENCY);
            pricing.setAssistantMessageCents(resolveCents(charges, PlatformWalletActionCodes.HELP_BOT_MESSAGE, 0L));
            pricing.setAgentMessageCents(resolveCents(charges, PlatformWalletActionCodes.HELP_BOT_AGENT_MESSAGE, 115L));
            pricing.setSessionStartCents(resolveCents(charges, PlatformWalletActionCodes.BOT_SESSION_START, 0L));
            pricing.setSupportTicketOpenCents(resolveCents(charges, PlatformWalletActionCodes.HELP_SUPPORT_TICKET_OPEN, 0L));
            pricing.setLiveChatMessageCents(resolveCents(charges, PlatformWalletActionCodes.HELP_LIVE_CHAT_MESSAGE, 0L));
            return pricing;
        } catch (Exception ex) {
            log.warn("Could not load Help & Support pricing catalog: {}", ex.getMessage());
            return fallback;
        }
    }

    private static long resolveCents(JsonNode charges, String actionCode, long defaultCents) {
        for (JsonNode charge : charges) {
            if (actionCode.equalsIgnoreCase(charge.path("actionCode").asText(""))) {
                if (charge.path("active").asBoolean(true)) {
                    return charge.path("chargeCents").asLong(defaultCents);
                }
                return defaultCents;
            }
        }
        return defaultCents;
    }

    private static BotPricingDto fallbackPricing() {
        BotPricingDto pricing = new BotPricingDto();
        pricing.setCurrencyCode(DEFAULT_CURRENCY);
        pricing.setAssistantMessageCents(0L);
        pricing.setAgentMessageCents(115L);
        pricing.setSessionStartCents(0L);
        pricing.setSupportTicketOpenCents(0L);
        pricing.setLiveChatMessageCents(0L);
        return pricing;
    }
}
