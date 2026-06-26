package projectlx.messaging.inbound.business.logic.support;

import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Cleans conversation history and resolves what the user is really asking. */
public final class BotLlmHistorySupport {

    private static final Pattern LEGACY_GEMINI_NAG = Pattern.compile(
            "\\s*\\(Configure GEMINI_API_KEY for full AI answers\\.\\)\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_API_KEY_NAG = Pattern.compile(
            "\\s*\\(Configure [A-Z_]+_API_KEY[^)]*\\)\\s*",
            Pattern.CASE_INSENSITIVE);

    private BotLlmHistorySupport() {
    }

    public static List<BotMessage> sanitizeForLlm(List<BotMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<BotMessage> sanitized = new ArrayList<>(history.size());
        for (BotMessage message : history) {
            if (message == null) {
                continue;
            }
            String body = message.getBody();
            if (body == null || body.isBlank()) {
                sanitized.add(message);
                continue;
            }
            String cleaned = stripLegacyKeyNag(body);
            if (cleaned.equals(body)) {
                sanitized.add(message);
                continue;
            }
            BotMessage copy = new BotMessage();
            copy.setId(message.getId());
            copy.setBotSession(message.getBotSession());
            copy.setRole(message.getRole());
            copy.setBody(cleaned);
            copy.setEntityStatus(message.getEntityStatus());
            copy.setCreatedAt(message.getCreatedAt());
            copy.setCreatedBy(message.getCreatedBy());
            sanitized.add(copy);
        }
        return sanitized;
    }

    public static String stripLegacyKeyNag(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = LEGACY_GEMINI_NAG.matcher(text).replaceAll(" ").trim();
        cleaned = LEGACY_API_KEY_NAG.matcher(cleaned).replaceAll(" ").trim();
        return cleaned.replaceAll(" +", " ").trim();
    }

    /**
     * For vague follow-ups ("go ahead", "yes"), reuse the last substantive user question
     * so keyword fallbacks and retrieval stay on-topic.
     */
    public static String effectiveUserQuery(List<BotMessage> history) {
        String latest = BotLlmFallbackSupport.lastUserMessage(history);
        if (latest == null || latest.isBlank()) {
            return "";
        }
        if (!isContinuation(latest)) {
            return latest.trim();
        }
        if (history == null) {
            return latest.trim();
        }
        int continuationSeen = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            BotMessage message = history.get(i);
            if (message.getRole() != BotMessageRole.USER || message.getBody() == null) {
                continue;
            }
            String body = message.getBody().trim();
            if (body.isBlank()) {
                continue;
            }
            if (isContinuation(body)) {
                continuationSeen++;
                continue;
            }
            if (continuationSeen > 0 || !body.equalsIgnoreCase(latest.trim())) {
                return body;
            }
        }
        return latest.trim();
    }

    public static boolean isContinuation(String text) {
        String lower = text.toLowerCase(Locale.ROOT).trim();
        if (lower.length() <= 3) {
            return true;
        }
        return lower.matches("^(yes|yeah|yep|ok|okay|sure|please|thanks|thank you|go ahead|continue|proceed|"
                + "well,? you can( just)? go ahead|carry on|do it|tell me more|go on|what|waht|huh|sorry|"
                + "pardon|repeat|again)[.!?]*$");
    }
}
