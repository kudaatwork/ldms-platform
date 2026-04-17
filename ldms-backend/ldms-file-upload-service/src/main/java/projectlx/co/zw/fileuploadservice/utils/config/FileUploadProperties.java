package projectlx.co.zw.fileuploadservice.utils.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    /**
     * Legacy local directory for {@link projectlx.co.zw.shared_library.utils.enums.StorageProvider#LOCAL} files.
     */
    private String location = "./uploads";
}
