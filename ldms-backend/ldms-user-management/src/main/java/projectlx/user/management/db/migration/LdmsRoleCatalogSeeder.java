package projectlx.user.management.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import projectlx.user.management.utils.security.LdmsRoleCatalog;
import projectlx.user.management.utils.security.LdmsRoleCatalog.RoleSeed;

/**
 * Idempotent seed of {@code user_role} rows and full assignment to the {@code Administrator} group.
 */
public final class LdmsRoleCatalogSeeder {

    private static final String ADMIN_GROUP_NAME = "Administrator";

    private LdmsRoleCatalogSeeder() {
    }

    public static void seedAllRolesAndAdministratorGroup(Connection connection) throws SQLException {
        ensureRoleUniqueIndex(connection);
        for (RoleSeed seed : LdmsRoleCatalog.all()) {
            upsertRole(connection, seed.role(), seed.description());
        }
        long groupId = ensureAdministratorGroup(connection);
        linkAllActiveRolesToGroup(connection, groupId);
    }

    private static void ensureRoleUniqueIndex(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    """
                    CREATE UNIQUE INDEX uk_user_role_role ON user_role (role)
                    """);
        } catch (SQLException ex) {
            if (ex.getErrorCode() != 1061 && ex.getErrorCode() != 1062) {
                String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                if (!message.contains("duplicate key name") && !message.contains("already exists")) {
                    throw ex;
                }
            }
        }
    }

    private static void upsertRole(Connection connection, String role, String description) throws SQLException {
        String sql = """
                INSERT INTO user_role (role, description, entity_status, created_at, updated_at)
                VALUES (?, ?, 'ACTIVE', NOW(6), NOW(6))
                ON DUPLICATE KEY UPDATE
                    description = VALUES(description),
                    entity_status = 'ACTIVE',
                    updated_at = NOW(6)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, description);
            ps.executeUpdate();
        }
    }

    private static long ensureAdministratorGroup(Connection connection) throws SQLException {
        Long existingId = findAdministratorGroupId(connection);
        if (existingId != null) {
            String update = """
                    UPDATE user_group
                    SET description = 'Platform administrators with all LDMS permissions',
                        entity_status = 'ACTIVE',
                        updated_at = NOW(6)
                    WHERE id = ?
                    """;
            try (PreparedStatement ps = connection.prepareStatement(update)) {
                ps.setLong(1, existingId);
                ps.executeUpdate();
            }
            return existingId;
        }
        String insert = """
                INSERT INTO user_group (name, description, entity_status, created_at, updated_at)
                VALUES (?, 'Platform administrators with all LDMS permissions', 'ACTIVE', NOW(6), NOW(6))
                """;
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            ps.setString(1, ADMIN_GROUP_NAME);
            ps.executeUpdate();
        }
        Long createdId = findAdministratorGroupId(connection);
        if (createdId == null) {
            throw new IllegalStateException("Administrator user group was not created");
        }
        return createdId;
    }

    private static Long findAdministratorGroupId(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM user_group WHERE name = ? AND entity_status <> 'DELETED' LIMIT 1")) {
            ps.setString(1, ADMIN_GROUP_NAME);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private static void linkAllActiveRolesToGroup(Connection connection, long groupId) throws SQLException {
        String deleteRemoved = """
                DELETE ugur FROM user_group_user_role ugur
                INNER JOIN user_role ur ON ur.id = ugur.user_role_id
                WHERE ugur.user_group_id = ?
                  AND ur.entity_status = 'DELETED'
                """;
        try (PreparedStatement ps = connection.prepareStatement(deleteRemoved)) {
            ps.setLong(1, groupId);
            ps.executeUpdate();
        }

        String insertLinks = """
                INSERT IGNORE INTO user_group_user_role (user_group_id, user_role_id)
                SELECT ?, ur.id
                FROM user_role ur
                WHERE ur.entity_status = 'ACTIVE'
                """;
        try (PreparedStatement ps = connection.prepareStatement(insertLinks)) {
            ps.setLong(1, groupId);
            ps.executeUpdate();
        }
    }
}
