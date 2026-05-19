package projectlx.co.zw.notifications.utils.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class LdmsConfigRepoDotenvTest {

    @Test
    @EnabledIf("configRepoEnvExists")
    void resolvesSiblingConfigRepoEnvFile() {
        Path envFile = LdmsConfigRepoDotenv.resolveEnvFile();
        assertNotNull(envFile);
        assertTrue(Files.isRegularFile(envFile), "expected .env at " + envFile);
    }

    @Test
    @EnabledIf("configRepoEnvExists")
    void loadsSendGridAndAwsKeysWithoutExposingValues() {
        Map<String, Object> props = LdmsConfigRepoDotenv.loadPropertiesForSpring();
        assertFalse(props.isEmpty());
        assertTrue(LdmsConfigRepoDotenv.containsEmailKeys(props), "expected email-related keys in .env");
    }

    @Test
    @EnabledIf("configRepoEnvExists")
    void mapsSpringPropertyAliases() {
        Map<String, Object> props = LdmsConfigRepoDotenv.loadPropertiesForSpring();
        if (props.containsKey("SENDGRID_API_KEY")) {
            assertTrue(props.containsKey("sendgrid.api-key"));
        }
        if (props.containsKey("TWILIO_ACCOUNT_SID")) {
            assertTrue(props.containsKey("twilio.account-sid"));
            assertTrue(LdmsConfigRepoDotenv.containsTwilioKeys(props));
        }
    }

    @Test
    void emptyOsEnvironmentDoesNotBlockDotenv() {
        assertFalse(LdmsConfigRepoDotenv.hasNonEmptyOsValue("SENDGRID_API_KEY_THAT_SHOULD_NOT_EXIST_XYZ"));
    }

    @Test
    @EnabledIf("configRepoEnvExists")
    void secretsResolverReadsAwsKeysFromFile() {
        LdmsConfigRepoSecretsResolver.resetCacheForTests();
        LdmsConfigRepoSecretsResolver resolver = new LdmsConfigRepoSecretsResolver(
                new org.springframework.mock.env.MockEnvironment());
        assertTrue(org.springframework.util.StringUtils.hasText(resolver.awsAccessKeyId()));
        assertTrue(org.springframework.util.StringUtils.hasText(resolver.awsSecretAccessKey()));
        assertTrue(org.springframework.util.StringUtils.hasText(resolver.awsSesFromEmail()));
        assertTrue(resolver.isSesConfigured());
    }

    static boolean configRepoEnvExists() {
        Path envFile = LdmsConfigRepoDotenv.resolveEnvFile();
        return envFile != null && Files.isRegularFile(envFile);
    }
}
