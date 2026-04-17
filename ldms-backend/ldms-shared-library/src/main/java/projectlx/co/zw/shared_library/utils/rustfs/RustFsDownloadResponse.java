package projectlx.co.zw.shared_library.utils.rustfs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RustFsDownloadResponse {
    private byte[] fileBytes;
    private String contentType;
    private String fileKey;
}
