package projectlx.user.management.utils.security;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Canonical LDMS permission codes for seeding {@code user_role} and linking to user groups.
 */
public final class LdmsRoleCatalog {

    private static final String CATALOG_RESOURCE = "ldms/role-catalog.properties";

    private LdmsRoleCatalog() {
    }

    public record RoleSeed(String role, String description) {
    }

    public static List<RoleSeed> all() {
        Map<String, RoleSeed> unique = new LinkedHashMap<>();
        loadFromProperties(unique);
        addEnumRoles(unique, UserRoles.class);
        addEnumRoles(unique, UserAccountRoles.class);
        addEnumRoles(unique, UserAddressRoles.class);
        addEnumRoles(unique, UserGroupRoles.class);
        addEnumRoles(unique, UserPasswordRoles.class);
        addEnumRoles(unique, UserPreferencesRoles.class);
        addEnumRoles(unique, UserRoleRoles.class);
        addEnumRoles(unique, UserSecurityRoles.class);
        addEnumRoles(unique, UserTypeRoles.class);
        addEnumRoles(unique, PlatformRoles.class);
        return unique.values().stream()
                .sorted(Comparator.comparing(RoleSeed::role))
                .toList();
    }

    private static void loadFromProperties(Map<String, RoleSeed> unique) {
        try (InputStream in = LdmsRoleCatalog.class.getClassLoader().getResourceAsStream(CATALOG_RESOURCE)) {
            if (in == null) {
                return;
            }
            Properties properties = new Properties();
            properties.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            for (String role : properties.stringPropertyNames()) {
                if (role.startsWith("#") || role.isBlank()) {
                    continue;
                }
                String normalized = role.trim().toUpperCase();
                String description = properties.getProperty(role, "").trim();
                unique.putIfAbsent(normalized, new RoleSeed(normalized, description));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load " + CATALOG_RESOURCE, ex);
        }
    }

    private static void addEnumRoles(Map<String, RoleSeed> unique, Class<?> enumClass) {
        if (enumClass == null || !enumClass.isEnum()) {
            return;
        }
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return;
        }
        for (Object constant : constants) {
            try {
                Method roleNameMethod = enumClass.getMethod("getRoleName");
                Method descriptionMethod = enumClass.getMethod("getDescription");
                String role = String.valueOf(roleNameMethod.invoke(constant)).trim().toUpperCase();
                String description = String.valueOf(descriptionMethod.invoke(constant)).trim();
                if (!role.isEmpty()) {
                    unique.putIfAbsent(role, new RoleSeed(role, description));
                }
            } catch (ReflectiveOperationException ignored) {
                // Skip enums that do not follow the standard roleName/description contract.
            }
        }
    }
}
