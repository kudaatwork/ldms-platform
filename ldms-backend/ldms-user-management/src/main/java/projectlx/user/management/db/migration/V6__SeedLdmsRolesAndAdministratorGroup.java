package projectlx.user.management.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Seeds all LDMS permission roles and grants them to the {@code Administrator} user group.
 */
public class V6__SeedLdmsRolesAndAdministratorGroup extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        LdmsRoleCatalogSeeder.seedAllRolesAndAdministratorGroup(context.getConnection());
    }
}
