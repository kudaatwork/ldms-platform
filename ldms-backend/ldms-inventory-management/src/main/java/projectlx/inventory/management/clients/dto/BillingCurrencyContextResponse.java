package projectlx.inventory.management.clients.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillingCurrencyContextResponse extends CommonResponse {
    private OrganizationCurrencyContextDto organizationCurrencyContextDto;
}
