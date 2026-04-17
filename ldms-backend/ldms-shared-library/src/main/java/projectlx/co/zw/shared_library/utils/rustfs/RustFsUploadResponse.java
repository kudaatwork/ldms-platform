package projectlx.co.zw.shared_library.utils.rustfs;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RustFsUploadResponse {
    private String fileKey;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private String fileHash;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private LocalDateTime storedAt;
}
