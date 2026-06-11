package projectlx.inventory.management.utils.responses;

import lombok.Getter;
import lombok.Setter;
import projectlx.inventory.management.utils.dtos.SupplierQuoteDto;

import java.util.List;

@Getter
@Setter
public class SupplierQuoteResponse {
    private int statusCode;
    private boolean success;
    private String message;
    private SupplierQuoteDto supplierQuoteDto;
    private List<SupplierQuoteDto> supplierQuoteDtoList;
    private List<String> errorMessages;
}
