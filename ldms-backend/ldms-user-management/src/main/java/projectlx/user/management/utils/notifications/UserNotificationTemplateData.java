package projectlx.user.management.utils.notifications;

import org.springframework.util.StringUtils;
import projectlx.user.management.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds Handlebars context for user-related notification templates.
 */
public final class UserNotificationTemplateData {

    private UserNotificationTemplateData() {
    }

    public static Map<String, Object> forUser(User user) {
        return forUser(user, Map.of());
    }

    public static Map<String, Object> forUser(User user, Map<String, Object> extra) {
        Map<String, Object> data = new HashMap<>();
        if (extra != null && !extra.isEmpty()) {
            data.putAll(extra);
        }
        if (user == null) {
            return Map.copyOf(data);
        }

        String username = blankToNull(user.getUsername());
        String firstName = blankToNull(user.getFirstName());
        String displayFirstName = firstName != null ? firstName : username;

        if (displayFirstName != null) {
            data.put("firstName", displayFirstName);
        }
        if (username != null) {
            data.put("userName", username);
        }
        if (StringUtils.hasText(user.getEmail())) {
            data.put("email", user.getEmail());
            data.put("Email", user.getEmail());
        }
        if (StringUtils.hasText(user.getPhoneNumber())) {
            data.put("phoneNumber", user.getPhoneNumber());
        }

        return Map.copyOf(data);
    }

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
