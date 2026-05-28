package projectlx.co.zw.notifications.utils;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes Handlebars context so {@code {{firstName}}} shows the recipient's first name when present,
 * otherwise falls back to {@code userName} / {@code username}.
 */
public final class NotificationTemplateDataEnricher {

    private NotificationTemplateDataEnricher() {
    }

    public static Map<String, Object> enrich(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return data == null ? Map.of() : Map.copyOf(data);
        }

        Map<String, Object> enriched = new HashMap<>(data);

        String username = firstNonBlank(
                asTrimmedString(enriched.get("userName")),
                asTrimmedString(enriched.get("username")));

        String rawFirstName = firstNonBlank(
                asTrimmedString(enriched.get("firstName")),
                asTrimmedString(enriched.get("FirstName")),
                asTrimmedString(enriched.get("contactName")));

        String displayFirstName = StringUtils.hasText(rawFirstName) ? rawFirstName : username;

        if (StringUtils.hasText(displayFirstName)) {
            enriched.put("firstName", displayFirstName);
        }
        if (StringUtils.hasText(username)) {
            enriched.put("userName", username);
        }

        return Map.copyOf(enriched);
    }

    private static String asTrimmedString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
