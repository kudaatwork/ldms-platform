package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@ToString
public class CreateProductDocumentRequest {

    private Long productId;
    private String name;
    private String description;

    // External reference
    private MultipartFile documentUpload;

    // Other
    private LocalDate expiresAt;
}
