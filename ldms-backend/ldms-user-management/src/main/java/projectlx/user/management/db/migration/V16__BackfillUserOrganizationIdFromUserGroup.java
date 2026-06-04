package projectlx.user.management.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Copies {@code user_group.organization_id} onto {@code user.organization_id} when the user row
 * is linked to an organisation workspace group but the column was never set (breaks org listings).
 */
public class V16__BackfillUserOrganizationIdFromUserGroup extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                """
                UPDATE user u
                INNER JOIN user_group g ON u.user_group_id = g.id
                SET u.organization_id = g.organization_id,
                    u.updated_at = NOW(6)
                WHERE (u.organization_id IS NULL OR u.organization_id <= 0)
                  AND g.organization_id IS NOT NULL
                  AND g.organization_id > 0
                  AND u.entity_status <> 'DELETED'
                """)) {
            ps.executeUpdate();
        }
    }
}
