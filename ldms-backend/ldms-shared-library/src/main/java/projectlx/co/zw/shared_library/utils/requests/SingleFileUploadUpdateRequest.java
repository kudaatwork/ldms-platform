package projectlx.co.zw.shared_library.utils.requests;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;


public class SingleFileUploadUpdateRequest {
    private Long id; // ✅ Added here
    private MultipartFile file;
    private String fileType;
    private LocalDateTime expiresAt;
}
