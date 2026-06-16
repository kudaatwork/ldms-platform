package projectlx.user.management.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Ensures a single platform-wide {@code Administrator} group and reassigns users off any remaining
 * per-organisation duplicate groups (idempotent with {@link V19__ConsolidateOrganizationAdministratorGroups}).
 */
public class V20__ConsolidateAdministratorGroupsAndEnsureWorkspaceRoles extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        LdmsRoleCatalogSeeder.seedAllRolesAndAdministratorGroup(connection);
        long platformGroupId = LdmsRoleCatalogSeeder.platformAdministratorGroupId(connection);
        ensureOrganizationAdministratorRoleLinked(connection, platformGroupId);

        try (PreparedStatement ps = connection.prepareStatement(
                """
                SELECT id FROM user_group
                WHERE organization_id IS NOT NULL
                  AND organization_id > 0
                  AND LOWER(name) = LOWER(?)
                  AND entity_status <> 'DELETED'
                """)) {
            ps.setString(1, LdmsRoleCatalogSeeder.administratorGroupName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long orgGroupId = rs.getLong(1);
                    reassignMembers(connection, orgGroupId, platformGroupId);
                    softDeleteGroup(connection, orgGroupId);
                }
            }
        }
    }

    private static void ensureOrganizationAdministratorRoleLinked(Connection connection, long groupId)
            throws SQLException {
        String sql = """
                INSERT IGNORE INTO user_group_user_role (user_group_id, user_role_id)
                SELECT ?, ur.id FROM user_role ur
                WHERE ur.role = 'ORGANIZATION_ADMINISTRATOR'
                  AND ur.entity_status = 'ACTIVE'
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.executeUpdate();
        }
    }

    private static void reassignMembers(Connection connection, long fromGroupId, long toGroupId)
            throws SQLException {
        String sql = """
                UPDATE user
                SET user_group_id = ?,
                    updated_at = NOW(6)
                WHERE user_group_id = ?
                  AND entity_status <> 'DELETED'
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, toGroupId);
            ps.setLong(2, fromGroupId);
            ps.executeUpdate();
        }
    }

    private static void softDeleteGroup(Connection connection, long groupId) throws SQLException {
        String sql = """
                UPDATE user_group
                SET entity_status = 'DELETED',
                    updated_at = NOW(6)
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.executeUpdate();
        }
    }
}
