package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCategoryDto {

    private Long id;

    private String name;
    private String description;
    private Long supplierId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;
}
