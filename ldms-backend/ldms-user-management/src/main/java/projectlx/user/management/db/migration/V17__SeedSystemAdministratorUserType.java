package projectlx.user.management.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import projectlx.user.management.business.logic.support.OrganizationPortalUserTypePolicy;

/**
 * Ensures the default platform-portal bootstrap user type exists for first-login workspace setup.
 */
public class V17__SeedSystemAdministratorUserType extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String name = OrganizationPortalUserTypePolicy.SYSTEM_ADMINISTRATOR;
        try (PreparedStatement count = connection.prepareStatement(
                """
                SELECT COUNT(*) FROM user_type
                WHERE user_type_name = ?
                  AND entity_status <> 'DELETED'
                """)) {
            count.setString(1, name);
            try (ResultSet rs = count.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                """
                INSERT INTO user_type (user_type_name, description, created_at, updated_at, entity_status)
                VALUES (?, ?, NOW(6), NOW(6), 'ACTIVE')
                """)) {
            insert.setString(1, name);
            insert.setString(2, "Platform workspace administrator with full organisation access");
            insert.executeUpdate();
        }
    }
}
