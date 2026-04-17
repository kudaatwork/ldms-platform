package projectlx.co.zw.fileuploadservice.utils.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One element of the {@code filesMetadata} array sent by Feign clients (JSON only — no multipart bytes).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeignMultipartFileMetadataEntry {

    private Long id;
    private String fileType;
    private LocalDateTime expiresAt;
}
