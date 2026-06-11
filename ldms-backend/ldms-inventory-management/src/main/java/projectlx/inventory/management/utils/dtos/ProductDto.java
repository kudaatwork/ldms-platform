package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.UnitOfMeasure;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDto {

    private Long id;

    private Long supplierId;
    private String name;
    private String description;
    private BigDecimal price;
    private UnitOfMeasure unitOfMeasure;
    private String productCode;
    private String barcode;
    private Long categoryId;
    private String categoryName;
    private Long subcategoryId;
    private String subcategoryName;
    private String manufacturer;
    private Long imageId;

    private LocalDate expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;
}
