package projectlx.inventory.management.utils.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.IntegrationCredentialStatus;

@Getter
@Setter
@ToString
public class EditInventoryIntegrationCredentialRequest {

    @NotNull
    private Long id;

    private String credentialLabel;
    private String webhookUrl;
    private String callbackGrvUrl;
    private IntegrationCredentialStatus status;
}
