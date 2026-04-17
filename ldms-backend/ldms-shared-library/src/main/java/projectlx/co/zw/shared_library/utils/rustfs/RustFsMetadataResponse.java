package projectlx.co.zw.shared_library.utils.rustfs;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RustFsMetadataResponse {
    private String fileKey;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private String fileHash;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime storedAt;

    @JsonProperty("isDeleted")
    private boolean deleted;
}
