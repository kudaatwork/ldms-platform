package projectlx.messaging.inbound.business.logic.support;

import org.springframework.core.env.Environment;

/** Resolves bot config when application.yml binds empty env placeholders over local overrides. */
public final class BotConfigPropertySupport {

    private BotConfigPropertySupport() {
    }

    public static String firstNonBlank(Environment env, String... keys) {
        if (env == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            String value = env.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public static boolean firstEnabled(Environment env, boolean defaultValue, String... keys) {
        if (env == null || keys == null) {
            return defaultValue;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            String value = env.getProperty(key);
            if (value != null && !value.isBlank()) {
                return Boolean.parseBoolean(value.trim());
            }
        }
        return defaultValue;
    }
}
