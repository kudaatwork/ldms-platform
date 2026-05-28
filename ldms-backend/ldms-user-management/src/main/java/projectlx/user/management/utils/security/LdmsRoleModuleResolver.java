package projectlx.user.management.utils.security;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maps permission codes to a stable module key and display label for admin UI grouping.
 */
public final class LdmsRoleModuleResolver {

    public record RoleModule(String key, String label, int sortOrder) {
    }

    private static final Map<String, RoleModule> MODULES = buildModules();
    private static final List<ModuleRule> RULES = buildRules();

    private LdmsRoleModuleResolver() {
    }

    public static RoleModule resolve(String role) {
        if (role == null || role.isBlank()) {
            return MODULES.get("other");
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        for (ModuleRule rule : RULES) {
            if (rule.matches(normalized)) {
                return rule.module();
            }
        }
        return MODULES.get("other");
    }

    public static List<RoleModule> allModulesSorted() {
        return MODULES.values().stream()
                .sorted(Comparator.comparingInt(RoleModule::sortOrder).thenComparing(RoleModule::label))
                .toList();
    }

    private static Map<String, RoleModule> buildModules() {
        Map<String, RoleModule> modules = new LinkedHashMap<>();
        int order = 0;
        modules.put("platform", new RoleModule("platform", "Platform", order++));
        modules.put("user-management.users", new RoleModule("user-management.users", "User management — Users", order++));
        modules.put("user-management.accounts", new RoleModule("user-management.accounts", "User management — Accounts", order++));
        modules.put("user-management.addresses", new RoleModule("user-management.addresses", "User management — Addresses", order++));
        modules.put("user-management.groups", new RoleModule("user-management.groups", "User management — Groups", order++));
        modules.put("user-management.roles", new RoleModule("user-management.roles", "User management — Roles", order++));
        modules.put("user-management.security", new RoleModule("user-management.security", "User management — Security", order++));
        modules.put("user-management.preferences", new RoleModule("user-management.preferences", "User management — Preferences", order++));
        modules.put("user-management.types", new RoleModule("user-management.types", "User management — Types", order++));
        modules.put("user-management.password", new RoleModule("user-management.password", "User management — Password & verification", order++));
        modules.put("organization-management", new RoleModule("organization-management", "Organization management", order++));
        modules.put("audit-trail", new RoleModule("audit-trail", "Audit trail", order++));
        modules.put("notifications", new RoleModule("notifications", "Notifications", order++));
        modules.put("locations.address", new RoleModule("locations.address", "Locations — Addresses", order++));
        modules.put("locations.administrative-level", new RoleModule("locations.administrative-level", "Locations — Administrative levels", order++));
        modules.put("locations.city", new RoleModule("locations.city", "Locations — Cities", order++));
        modules.put("locations.country", new RoleModule("locations.country", "Locations — Countries", order++));
        modules.put("locations.district", new RoleModule("locations.district", "Locations — Districts", order++));
        modules.put("locations.geo-coordinates", new RoleModule("locations.geo-coordinates", "Locations — Geo coordinates", order++));
        modules.put("locations.language", new RoleModule("locations.language", "Locations — Languages", order++));
        modules.put("locations.localized-name", new RoleModule("locations.localized-name", "Locations — Localized names", order++));
        modules.put("locations.location-node", new RoleModule("locations.location-node", "Locations — Location nodes", order++));
        modules.put("locations.province", new RoleModule("locations.province", "Locations — Provinces", order++));
        modules.put("locations.suburb", new RoleModule("locations.suburb", "Locations — Suburbs", order++));
        modules.put("locations.village", new RoleModule("locations.village", "Locations — Villages", order++));
        modules.put("other", new RoleModule("other", "Other", order++));
        return Map.copyOf(modules);
    }

    private static List<ModuleRule> buildRules() {
        RoleModule platform = MODULES.get("platform");
        RoleModule org = MODULES.get("organization-management");
        RoleModule audit = MODULES.get("audit-trail");
        RoleModule notifications = MODULES.get("notifications");
        return List.of(
                new ModuleRule(platform, Set.of("ADMIN", "KYC_STAGE1", "KYC_STAGE2", "READ_ONLY")),
                new ModuleRule(org, Set.of(
                        "SUBMIT_KYC", "VIEW_MY_ORGAN", "UPDATE_MY_ORGAN", "MANAGE_BRANCHES",
                        "LIST_CUSTOMERS", "REGISTER_CUSTOMER", "LINK_TRANSPORTER")),
                new ModuleRule(audit, prefix("AUDIT_LOG")),
                new ModuleRule(notifications, r -> contains("TEMPLATE").test(r) || contains("NOTIFICATION_LOG").test(r)),
                new ModuleRule(MODULES.get("user-management.password"), Set.of(
                        "CHANGE_USER_PASSWORD", "RESET_USER_PASSWORD", "FORGOT_PASSWORD",
                        "VALIDATE_RESET_TOKEN", "VERIFY_USER_EMAIL", "RESEND_VERIFICATION_LINK")),
                new ModuleRule(MODULES.get("user-management.accounts"), contains("USER_ACCOUNT")),
                new ModuleRule(MODULES.get("user-management.addresses"), contains("USER_ADDRESS")),
                new ModuleRule(MODULES.get("user-management.groups"), r -> r.contains("USER_GROUP")
                        || r.equals("ASSIGN_USER_ROLES_TO_USER_GROUP")
                        || r.equals("REMOVE_USER_ROLES_FROM_USER_GROUP")),
                new ModuleRule(MODULES.get("user-management.roles"), r -> r.contains("USER_ROLE")
                        && !r.equals("ASSIGN_USER_ROLES_TO_USER_GROUP")
                        && !r.equals("REMOVE_USER_ROLES_FROM_USER_GROUP")),
                new ModuleRule(MODULES.get("user-management.security"), contains("USER_SECURIT")),
                new ModuleRule(MODULES.get("user-management.preferences"), contains("USER_PREFERENCES")),
                new ModuleRule(MODULES.get("user-management.types"), contains("USER_TYPE")),
                new ModuleRule(MODULES.get("user-management.users"), r -> r.contains("USER")
                        && !r.contains("USER_ACCOUNT")
                        && !r.contains("USER_ADDRESS")
                        && !r.contains("USER_GROUP")
                        && !r.contains("USER_ROLE")
                        && !r.contains("USER_SECURIT")
                        && !r.contains("USER_PREFERENCES")
                        && !r.contains("USER_TYPE")),
                new ModuleRule(MODULES.get("locations.address"), r -> r.contains("ADDRESS") && !r.contains("USER_ADDRESS")),
                new ModuleRule(MODULES.get("locations.administrative-level"), contains("ADMINISTRATIVE_LEVEL")),
                new ModuleRule(MODULES.get("locations.city"), entity("CITY", "CITIES")),
                new ModuleRule(MODULES.get("locations.country"), entity("COUNTRY", "COUNTRIES")),
                new ModuleRule(MODULES.get("locations.district"), entity("DISTRICT", "DISTRICTS")),
                new ModuleRule(MODULES.get("locations.geo-coordinates"), contains("GEO_COORDINATES")),
                new ModuleRule(MODULES.get("locations.language"), entity("LANGUAGE", "LANGUAGES")),
                new ModuleRule(MODULES.get("locations.localized-name"), contains("LOCALIZED_NAME")),
                new ModuleRule(MODULES.get("locations.location-node"), contains("LOCATION_NODE")),
                new ModuleRule(MODULES.get("locations.province"), entity("PROVINCE", "PROVINCES")),
                new ModuleRule(MODULES.get("locations.suburb"), entity("SUBURB", "SUBURBS")),
                new ModuleRule(MODULES.get("locations.village"), entity("VILLAGE", "VILLAGES"))
        );
    }

    private static java.util.function.Predicate<String> contains(String fragment) {
        return role -> role.contains(fragment);
    }

    private static java.util.function.Predicate<String> prefix(String fragment) {
        return role -> role.startsWith(fragment) || role.contains("_" + fragment);
    }

    private static java.util.function.Predicate<String> entity(String singular, String plural) {
        return role -> role.contains("_" + singular) || role.contains("_" + plural);
    }

    private record ModuleRule(RoleModule module, java.util.function.Predicate<String> predicate) {
        ModuleRule(RoleModule module, Set<String> exact) {
            this(module, exact::contains);
        }

        boolean matches(String role) {
            return predicate.test(role);
        }
    }
}
