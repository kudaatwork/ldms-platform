package projectlx.inventory.management.utils.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class DispatchIngestRequest {

    @NotBlank
    private String apiKey;

    @NotBlank
    private String externalDispatchId;

    private String productCode;
    private String externalProductId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Quantity must be positive")
    private BigDecimal quantity;

    private String fromLocationLabel;
    private String toLocationLabel;
    private String customerReference;

    private List<String> enRouteDepotLabels = new ArrayList<>();
}
