package projectlx.user.management.utils.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Canonical user-group naming: trim and uppercase (e.g. {@code administrator} → {@code ADMINISTRATOR}).
 */
public final class UserGroupNameSupport {

    public static final String ADMINISTRATOR_GROUP_NAME = "ADMINISTRATOR";
    public static final String DRIVER_GROUP_NAME = "DRIVER";

    private UserGroupNameSupport() {
    }

    /**
     * @return normalized name, or {@code null} when blank after trim
     */
    public static String normalize(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return name.trim().toUpperCase(Locale.ROOT);
    }
}
