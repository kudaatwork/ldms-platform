package projectlx.user.authentication.service.utils.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LdmsFeignUrlsTest {

    @Test
    void resolveUserManagementServiceBaseUrl_usesLocalHostWhenForceLocal() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("ldms.dev.force-local-feign-clients", "true");
        env.setProperty("clients.base-url.userManagementService", "http://user-management:8081");

        assertEquals("http://127.0.0.1:8086", LdmsFeignUrls.resolveUserManagementServiceBaseUrl(env));
    }

    @Test
    void resolveUserManagementServiceBaseUrl_honorsExplicitOverride() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("CLIENTS_USER_MANAGEMENT_SERVICE_URL", "http://localhost:9099/");

        assertEquals("http://localhost:9099", LdmsFeignUrls.resolveUserManagementServiceBaseUrl(env));
    }
}
