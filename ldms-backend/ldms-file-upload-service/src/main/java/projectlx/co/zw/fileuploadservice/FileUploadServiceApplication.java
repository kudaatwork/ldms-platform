package projectlx.co.zw.fileuploadservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import projectlx.co.zw.fileuploadservice.utils.config.FileUploadProperties;
import projectlx.co.zw.shared_library.utils.rustfs.RustFsClientConfig;

@SpringBootApplication(scanBasePackages = {
        "projectlx.co.zw.fileuploadservice",
        "projectlx.co.zw.shared_library"
})
@Import(RustFsClientConfig.class)
@EnableConfigurationProperties(FileUploadProperties.class)
@EnableDiscoveryClient
public class FileUploadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileUploadServiceApplication.class, args);
    }
}
