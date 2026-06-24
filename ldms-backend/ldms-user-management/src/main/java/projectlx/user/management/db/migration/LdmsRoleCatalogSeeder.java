package projectlx.user.management.db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import projectlx.user.management.utils.support.OrganizationPortalRolePolicy;
import projectlx.user.management.utils.support.RoleClassificationPolicy;

/**
 * Idempotent seed of {@code user_role} rows and assignment to platform / organisation administrator groups.
 */
public final class LdmsRoleCatalogSeeder {

    private static final String ADMIN_GROUP_NAME = "Administrator";
    public static final String ORGANIZATION_ADMIN_GROUP_DESCRIPTION =
            "Organisation workspace administrators with user-management, audit, and self-service permissions";
    private static final String CATALOG_RESOURCE = "ldms/role-catalog.properties";

    private LdmsRoleCatalogSeeder() {
    }

    public record RoleSeed(String role, String description) {
    }

    public static void seedAllRolesAndAdministratorGroup(Connection connection) throws SQLException {
        ensureRoleUniqueIndex(connection);
        for (RoleSeed seed : loadRoleCatalog()) {
            upsertRole(connection, seed.role(), seed.description());
        }
        seedRoleClassifications(connection);
        long groupId = ensurePlatformAdministratorGroup(connection);
        linkAllActiveRolesToGroup(connection, groupId);
    }

    public static String administratorGroupName() {
        return ADMIN_GROUP_NAME;
    }

    public static long platformAdministratorGroupId(Connection connection) throws SQLException {
        Long existingId = findPlatformAdministratorGroupId(connection);
        if (existingId == null) {
            throw new IllegalStateException("Platform Administrator user group was not found");
        }
        return existingId;
    }

    /**
     * Ensures an organisation-scoped {@code Administrator} group exists and holds
     * classification-filtered workspace permissions.
     */
    public static long seedOrganizationAdministratorGroup(Connection connection, long organizationId) throws SQLException {
        return seedOrganizationAdministratorGroup(connection, organizationId, null);
    }

    /**
     * Ensures an organisation-scoped {@code Administrator} group exists and holds
     * classification-filtered workspace permissions.
     */
    public static long seedOrganizationAdministratorGroup(Connection connection, long organizationId, String organizationClassification) throws SQLException {
        if (organizationId <= 0) {
            throw new IllegalArgumentException("organizationId must be positive");
        }
        ensureRoleUniqueIndex(connection);
        for (RoleSeed seed : loadRoleCatalog()) {
            upsertRole(connection, seed.role(), seed.description());
        }
        seedRoleClassifications(connection);
        long groupId = ensureOrganizationAdministratorGroup(connection, organizationId);
        linkClassificationFilteredRolesToGroup(connection, groupId, organizationClassification);
        return groupId;
    }

    /**
     * Grants an existing active user platform-administrator group membership (admin portal).
     * Clears organisation/branch scope so the account is treated as a platform operator.
     */
    public static void assignUserToAdministratorGroupByUsername(Connection connection, String username)
            throws SQLException {
        if (username == null || username.isBlank()) {
            return;
        }
        long groupId = ensurePlatformAdministratorGroup(connection);
        String sql = """
                UPDATE user u
                INNER JOIN user_group g ON g.id = ?
                SET u.user_group_id = g.id,
                    u.organization_id = NULL,
                    u.branch_id = NULL,
                    u.updated_at = NOW(6)
                WHERE LOWER(u.username) = LOWER(?)
                  AND u.entity_status = 'ACTIVE'
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setString(2, username.trim());
            ps.executeUpdate();
        }
    }

    private static List<RoleSeed> loadRoleCatalog() {
        Map<String, RoleSeed> unique = new LinkedHashMap<>();
        try (InputStream in = LdmsRoleCatalogSeeder.class.getClassLoader().getResourceAsStream(CATALOG_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + CATALOG_RESOURCE);
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
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
        List<RoleSeed> seeds = new ArrayList<>(unique.values());
        seeds.sort(Comparator.comparing(RoleSeed::role));
        return seeds;
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

    private static long ensurePlatformAdministratorGroup(Connection connection) throws SQLException {
        Long existingId = findPlatformAdministratorGroupId(connection);
        if (existingId != null) {
            String update = """
                    UPDATE user_group
                    SET description = 'Platform administrators — full LDMS catalog; effective JWT roles depend on user organisation scope',
                        organization_id = NULL,
                        organization_classification = 'ADMIN_PORTAL',
                        is_system_group = TRUE,
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
                INSERT INTO user_group (name, description, organization_id, organization_classification, is_system_group, entity_status, created_at, updated_at)
                VALUES (?, 'Platform administrators — full LDMS catalog; effective JWT roles depend on user organisation scope', NULL, 'ADMIN_PORTAL', TRUE, 'ACTIVE', NOW(6), NOW(6))
                """;
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            ps.setString(1, ADMIN_GROUP_NAME);
            ps.executeUpdate();
        }
        Long createdId = findPlatformAdministratorGroupId(connection);
        if (createdId == null) {
            throw new IllegalStateException("Platform Administrator user group was not created");
        }
        return createdId;
    }

    private static long ensureOrganizationAdministratorGroup(Connection connection, long organizationId)
            throws SQLException {
        Long existingId = findOrganizationAdministratorGroupId(connection, organizationId);
        if (existingId != null) {
            String update = """
                    UPDATE user_group
                    SET description = ?,
                        is_system_group = TRUE,
                        entity_status = 'ACTIVE',
                        updated_at = NOW(6)
                    WHERE id = ?
                    """;
            try (PreparedStatement ps = connection.prepareStatement(update)) {
                ps.setString(1, ORGANIZATION_ADMIN_GROUP_DESCRIPTION);
                ps.setLong(2, existingId);
                ps.executeUpdate();
            }
            return existingId;
        }
        String insert = """
                INSERT INTO user_group (name, description, organization_id, is_system_group, entity_status, created_at, updated_at)
                VALUES (?, ?, ?, TRUE, 'ACTIVE', NOW(6), NOW(6))
                """;
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            ps.setString(1, ADMIN_GROUP_NAME);
            ps.setString(2, ORGANIZATION_ADMIN_GROUP_DESCRIPTION);
            ps.setLong(3, organizationId);
            ps.executeUpdate();
        }
        Long createdId = findOrganizationAdministratorGroupId(connection, organizationId);
        if (createdId == null) {
            throw new IllegalStateException(
                    "Organisation Administrator user group was not created for organisation " + organizationId);
        }
        return createdId;
    }

    private static Long findPlatformAdministratorGroupId(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                SELECT id FROM user_group
                WHERE LOWER(name) = LOWER(?)
                  AND organization_id IS NULL
                  AND entity_status <> 'DELETED'
                LIMIT 1
                """)) {
            ps.setString(1, ADMIN_GROUP_NAME);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private static Long findOrganizationAdministratorGroupId(Connection connection, long organizationId)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                SELECT id FROM user_group
                WHERE LOWER(name) = LOWER(?)
                  AND organization_id = ?
                  AND entity_status <> 'DELETED'
                LIMIT 1
                """)) {
            ps.setString(1, ADMIN_GROUP_NAME);
            ps.setLong(2, organizationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private static void linkOrganizationPortalRolesToGroup(Connection connection, long groupId) throws SQLException {
        linkClassificationFilteredRolesToGroup(connection, groupId, null);
    }

    private static void linkClassificationFilteredRolesToGroup(Connection connection, long groupId,
                                                                String organizationClassification) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM user_group_user_role WHERE user_group_id = ?")) {
            ps.setLong(1, groupId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM user_group_default_roles WHERE user_group_id = ?")) {
            ps.setLong(1, groupId);
            ps.executeUpdate();
        }

        String classificationFilter = organizationClassification == null || organizationClassification.isBlank()
                ? null
                : organizationClassification.trim().toUpperCase();
        for (RoleSeed seed : loadRoleCatalog()) {
            if (!OrganizationPortalRolePolicy.isOrganizationPortalRole(seed.role())) {
                continue;
            }
            // Read the admin-editable persisted mapping (source of truth), not the bootstrap policy.
            if (classificationFilter != null
                    && !roleHasClassification(connection, seed.role(), classificationFilter)) {
                continue;
            }
            String insertLink = """
                    INSERT IGNORE INTO user_group_user_role (user_group_id, user_role_id)
                    SELECT ?, ur.id FROM user_role ur
                    WHERE ur.role = ? AND ur.entity_status = 'ACTIVE'
                    """;
            try (PreparedStatement ps = connection.prepareStatement(insertLink)) {
                ps.setLong(1, groupId);
                ps.setString(2, seed.role());
                ps.executeUpdate();
            }
            String insertDefault = """
                    INSERT IGNORE INTO user_group_default_roles (user_group_id, user_role_id)
                    SELECT ?, ur.id FROM user_role ur
                    WHERE ur.role = ? AND ur.entity_status = 'ACTIVE'
                    """;
            try (PreparedStatement ps = connection.prepareStatement(insertDefault)) {
                ps.setLong(1, groupId);
                ps.setString(2, seed.role());
                ps.executeUpdate();
            }
        }
    }

    private static boolean hasAnyRoleClassificationMapping(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM user_role_organization_classifications LIMIT 1");
                ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }

    private static boolean roleHasClassification(Connection connection, String role, String classification)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                SELECT 1 FROM user_role_organization_classifications uroc
                INNER JOIN user_role ur ON ur.id = uroc.user_role_id
                WHERE ur.role = ? AND UPPER(uroc.organization_classification) = ?
                LIMIT 1
                """)) {
            ps.setString(1, role);
            ps.setString(2, classification);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void seedRoleClassifications(Connection connection) throws SQLException {
        // Clear stale mappings for deleted roles (always — keeps the editable table tidy)
        String cleanup = """
                DELETE uroc FROM user_role_organization_classifications uroc
                LEFT JOIN user_role ur ON ur.id = uroc.user_role_id
                WHERE ur.id IS NULL OR ur.entity_status = 'DELETED'
                """;
        try (PreparedStatement ps = connection.prepareStatement(cleanup)) {
            ps.executeUpdate();
        }

        // The classification -> role mapping is admin-editable from the portal, so the policy only
        // provides the one-time bootstrap. Once any mapping exists, the persisted table is the
        // source of truth and must not be overwritten on subsequent startups.
        if (hasAnyRoleClassificationMapping(connection)) {
            return;
        }

        for (RoleSeed seed : loadRoleCatalog()) {
            Set<String> classifications = RoleClassificationPolicy.classificationsForRole(seed.role());
            if (classifications.isEmpty()) {
                // Remove any existing mappings for platform-only roles
                String delete = """
                        DELETE uroc FROM user_role_organization_classifications uroc
                        INNER JOIN user_role ur ON ur.id = uroc.user_role_id
                        WHERE ur.role = ?
                        """;
                try (PreparedStatement ps = connection.prepareStatement(delete)) {
                    ps.setString(1, seed.role());
                    ps.executeUpdate();
                }
                continue;
            }
            for (String classification : classifications) {
                String upsert = """
                        INSERT IGNORE INTO user_role_organization_classifications (user_role_id, organization_classification)
                        SELECT ur.id, ? FROM user_role ur
                        WHERE ur.role = ? AND ur.entity_status = 'ACTIVE'
                        """;
                try (PreparedStatement ps = connection.prepareStatement(upsert)) {
                    ps.setString(1, classification.trim().toUpperCase());
                    ps.setString(2, seed.role());
                    ps.executeUpdate();
                }
            }
        }
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
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM user_group_default_roles WHERE user_group_id = ?")) {
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
        String insertDefaults = """
                INSERT IGNORE INTO user_group_default_roles (user_group_id, user_role_id)
                SELECT ?, ur.id
                FROM user_role ur
                WHERE ur.entity_status = 'ACTIVE'
                """;
        try (PreparedStatement ps = connection.prepareStatement(insertDefaults)) {
            ps.setLong(1, groupId);
            ps.executeUpdate();
        }
    }
}
