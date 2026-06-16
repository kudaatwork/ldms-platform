package projectlx.co.zw.shared_library.utils.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdministratorRoleScopePolicyTest {

    @Test
    void organizationWorkspaceUsersReceiveOrganizationAdministratorAndUserManagementRoles() {
        List<String> effective = AdministratorRoleScopePolicy.filterRoleCodesForUser(
                List.of("ADMIN", "UPDATE_USER", "CREATE_COUNTRY", "VIEW_MY_ORGAN"),
                42L);

        assertTrue(effective.contains("ORGANIZATION_ADMINISTRATOR"));
        assertTrue(effective.contains("UPDATE_USER"));
        assertTrue(effective.contains("VIEW_MY_ORGAN"));
        assertFalse(effective.contains("ADMIN"));
        assertFalse(effective.contains("CREATE_COUNTRY"));
    }

    @Test
    void platformOperatorsExcludeOrganizationExclusiveRoles() {
        List<String> effective = AdministratorRoleScopePolicy.filterRoleCodesForUser(
                List.of("ADMIN", "UPDATE_USER", "VIEW_MY_ORGAN", "KYC_STAGE1"),
                null);

        assertTrue(effective.contains("ADMIN"));
        assertTrue(effective.contains("UPDATE_USER"));
        assertTrue(effective.contains("KYC_STAGE1"));
        assertFalse(effective.contains("VIEW_MY_ORGAN"));
        assertFalse(effective.contains("ORGANIZATION_ADMINISTRATOR"));
    }
}
