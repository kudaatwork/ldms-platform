package projectlx.user.management.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Re-syncs the full LDMS role catalog and grants every active role to the {@code Administrator} group.
 * Safe to run after catalog additions (e.g. new service permissions).
 */
public class V7__ResyncLdmsRolesAndAdministratorGroup extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        LdmsRoleCatalogSeeder.seedAllRolesAndAdministratorGroup(context.getConnection());
    }
}
