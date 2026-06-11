package projectlx.inventory.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.inventory.management.utils.dtos.ProductCategoryDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCategoryResponse extends CommonResponse {
    private ProductCategoryDto productCategoryDto;
    private List<ProductCategoryDto> productCategoryDtoList;
    private Page<ProductCategoryDto> productCategoryDtoPage;
}
