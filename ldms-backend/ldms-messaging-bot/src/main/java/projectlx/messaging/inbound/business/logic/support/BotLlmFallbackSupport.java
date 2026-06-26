package projectlx.messaging.inbound.business.logic.support;

import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;

import java.util.List;
import java.util.Locale;

/** Keyword fallbacks when no LLM provider is configured or the remote call fails. */
public final class BotLlmFallbackSupport {

    private BotLlmFallbackSupport() {
    }

    public static String replyFor(String userMessage) {
        return replyFor(userMessage, null, true);
    }

    public static String replyFor(String userMessage, List<BotMessage> history) {
        return replyFor(userMessage, history, true);
    }

    public static String replyFor(String userMessage, List<BotMessage> history, boolean llmConfigured) {
        String effective = history == null || history.isEmpty()
                ? userMessage
                : BotLlmHistorySupport.effectiveUserQuery(history);
        if (effective == null || effective.isBlank()) {
            effective = userMessage == null ? "" : userMessage;
        }
        String lower = effective.toLowerCase(Locale.ROOT);

        if (isMetaQuestion(lower)) {
            return metaAssistantReply(llmConfigured);
        }
        if (matchesAccuracyChallenge(lower)) {
            return accuracyChallengeReply();
        }

        return keywordReplyFor(effective, history, userMessage, llmConfigured);
    }

    public static String keywordReplyFor(String effective, List<BotMessage> history, String userMessage) {
        return keywordReplyFor(effective, history, userMessage, true);
    }

    public static String keywordReplyFor(String effective, List<BotMessage> history,
                                         String userMessage, boolean llmConfigured) {
        if (effective == null || effective.isBlank()) {
            effective = userMessage == null ? "" : userMessage;
        }
        String lower = effective.toLowerCase(Locale.ROOT);

        if (isMetaQuestion(lower)) {
            return metaAssistantReply(llmConfigured);
        }
        if (matchesAccuracyChallenge(lower)) {
            return accuracyChallengeReply();
        }

        String keywordReply = keywordReply(lower, history, effective, userMessage);
        if (!llmConfigured) {
            return guideModePrefix() + keywordReply;
        }
        return keywordReply;
    }

    private static boolean matchesAccuracyChallenge(String lower) {
        return lower.contains("hallucin")
                || lower.contains("aren't you")
                || lower.contains("arent you")
                || lower.contains("making that up")
                || lower.contains("made that up")
                || lower.contains("are you sure")
                || lower.contains("is that true")
                || lower.contains("is that correct")
                || lower.matches(".*\\b(not true|wrong|incorrect)\\b.*");
    }

    public static boolean isAccuracyChallengeQuery(String text) {
        return text != null && !text.isBlank() && matchesAccuracyChallenge(text.toLowerCase(Locale.ROOT));
    }

    private static String accuracyChallengeReply() {
        return LexiBotPersonality.accuracyChallengeReply();
    }

    public static String accuracyChallengeReplyFor() {
        return LexiBotPersonality.accuracyChallengeReply();
    }

    private static boolean isMetaQuestion(String lower) {
        return lower.matches(".*\\b(what is this|what's this|what is that|what's that|what are you|who are you|who is lexi|what is lexi|"
                + "what is ldms|what's ldms|what do you do|how do you work|what can you do)\\b.*")
                || lower.matches("^(hi|hello|hey)[.!?]*$");
    }

    private static String metaAssistantReply(boolean llmConfigured) {
        return LexiBotPersonality.metaReply(llmConfigured);
    }

    private static String guideModePrefix() {
        return LexiBotPersonality.guideModePrefix();
    }

    private static String keywordReply(String lower, List<BotMessage> history, String effective, String userMessage) {
        if (matchesUserGroupTopic(lower)) {
            return """
                    To **create a user group** in LDMS on the platform portal:
                    
                    1. Open **User management → User groups** (`/users/groups`).
                    2. Click **Create group** and enter the name (e.g. Operations Management).
                    3. Set the organisation **classification** scope and assign **roles** for the group.
                    4. Save, then use **Assign users** on the group row to add members.
                    
                    I can't create groups directly in chat — head to the portal UI. For permissions issues, open **Help & Support → New ticket**.""";
        }
        if (matchesOrderTopic(lower)) {
            return """
                    To **create a purchase order or requisition** in LDMS:
                    
                    - **Customer:** open **My Orders** → **Create order** / **New requisition**, add lines and submit.
                    - **Supplier:** open **Inventory → Orders workspace** to review incoming POs and approve them.
                    
                    After approval, stock is reserved and dispatch becomes a **shipment** you can track under **Shipment Management**.
                    For account-specific PO status, open **Help & Support → New ticket** with your PO reference.""";
        }
        if (lower.contains("shipment") || lower.contains("dispatch")) {
            return "In LDMS, shipments are created after a purchase order is approved and stock is dispatched. "
                    + "Track them from **Track shipments** or your operations dashboard. "
                    + "For a specific reference, open Help & Support → New ticket with the shipment number.";
        }
        if (lower.contains("trip") || lower.contains("track") || lower.contains("gps")) {
            return "Trips start when a truck and driver are assigned to a shipment. Live GPS and stop events "
                    + "are recorded in **Trip & Tracking**. Drivers can also report border stops via WhatsApp commands.";
        }
        if (lower.contains("invoice") || lower.contains("billing") || lower.contains("payment")
                || lower.contains("charge") || lower.contains("wallet") || lower.contains("report")) {
            return """
                    LDMS has two billing layers:
                    
                    **Customer invoicing (GRV → invoice)**
                    - Invoices are generated after goods are received (GRV).
                    - View them under **Billing** on the platform portal.
                    - For disputes, open a **Billing** support ticket with the invoice number.
                    
                    **Platform billing charges (prepaid wallet)**
                    - Organisations pay small per-action fees from a prepaid wallet (trips, shipments, bot messages, notifications, etc.).
                    - **Report export** and analytics exports are billable actions — a strong recurring revenue source for the platform.
                    - Admins configure prices in **LX Admin → Settings → Platform billing → Action charges**.
                    - Organisations review usage under **Settings → Platform billing** on the platform portal.""";
        }
        if (lower.contains("kyc") || lower.contains("onboard") || lower.contains("register")) {
            return "Organisation onboarding includes registration, email verification, and KYC review. "
                    + "After approval, your contact person receives sign-in credentials. "
                    + "See Help & Support → FAQ → Getting started for the full flow.";
        }
        if (lower.contains("grv") || lower.contains("deliver")) {
            return "A GRV (Goods Received Voucher) confirms delivery at the destination. Receivers scan QR codes "
                    + "or confirm in the Receiver app. This triggers invoicing in the billing phase.";
        }
        if (history != null && !history.isEmpty()
                && isVagueContinuation(userMessage, history)
                && effective.equalsIgnoreCase(userMessage == null ? "" : userMessage.trim())) {
            String priorTopic = priorSubstantiveUserTopic(history);
            if (!priorTopic.isBlank() && matchesOrderTopic(priorTopic.toLowerCase(Locale.ROOT))) {
                return keywordReply(priorTopic.toLowerCase(Locale.ROOT), history, priorTopic, priorTopic);
            }
            return "Sure — could you repeat the LDMS topic you want help with (onboarding, orders, shipments, "
                    + "trips, billing, or Help & Support)? I'll pick up from there.";
        }
        return LexiBotPersonality.genericFallback();
    }

    private static boolean matchesOrderTopic(String lower) {
        return lower.contains("requis")
                || lower.contains("purchase order")
                || lower.contains("procurement")
                || lower.matches(".*\\bpo\\b.*")
                || lower.contains("create order")
                || lower.contains("new order")
                || lower.contains("my orders");
    }

    private static boolean matchesUserGroupTopic(String lower) {
        return lower.contains("user group")
                || lower.contains("user groups")
                || ((lower.contains("create") || lower.contains("new")) && lower.contains("group"));
    }

    private static boolean isVagueContinuation(String userMessage, List<BotMessage> history) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        String lower = userMessage.toLowerCase(Locale.ROOT).trim();
        if (lower.matches("^(yes|yeah|yep|ok|okay|sure|please|thanks|thank you|go ahead|continue|proceed|"
                + "well,? you can( just)? go ahead|carry on|do it|tell me more|go on|what|waht|huh|sorry|"
                + "pardon|repeat|again)[.!?]*$")) {
            return true;
        }
        return lower.length() <= 5 && priorSubstantiveUserTopic(history) != null
                && !priorSubstantiveUserTopic(history).isBlank();
    }

    private static String priorSubstantiveUserTopic(List<BotMessage> history) {
        if (history == null) {
            return "";
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            BotMessage message = history.get(i);
            if (message.getRole() != BotMessageRole.USER || message.getBody() == null) {
                continue;
            }
            String body = message.getBody().trim();
            if (body.isBlank() || BotLlmHistorySupport.isContinuation(body)) {
                continue;
            }
            return body;
        }
        return "";
    }

    public static String lastUserMessage(List<BotMessage> history) {
        if (history == null) {
            return "";
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            BotMessage message = history.get(i);
            if (message.getRole() == BotMessageRole.USER && message.getBody() != null) {
                return message.getBody();
            }
        }
        return "";
    }
}
