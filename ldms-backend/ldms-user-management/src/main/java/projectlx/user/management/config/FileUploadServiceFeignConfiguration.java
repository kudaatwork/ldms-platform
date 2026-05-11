package projectlx.user.management.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import projectlx.user.management.clients.FileUploadServiceClient;

/**
 * Builds {@link FileUploadServiceClient} after Config Server has merged properties, so the Feign base URL
 * respects {@code ldms.dev.force-local-feign-clients} and {@code CLIENTS_FILE_UPLOAD_SERVICE_URL}.
 */
@Slf4j
@Configuration
public class FileUploadServiceFeignConfiguration {

    @Bean
    public FileUploadServiceClient fileUploadServiceClient(ApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = LdmsFeignUrls.resolveFileUploadServiceBaseUrl(env);
        log.info("File upload Feign client base URL: {} (ldms.dev.force-local-feign-clients={})",
                url, env.getProperty("ldms.dev.force-local-feign-clients", "false"));
        return new FeignClientBuilder(applicationContext)
                .forType(FileUploadServiceClient.class, "file-upload-service")
                .inheritParentContext(true)
                .contextId("file-upload-service")
                .url(url)
                .build();
    }
}
