package projectlx.user.management.config;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import projectlx.user.management.db.migration.LdmsRoleCatalogSeeder;

/**
 * Ensures role catalog data and Administrator grants are always synchronized at startup.
 */
@Component
public class LdmsRoleCatalogStartupSync {

    private static final Logger log = LoggerFactory.getLogger(LdmsRoleCatalogStartupSync.class);

    private final DataSource dataSource;

    @Value("${ldms.roles.sync-on-startup:true}")
    private boolean syncOnStartup;

    public LdmsRoleCatalogStartupSync(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void syncRolesAtStartup() {
        if (!syncOnStartup) {
            log.info("LDMS role startup sync disabled by configuration");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            LdmsRoleCatalogSeeder.seedAllRolesAndAdministratorGroup(connection);
            log.info("LDMS role catalog synchronized and Administrator grants refreshed");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to synchronize LDMS role catalog at startup", ex);
        }
    }
}
