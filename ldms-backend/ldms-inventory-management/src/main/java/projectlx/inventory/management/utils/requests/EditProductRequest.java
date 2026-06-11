package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class EditProductRequest {

    private Long productId; // Identifier field

    // Editable fields
    private String name;
    private String description;
    private String productCode;
    private String barcode;
    private BigDecimal price;
    private UnitOfMeasure unitOfMeasure;
    private Long categoryId;
    private Long subcategoryId;
    private Long supplierId;
    private Long imageId;
    private String manufacturer;
    private LocalDate expiresAt;
    private EntityStatus entityStatus;

    // Optional new image upload (if provided, will replace existing image)
    private org.springframework.web.multipart.MultipartFile imageUpload;
}
