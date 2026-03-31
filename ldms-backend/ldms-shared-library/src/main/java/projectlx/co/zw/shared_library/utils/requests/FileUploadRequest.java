package projectlx.co.zw.shared_library.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class FileUploadRequest {
    private List<SingleFileUploadRequest> filesMetadata;
    private String ownerType;
    private Long ownerId;
}
