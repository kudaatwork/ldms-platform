package projectlx.co.zw.shared_library.utils.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Ensures {@link JwtProperties} is bound even when {@link UtilsConfig} is not explicitly imported.
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class LdmsJwtAutoConfiguration {
}
