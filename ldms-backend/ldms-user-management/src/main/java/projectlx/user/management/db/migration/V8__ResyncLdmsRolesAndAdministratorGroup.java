package projectlx.user.management.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Re-syncs the full LDMS role catalog (190 permissions) and grants every active role to the
 * {@code Administrator} user group. Idempotent — safe on every deploy.
 */
public class V8__ResyncLdmsRolesAndAdministratorGroup extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        LdmsRoleCatalogSeeder.seedAllRolesAndAdministratorGroup(context.getConnection());
    }
}
