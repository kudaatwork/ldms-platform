package projectlx.user.management.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Creates organisation-scoped {@code Administrator} groups (with workspace permissions) and moves
 * organisation users off the platform-wide Administrator group.
 */
public class V12__BackfillOrganizationAdministratorGroups extends BaseJavaMigration {

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
            throws SQLException {
        String sql = """
                UPDATE user
                SET user_group_id = ?,
                    updated_at = NOW(6)
                WHERE organization_id = ?
                  AND entity_status = 'ACTIVE'
                  AND (user_group_id IS NULL OR user_group_id IN (
                      SELECT id FROM (
                          SELECT g.id FROM user_group g
                          WHERE LOWER(g.name) = LOWER('Administrator')
                            AND g.organization_id IS NULL
                            AND g.entity_status <> 'DELETED'
                      ) platform_admin
                  ))
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setLong(2, organizationId);
            ps.executeUpdate();
        }
    }
}
