package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import projectlx.inventory.management.model.UnitOfMeasure;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
public class CreateProductRequest {

    // Basic info
    private String name;
    private String description;
    private String productCode;
    private String barcode;
    private BigDecimal price;
    private UnitOfMeasure unitOfMeasure;

    // Classification
    private Long productCategoryId;
    private Long productSubCategoryId;

    // External references
    private Long supplierId;

    // Optional Document Upload IDs (linked to a FileUploadService)
    private MultipartFile imageUpload;

    // Other
    private String manufacturer;
    private LocalDate expiresAt;
}
