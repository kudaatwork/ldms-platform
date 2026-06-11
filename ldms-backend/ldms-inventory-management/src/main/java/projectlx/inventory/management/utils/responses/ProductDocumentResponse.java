package projectlx.inventory.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.inventory.management.utils.dtos.ProductDocumentDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDocumentResponse extends CommonResponse {
    private ProductDocumentDto productDocumentDto;
    private List<ProductDocumentDto> productDocumentDtoList;
    private Page<ProductDocumentDto> productDocumentDtoPage;
}
