package projectlx.inventory.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.utils.dtos.InventoryIntegrationCredentialDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryIntegrationCredentialResponse extends CommonResponse {
    private InventoryIntegrationCredentialDto inventoryIntegrationCredentialDto;
    private List<InventoryIntegrationCredentialDto> inventoryIntegrationCredentialDtoList;
}
