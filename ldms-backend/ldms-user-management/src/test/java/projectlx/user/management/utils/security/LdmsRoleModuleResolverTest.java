package projectlx.user.management.utils.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LdmsRoleModuleResolverTest {

    @Test
    void resolvesPlatformAndServiceModules() {
        assertEquals("platform", LdmsRoleModuleResolver.resolve("ADMIN").key());
        assertEquals("organization-management", LdmsRoleModuleResolver.resolve("VIEW_MY_ORGAN").key());
        assertEquals("audit-trail", LdmsRoleModuleResolver.resolve("SEARCH_AUDIT_LOGS").key());
        assertEquals("notifications", LdmsRoleModuleResolver.resolve("CREATE_TEMPLATE").key());
        assertEquals("notifications", LdmsRoleModuleResolver.resolve("SEARCH_NOTIFICATION_LOGS").key());
        assertEquals("user-management.users", LdmsRoleModuleResolver.resolve("CREATE_USER").key());
        assertEquals("user-management.roles", LdmsRoleModuleResolver.resolve("EXPORT_USER_ROLES").key());
        assertEquals("locations.city", LdmsRoleModuleResolver.resolve("VIEW_CITY_BY_ID").key());
    }
}
