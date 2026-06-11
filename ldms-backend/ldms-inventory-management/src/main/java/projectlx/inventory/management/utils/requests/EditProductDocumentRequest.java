package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDate;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class EditProductDocumentRequest {

    private Long productDocumentId; // Identifier field

    // Editable fields
    private Long productId;
    private String name;
    private String description;
    private org.springframework.web.multipart.MultipartFile documentUpload; // optional new file
    private String documentId; // if provided without upload, directly set external id
    private LocalDate expiresAt;
    private EntityStatus entityStatus;
}
