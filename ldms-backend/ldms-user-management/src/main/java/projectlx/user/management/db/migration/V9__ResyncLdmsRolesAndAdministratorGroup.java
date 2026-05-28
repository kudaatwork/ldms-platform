package projectlx.user.management.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Re-syncs all LDMS service permissions from the role catalog and grants them to
 * the {@code Administrator} user group.
 */
public class V9__ResyncLdmsRolesAndAdministratorGroup extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        LdmsRoleCatalogSeeder.seedAllRolesAndAdministratorGroup(context.getConnection());
    }
}
