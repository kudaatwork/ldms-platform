package projectlx.user.management.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Restores organisation-scoped {@code Administrator} groups for platform portal workspaces and
 * moves organisation users off the shared platform Administrator group.
 */
public class V24__RestoreOrganizationWorkspaceAdministratorGroups extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        LdmsRoleCatalogSeeder.seedAllRolesAndAdministratorGroup(connection);

        Set<Long> organizationIds = new LinkedHashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(
                """
                SELECT DISTINCT organization_id FROM user
                WHERE organization_id IS NOT NULL
                  AND organization_id > 0
                  AND entity_status = 'ACTIVE'
                """)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    organizationIds.add(rs.getLong(1));
                }
            }
        }

        for (Long organizationId : organizationIds) {
            long groupId = LdmsRoleCatalogSeeder.seedOrganizationAdministratorGroup(connection, organizationId);
            reassignOrganisationUsers(connection, organizationId, groupId);
        }
    }

    private static void reassignOrganisationUsers(Connection connection, long organizationId, long groupId)
            throws Exception {
        String sql = """
                UPDATE user
                SET user_group_id = ?,
                    updated_at = NOW(6)
                WHERE organization_id = ?
                  AND entity_status = 'ACTIVE'
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setLong(2, organizationId);
            ps.executeUpdate();
        }
    }
}
