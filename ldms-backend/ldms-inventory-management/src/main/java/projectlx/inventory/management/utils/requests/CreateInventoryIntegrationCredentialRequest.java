package projectlx.inventory.management.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateInventoryIntegrationCredentialRequest {

    @NotNull
    private Long organizationId;

    @NotBlank
    private String credentialLabel;

    private String webhookUrl;
    private String callbackGrvUrl;
}
