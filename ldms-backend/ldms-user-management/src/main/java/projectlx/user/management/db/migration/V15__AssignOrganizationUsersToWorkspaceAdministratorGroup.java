package projectlx.user.management.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Ensures every active organisation user belongs to that organisation's workspace
 * {@code Administrator} group (with {@code ORGANIZATION_ADMINISTRATOR} and portal permissions).
 */
public class V15__AssignOrganizationUsersToWorkspaceAdministratorGroup extends BaseJavaMigration {

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
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                    UPDATE user
                    SET user_group_id = ?,
                        updated_at = NOW(6)
                    WHERE organization_id = ?
                      AND entity_status = 'ACTIVE'
                    """)) {
                ps.setLong(1, groupId);
                ps.setLong(2, organizationId);
                ps.executeUpdate();
            }
        }
    }
}
