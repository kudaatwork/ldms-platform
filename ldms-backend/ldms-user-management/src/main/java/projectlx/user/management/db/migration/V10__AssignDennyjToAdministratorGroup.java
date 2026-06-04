package projectlx.user.management.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Ensures the {@code Administrator} group exists and assigns {@code dennyj} for admin-portal access.
 */
public class V10__AssignDennyjToAdministratorGroup extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        LdmsRoleCatalogSeeder.seedAllRolesAndAdministratorGroup(context.getConnection());
        LdmsRoleCatalogSeeder.assignUserToAdministratorGroupByUsername(context.getConnection(), "dennyj");
    }
}
