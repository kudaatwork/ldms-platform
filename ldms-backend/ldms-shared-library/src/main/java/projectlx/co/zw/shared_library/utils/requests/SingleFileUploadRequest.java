package projectlx.co.zw.shared_library.utils.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.json.LenientLocalDateTimeDeserializer;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class SingleFileUploadRequest {
    private Long id;
    private MultipartFile file;
    private String fileType;

    @JsonDeserialize(using = LenientLocalDateTimeDeserializer.class)
    private LocalDateTime expiresAt;
}