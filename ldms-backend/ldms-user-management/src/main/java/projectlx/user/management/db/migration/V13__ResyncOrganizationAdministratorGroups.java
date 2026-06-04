package projectlx.user.management.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Re-applies organisation {@code Administrator} groups with {@code ORGANIZATION_ADMINISTRATOR} and
 * workspace permission roles after catalog updates.
 */
public class V13__ResyncOrganizationAdministratorGroups extends BaseJavaMigration {

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
            String sql = """
                    UPDATE user
                    SET user_group_id = ?,
                        updated_at = NOW(6)
                    WHERE organization_id = ?
                      AND entity_status = 'ACTIVE'
                      AND user_group_id IS NULL
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, groupId);
                ps.setLong(2, organizationId);
                ps.executeUpdate();
            }
        }
    }
}
