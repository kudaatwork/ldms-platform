package projectlx.inventory.management.clients.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationCurrencyContextDto {
    private Long organizationId;
    private String functionalCurrencyCode;
    private String functionalCurrencySymbol;
    private String functionalCurrencyName;
}
