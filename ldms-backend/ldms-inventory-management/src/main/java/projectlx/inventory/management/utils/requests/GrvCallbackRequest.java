package projectlx.inventory.management.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class GrvCallbackRequest {

    @NotBlank
    private String apiKey;

    @NotNull
    private Long grvId;

    private String grvNumber;
    private Long shipmentId;
    private String shipmentNumber;
    private String status;
    private String notes;
}
